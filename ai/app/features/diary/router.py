"""일기(Diary) HTTP 라우터 (Stateless).

AI 서버는 stateless하게 동작하며, 매 요청마다 전체 컨텍스트를 받아 처리한다.
세션 상태는 Backend(Spring Boot)가 DB에서 관리한다.

Endpoints:
    POST /api/diary/first-question  - 첫 질문 생성
    POST /api/diary/next-question   - 다음 질문 생성 (대화 완료 판단 포함)
    POST /api/diary/generate        - 대화 내용 기반 일기 생성
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.common.logging import get_logger, logging_context

logger = get_logger(__name__)

router = APIRouter(prefix="/api/diary", tags=["diary"])


# --- Request / Response Schemas ---


class ScheduleEntry(BaseModel):
    """당일 일정 항목."""

    startTime: str
    endTime: str
    location: Optional[str] = None
    activity: str


class ConversationTurn(BaseModel):
    """대화 턴."""

    turnNumber: int
    question: str
    answer: str


class FirstQuestionRequest(BaseModel):
    """첫 질문 생성 요청."""

    userId: str
    date: str
    todaySchedule: list[ScheduleEntry] = Field(default_factory=list)
    previousDiaryContent: Optional[str] = None


class NextQuestionRequest(BaseModel):
    """다음 질문 생성 요청."""

    userId: str
    date: str
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)
    todaySchedule: list[ScheduleEntry] = Field(default_factory=list)


class GenerateRequest(BaseModel):
    """일기 생성 요청."""

    userId: str
    date: str
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)
    todaySchedule: list[ScheduleEntry] = Field(default_factory=list)


class FirstQuestionResponse(BaseModel):
    """첫 질문 응답."""

    question: str
    maxTurns: int = 5


class NextQuestionResponse(BaseModel):
    """다음 질문 응답."""

    question: Optional[str] = None
    isConversationComplete: bool = False
    currentTurn: int
    maxTurns: int = 5


class GenerateResponse(BaseModel):
    """일기 생성 응답."""

    compiledContent: str
    generatedAt: datetime


# --- Internal helpers ---


def _build_schedule_text(schedule: list[ScheduleEntry]) -> str:
    """일정 목록을 텍스트로 변환."""
    if not schedule:
        return "(일정 없음)"

    lines = []
    for entry in schedule:
        location = f" @ {entry.location}" if entry.location else ""
        lines.append(f"- {entry.startTime}~{entry.endTime}{location}: {entry.activity}")
    return "\n".join(lines)


def _build_conversation_messages(
    system_prompt: str,
    conversation_history: list[ConversationTurn],
) -> tuple[str, list[dict]]:
    """대화 히스토리를 Bedrock 메시지 형식으로 변환."""
    messages = []
    for turn in conversation_history:
        messages.append({"role": "assistant", "content": turn.question})
        messages.append({"role": "user", "content": turn.answer})
    return system_prompt, messages


def _build_first_question_system_prompt(
    schedule: list[ScheduleEntry],
    previous_diary: Optional[str],
) -> str:
    """첫 질문 생성용 시스템 프롬프트."""
    schedule_text = _build_schedule_text(schedule)

    previous_section = ""
    if previous_diary:
        previous_section = f"""
[어제 일기]
{previous_diary}
"""

    return f"""당신은 대학생의 일기 작성을 도와주는 따뜻한 AI 친구입니다.
사용자의 오늘 일정을 참고하여, 하루를 돌아볼 수 있는 자연스러운 질문을 해주세요.

[오늘 일정]
{schedule_text}
{previous_section}
[규칙]
1. 질문은 1개만 생성하세요.
2. 일정에 있는 구체적인 활동을 언급하며 질문하세요.
3. 친근하고 부담 없는 해요체를 사용하세요.
4. 감정, 느낌, 경험에 초점을 맞추세요.
5. 예/아니오로 답할 수 없는 열린 질문을 하세요.
"""


def _build_next_question_system_prompt(
    schedule: list[ScheduleEntry],
    max_turns: int,
    current_turn: int,
) -> str:
    """다음 질문 생성용 시스템 프롬프트."""
    schedule_text = _build_schedule_text(schedule)

    return f"""당신은 대학생의 일기 작성을 도와주는 따뜻한 AI 친구입니다.
이전 대화를 바탕으로 더 깊이 있는 후속 질문을 해주세요.

[오늘 일정]
{schedule_text}

[규칙]
1. 질문은 1개만 생성하세요.
2. 이전 답변에서 언급된 감정이나 경험을 더 깊이 탐색하세요.
3. 이미 물어본 내용을 반복하지 마세요.
4. 친근하고 부담 없는 해요체를 사용하세요.
5. 현재 {current_turn}/{max_turns} 턴입니다.
6. 사용자가 충분히 풍부한 답변을 했다고 판단되면, 질문 대신 정확히 "[COMPLETE]"만 출력하세요.
7. 아직 더 물어볼 내용이 있다면 질문을 생성하세요.
"""


def _build_generate_prompt(
    schedule: list[ScheduleEntry],
    conversation_history: list[ConversationTurn],
) -> str:
    """일기 생성 프롬프트."""
    schedule_text = _build_schedule_text(schedule)

    conversation_text = ""
    for turn in conversation_history:
        conversation_text += f"Q: {turn.question}\nA: {turn.answer}\n\n"

    return f"""당신은 대학생의 일기를 작성해주는 AI입니다.
