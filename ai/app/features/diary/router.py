"""일기(Diary) HTTP 라우터.

SQS 비동기 방식 대신 HTTP 동기 방식으로 일기 기능을 제공한다.
핵심 로직(DiaryHandler)을 재사용하되, 결과를 HTTP 응답으로 직접 반환한다.

Endpoints:
    POST /api/diary/start    - 일기 세션 시작
    POST /api/diary/answer   - 사용자 답변 제출
    POST /api/diary/complete - 세션 완료 및 일기 컴파일

Requirements: 7.1, 7.5, 7.6, 7.7, 7.8, 7.9
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.common.exceptions import SessionNotFoundError
from app.common.logging import get_logger, logging_context
from app.conversation.models import MessageRole
from app.features.diary.models import (
    DiaryResponseAction,
    DiaryResponseMessage,
    DiarySessionStatus,
    PlannerEntry,
)
from app.features.diary.prompt import (
    build_compile_prompt,
    build_diary_system_prompt,
)

logger = get_logger(__name__)

router = APIRouter(prefix="/api/diary", tags=["diary"])


# --- Request / Response Schemas ---


class DiaryStartRequest(BaseModel):
    """일기 세션 시작 요청."""

    sessionId: str
    userId: str
    plannerEntries: list[PlannerEntry] = Field(default_factory=list)


class DiaryAnswerRequest(BaseModel):
    """사용자 답변 제출 요청."""

    sessionId: str
    userId: str
    userMessage: str


class DiaryCompleteRequest(BaseModel):
    """세션 완료 요청."""

    sessionId: str
    userId: str


class DiaryStartResponse(BaseModel):
    """세션 시작 응답 (첫 질문 포함)."""

    action: str = DiaryResponseAction.QUESTION.value
    sessionId: str
    userId: str
    status: str = DiarySessionStatus.IN_PROGRESS.value
    question: str
    currentTurn: int = 0
    maxTurns: int = 5


class DiaryAnswerResponse(BaseModel):
    """답변 제출 응답 (후속 질문 또는 완료)."""

    action: str
    sessionId: str
    userId: str
    status: str
    question: Optional[str] = None
    compiledContent: Optional[str] = None
    currentTurn: int = 0
    maxTurns: int = 5


class DiaryCompleteResponse(BaseModel):
    """세션 완료 응답 (컴파일된 일기 포함)."""

    action: str = DiaryResponseAction.COMPLETED.value
    sessionId: str
    userId: str
    status: str = DiarySessionStatus.COMPLETED.value
    compiledContent: Optional[str] = None
    currentTurn: int = 0
    maxTurns: int = 5
    completedAt: Optional[datetime] = None


# --- Endpoints ---


@router.post("/start", response_model=DiaryStartResponse)
async def start_diary_session(body: DiaryStartRequest, request: Request):
    """일기 세션을 시작하고 첫 질문을 반환한다."""
    bedrock_client = request.app.state.bedrock_client
    conversation_manager = request.app.state.conversation_manager
    rag_pipeline = request.app.state.rag_pipeline
    settings = request.app.state.settings

    with logging_context(correlation_id=body.sessionId, feature="diary"):
        try:
            # RAG 검색: 과거 일기 패턴 조회
            rag_documents = await rag_pipeline.search(
                user_id=body.userId,
                query="오늘 하루 일기",
                collection="diaries",
            )

            # 시스템 프롬프트 생성
            system_prompt = build_diary_system_prompt(
                planner_entries=body.plannerEntries,
                rag_documents=rag_documents,
            )

            # 세션 생성
            await conversation_manager.create_session(
                session_id=body.sessionId,
                user_id=body.userId,
                feature="diary",
                system_prompt=system_prompt,
                context={
                    "planner_entries": [
                        entry.model_dump() for entry in body.plannerEntries
                    ],
                },
            )

            # 첫 질문 생성
            first_question = await _generate_question(
                bedrock_client=bedrock_client,
                system_prompt=system_prompt,
                messages=[],
            )

            # AI 질문을 세션에 추가
            await conversation_manager.add_message(
                session_id=body.sessionId,
                role=MessageRole.ASSISTANT,
                content=first_question,
            )

            return DiaryStartResponse(
                sessionId=body.sessionId,
                userId=body.userId,
                question=first_question,
                currentTurn=0,
                maxTurns=settings.conversation_max_turns,
            )

        except Exception as exc:
            logger.exception("일기 세션 시작 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/answer", response_model=DiaryAnswerResponse)
async def answer_diary(body: DiaryAnswerRequest, request: Request):
    """사용자 답변을 처리하고 후속 질문 또는 완료 결과를 반환한다."""
    bedrock_client = request.app.state.bedrock_client
    conversation_manager = request.app.state.conversation_manager
    settings = request.app.state.settings

    with logging_context(correlation_id=body.sessionId, feature="diary"):
        try:
            # 사용자 답변을 세션에 추가
            session = await conversation_manager.add_message(
                session_id=body.sessionId,
                role=MessageRole.USER,
                content=body.userMessage,
            )

            # 최대 턴 수 초과로 강제 완료된 경우
            if conversation_manager.is_session_force_completed(body.sessionId):
                logger.info("최대 턴 수 도달로 강제 완료 처리")
                return await _compile_and_respond(
                    body.sessionId, body.userId, request
                )

            # 후속 질문 생성
            history = await conversation_manager.get_history(body.sessionId)
            session_obj = await conversation_manager.get_session(body.sessionId)

            next_question = await _generate_question(
                bedrock_client=bedrock_client,
                system_prompt=session_obj.system_prompt,
                messages=history,
            )

            # AI 질문을 세션에 추가
            await conversation_manager.add_message(
                session_id=body.sessionId,
                role=MessageRole.ASSISTANT,
                content=next_question,
            )

            return DiaryAnswerResponse(
                action=DiaryResponseAction.QUESTION.value,
                sessionId=body.sessionId,
                userId=body.userId,
                status=DiarySessionStatus.IN_PROGRESS.value,
                question=next_question,
                currentTurn=session.current_turn,
                maxTurns=session.max_turns,
            )

        except SessionNotFoundError as e:
            raise HTTPException(
                status_code=404, detail=f"세션을 찾을 수 없습니다: {e.session_id}"
            )
        except Exception as exc:
            logger.exception("일기 답변 처리 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/complete", response_model=DiaryCompleteResponse)
async def complete_diary(body: DiaryCompleteRequest, request: Request):
    """세션을 완료하고 컴파일된 일기를 반환한다."""
    with logging_context(correlation_id=body.sessionId, feature="diary"):
        try:
            result = await _compile_and_respond(
                body.sessionId, body.userId, request
            )
            return DiaryCompleteResponse(
                sessionId=result.sessionId,
                userId=result.userId,
                status=result.status,
                compiledContent=result.compiledContent,
                currentTurn=result.currentTurn,
                maxTurns=result.maxTurns,
                completedAt=datetime.now(timezone.utc),
            )
        except SessionNotFoundError as e:
            raise HTTPException(
                status_code=404, detail=f"세션을 찾을 수 없습니다: {e.session_id}"
            )
        except Exception as exc:
            logger.exception("일기 완료 처리 실패")
            raise HTTPException(status_code=500, detail=str(exc))


# --- Internal helpers ---


async def _generate_question(
    bedrock_client,
    system_prompt: str,
    messages: list[dict],
) -> str:
    """Bedrock을 호출하여 질문을 생성한다."""
    if not messages:
        initial_messages = [
            {
                "role": "user",
                "content": "오늘 하루에 대한 일기를 시작하겠습니다. 첫 번째 질문을 해주세요.",
            }
        ]
        response = await bedrock_client.invoke_with_messages(
            system_prompt=system_prompt,
            messages=initial_messages,
        )
    else:
        response = await bedrock_client.invoke_with_messages(
            system_prompt=system_prompt,
            messages=messages,
        )
    return response


async def _compile_and_respond(
    session_id: str, user_id: str, request: Request
) -> DiaryAnswerResponse:
    """답변을 컴파일하여 일기를 생성하고 응답을 반환한다."""
    bedrock_client = request.app.state.bedrock_client
    conversation_manager = request.app.state.conversation_manager
    rag_pipeline = request.app.state.rag_pipeline

    # 세션 완료 처리 및 사용자 답변 수집
    user_answers = await conversation_manager.complete_session(session_id)

    if not user_answers:
        raise HTTPException(status_code=400, detail="컴파일할 답변이 없습니다")

    # 세션에서 플래너 엔트리 복원
    session = await conversation_manager.get_session(session_id)
    planner_entries_data = session.context.get("planner_entries", [])
    planner_entries = [PlannerEntry(**entry) for entry in planner_entries_data]

    # 일기 컴파일 (Bedrock 호출)
    compile_prompt = build_compile_prompt(
        planner_entries=planner_entries,
        user_answers=user_answers,
    )

    try:
        compiled_content = await bedrock_client.invoke(compile_prompt)
    except Exception:
        logger.exception("일기 컴파일 Bedrock 호출 실패, 답변 연결로 대체")
        compiled_content = "\n\n".join(user_answers)

    # 벡터 DB에 저장
    try:
        await rag_pipeline.store(
            user_id=user_id,
            content=compiled_content,
            collection="diaries",
            metadata={
                "created_at": datetime.now(timezone.utc).isoformat(),
            },
        )
    except Exception:
        logger.exception("일기 벡터 DB 저장 실패 (계속 진행)")

    return DiaryAnswerResponse(
        action=DiaryResponseAction.COMPLETED.value,
        sessionId=session_id,
        userId=user_id,
        status=DiarySessionStatus.COMPLETED.value,
        compiledContent=compiled_content,
        currentTurn=session.current_turn,
        maxTurns=session.max_turns,
    )