아래 대화 내용을 바탕으로 자연스러운 일기를 작성해주세요.

[오늘 일정]
{schedule_text}

[대화 내용]
{conversation_text}

[규칙]
1. 1인칭 시점으로 작성하세요 (나는, 내가).
2. 대화에서 나온 감정과 경험을 자연스럽게 녹여내세요.
3. 일정에 있는 활동과 연결지어 작성하세요.
4. 3~5문단 분량으로 작성하세요.
5. 친근하고 자연스러운 문체를 사용하세요 (해요체 X, 반말 일기체).
6. 대화 형식이 아닌 일기 형식으로 작성하세요.
"""


# --- Endpoints ---


@router.post("/first-question", response_model=FirstQuestionResponse)
async def first_question(body: FirstQuestionRequest, request: Request):
    """첫 질문을 생성한다.

    당일 일정 + 전날 일기를 기반으로 첫 번째 질문을 생성한다.
    """
    bedrock_client = request.app.state.bedrock_client
    settings = request.app.state.settings

    with logging_context(correlation_id=f"diary-{body.userId}-{body.date}", feature="diary"):
        try:
            system_prompt = _build_first_question_system_prompt(
                schedule=body.todaySchedule,
                previous_diary=body.previousDiaryContent,
            )

            # Bedrock 호출
            initial_messages = [
                {
                    "role": "user",
                    "content": "오늘 하루에 대한 일기를 시작하겠습니다. 첫 번째 질문을 해주세요.",
                }
            ]
            question = await bedrock_client.invoke_with_messages(
                system_prompt=system_prompt,
                messages=initial_messages,
            )

            return FirstQuestionResponse(
                question=question,
                maxTurns=settings.conversation_max_turns,
            )

        except Exception as exc:
            logger.exception("첫 질문 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/next-question", response_model=NextQuestionResponse)
async def next_question(body: NextQuestionRequest, request: Request):
    """다음 질문을 생성하거나 대화 완료를 판단한다.

    전체 대화 히스토리를 받아 다음 질문을 생성한다.
    AI가 충분하다고 판단하면 isConversationComplete=true를 반환한다.
    """
    bedrock_client = request.app.state.bedrock_client
    settings = request.app.state.settings
    max_turns = settings.conversation_max_turns
    current_turn = len(body.conversationHistory)

    with logging_context(correlation_id=f"diary-{body.userId}-{body.date}", feature="diary"):
        try:
            # maxTurns 도달 시 강제 완료
            if current_turn >= max_turns:
                return NextQuestionResponse(
                    question=None,
                    isConversationComplete=True,
                    currentTurn=current_turn,
                    maxTurns=max_turns,
                )

            system_prompt = _build_next_question_system_prompt(
                schedule=body.todaySchedule,
                max_turns=max_turns,
                current_turn=current_turn,
            )

            # 대화 히스토리를 메시지로 변환
            messages = []
            for turn in body.conversationHistory:
                messages.append({"role": "assistant", "content": turn.question})
                messages.append({"role": "user", "content": turn.answer})

            # 후속 질문 요청 추가
            messages.append({
                "role": "user",
                "content": "다음 질문을 해주세요. 충분하다고 판단되면 [COMPLETE]만 출력하세요.",
            })

            response = await bedrock_client.invoke_with_messages(
                system_prompt=system_prompt,
                messages=messages,
            )

            # AI가 완료 판단한 경우
            if "[COMPLETE]" in response:
                return NextQuestionResponse(
                    question=None,
                    isConversationComplete=True,
                    currentTurn=current_turn,
                    maxTurns=max_turns,
                )

            return NextQuestionResponse(
                question=response.strip(),
                isConversationComplete=False,
                currentTurn=current_turn,
                maxTurns=max_turns,
            )

        except Exception as exc:
            logger.exception("다음 질문 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/generate", response_model=GenerateResponse)
async def generate_diary(body: GenerateRequest, request: Request):
    """대화 내용을 기반으로 일기를 생성한다.

    전체 대화 히스토리를 받아 일기를 컴파일한다.
    """
    bedrock_client = request.app.state.bedrock_client

    with logging_context(correlation_id=f"diary-{body.userId}-{body.date}", feature="diary"):
        try:
            if not body.conversationHistory:
                raise HTTPException(status_code=400, detail="대화 내역이 없습니다")

            prompt = _build_generate_prompt(
                schedule=body.todaySchedule,
                conversation_history=body.conversationHistory,
            )

            compiled_content = await bedrock_client.invoke(prompt)

            return GenerateResponse(
                compiledContent=compiled_content,
                generatedAt=datetime.now(timezone.utc),
            )

        except HTTPException:
            raise
        except Exception as exc:
            logger.exception("일기 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))
