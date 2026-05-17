"""일기 핸들러.

일기(Diary) 작성 전체 흐름을 구현한다:
- START_SESSION: 플래너 데이터 수신 + RAG 검색 → 세션 생성 → 첫 질문 생성 → 응답 발행
- ANSWER: 답변 추가 → 후속 질문 생성 → 응답 발행
- COMPLETE: 전체 답변 컴파일 → 벡터 DB 저장 → 최종 결과 발행

세션 타임아웃 시 부분 컴파일 처리를 지원한다.

Requirements: 7.1, 7.5, 7.6, 7.7, 7.8, 7.9
"""

import asyncio
from datetime import datetime, timezone

from app.bedrock.client import BedrockClient
from app.common.exceptions import BedrockInvocationError, SessionNotFoundError
from app.common.logging import get_logger, logging_context
from app.config import Settings
from app.conversation.manager import ConversationManager
from app.conversation.models import MessageRole
from app.features.diary.models import (
    DiaryRequestAction,
    DiaryRequestMessage,
    DiaryResponseAction,
    DiaryResponseMessage,
    DiarySessionStatus,
    PlannerEntry,
)
from app.features.diary.prompt import (
    build_compile_prompt,
    build_diary_system_prompt,
)
from app.rag.pipeline import RAGPipeline
from app.sqs.publisher import SQSPublisher

logger = get_logger(__name__)


class DiaryHandler:
    """일기 작성 핸들러.

    SQS에서 수신한 일기 요청을 처리하여 멀티턴 대화를 통해
    일기를 작성하고, 결과를 응답 큐로 발행한다.

    Args:
        bedrock_client: Bedrock API 클라이언트.
        publisher: SQS 메시지 발행기.
        conversation_manager: 멀티턴 대화 세션 관리자.
        rag_pipeline: RAG 검색/저장 파이프라인.
        settings: 애플리케이션 설정.
    """

    def __init__(
        self,
        bedrock_client: BedrockClient,
        publisher: SQSPublisher,
        conversation_manager: ConversationManager,
        rag_pipeline: RAGPipeline,
        settings: Settings,
    ) -> None:
        self._bedrock = bedrock_client
        self._publisher = publisher
        self._conversation = conversation_manager
        self._rag = rag_pipeline
        self._settings = settings
        self._response_queue_url = settings.sqs_diary_response_queue

    async def handle(self, message: dict) -> None:
        """일기 요청 메시지를 처리한다.

        action 필드에 따라 적절한 처리 흐름을 실행한다.

        Args:
            message: SQS에서 수신한 원시 메시지 딕셔너리.
        """
        request = DiaryRequestMessage(**message)

        with logging_context(
            correlation_id=request.sessionId, feature="diary"
        ):
            logger.info(
                "일기 요청 수신",
                extra={
                    "action": request.action.value,
                    "session_id": request.sessionId,
                    "user_id": request.userId,
                },
            )

            try:
                response = await asyncio.wait_for(
                    self._dispatch(request),
                    timeout=self._settings.request_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.error(
                    "일기 처리 타임아웃",
                    extra={
                        "timeout_seconds": self._settings.request_timeout_seconds,
                    },
                )
                response = self._build_failed_response(
                    request,
                    f"요청 타임아웃 ({self._settings.request_timeout_seconds}초 초과)",
                )
            except SessionNotFoundError as e:
                logger.error(f"세션을 찾을 수 없음: {e.session_id}")
                response = self._build_failed_response(
                    request,
                    f"세션을 찾을 수 없습니다: {e.session_id}",
                )
            except Exception:
                logger.exception("일기 처리 중 예상치 못한 예외 발생")
                response = self._build_failed_response(
                    request,
                    "내부 서버 에러가 발생했습니다",
                )

            await self._publish_response(response)

    async def _dispatch(
        self, request: DiaryRequestMessage
    ) -> DiaryResponseMessage:
        """action에 따라 적절한 처리 메서드로 분기한다.

        Args:
            request: 일기 요청 메시지.

        Returns:
            일기 응답 메시지.
        """
        if request.action == DiaryRequestAction.START_SESSION:
            return await self._handle_start_session(request)
        elif request.action == DiaryRequestAction.ANSWER:
            return await self._handle_answer(request)
        elif request.action == DiaryRequestAction.COMPLETE:
            return await self._handle_complete(request)
        else:
            return self._build_failed_response(
                request, f"알 수 없는 action: {request.action}"
            )

    async def _handle_start_session(
        self, request: DiaryRequestMessage
    ) -> DiaryResponseMessage:
        """세션 시작 요청을 처리한다.

        흐름: 플래너 데이터 수신 + RAG 검색 → 세션 생성 → 첫 질문 생성 → 응답 반환

        Args:
            request: START_SESSION 요청 메시지.

        Returns:
            첫 질문이 포함된 응답 메시지.
        """
        planner_entries = request.plannerEntries or []

        # RAG 검색: 과거 일기 패턴 조회
        rag_documents = await self._rag.search(
            user_id=request.userId,
            query="오늘 하루 일기",
            collection="diaries",
        )

        logger.info(
            f"RAG 검색 완료: {len(rag_documents)}개 문서 검색됨",
            extra={"user_id": request.userId},
        )

        # 시스템 프롬프트 생성
        system_prompt = build_diary_system_prompt(
            planner_entries=planner_entries,
            rag_documents=rag_documents,
        )

        # 세션 생성
        session = await self._conversation.create_session(
            session_id=request.sessionId,
            user_id=request.userId,
            feature="diary",
            system_prompt=system_prompt,
            context={
                "planner_entries": [
                    entry.model_dump() for entry in planner_entries
                ],
            },
        )

        # 첫 질문 생성 (Bedrock 호출)
        first_question = await self._generate_question(
            session_id=request.sessionId,
            system_prompt=system_prompt,
            messages=[],
        )

        # AI 질문을 세션에 추가
        await self._conversation.add_message(
            session_id=request.sessionId,
            role=MessageRole.ASSISTANT,
            content=first_question,
        )

        return DiaryResponseMessage(
            action=DiaryResponseAction.QUESTION,
            sessionId=request.sessionId,
            userId=request.userId,
            status=DiarySessionStatus.IN_PROGRESS,
            question=first_question,
            currentTurn=0,
            maxTurns=self._settings.conversation_max_turns,
        )

    async def _handle_answer(
        self, request: DiaryRequestMessage
    ) -> DiaryResponseMessage:
        """사용자 답변을 처리한다.

        흐름: 답변 추가 → 후속 질문 생성 → 응답 반환
        최대 턴 수 초과 시 강제 완료 처리.

        Args:
            request: ANSWER 요청 메시지.

        Returns:
            후속 질문 또는 완료 응답 메시지.
        """
        if not request.userMessage:
            return self._build_failed_response(
                request, "ANSWER 요청에 userMessage가 필요합니다"
            )

        # 사용자 답변을 세션에 추가
        session = await self._conversation.add_message(
            session_id=request.sessionId,
            role=MessageRole.USER,
            content=request.userMessage,
        )

        # 최대 턴 수 초과로 강제 완료된 경우
        if self._conversation.is_session_force_completed(request.sessionId):
            logger.info("최대 턴 수 도달로 강제 완료 처리")
            return await self._compile_and_respond(request, partial=False)

        # 후속 질문 생성
        history = await self._conversation.get_history(request.sessionId)
        session_obj = await self._conversation.get_session(request.sessionId)

        next_question = await self._generate_question(
            session_id=request.sessionId,
            system_prompt=session_obj.system_prompt,
            messages=history,
        )

        # AI 질문을 세션에 추가
        await self._conversation.add_message(
            session_id=request.sessionId,
            role=MessageRole.ASSISTANT,
            content=next_question,
        )

        return DiaryResponseMessage(
            action=DiaryResponseAction.QUESTION,
            sessionId=request.sessionId,
            userId=request.userId,
            status=DiarySessionStatus.IN_PROGRESS,
            question=next_question,
            currentTurn=session.current_turn,
            maxTurns=session.max_turns,
        )

    async def _handle_complete(
        self, request: DiaryRequestMessage
    ) -> DiaryResponseMessage:
        """완료 요청을 처리한다.

        흐름: 전체 답변 컴파일 → 벡터 DB 저장 → 최종 결과 반환

        Args:
            request: COMPLETE 요청 메시지.

        Returns:
            컴파일된 일기가 포함된 완료 응답 메시지.
        """
        return await self._compile_and_respond(request, partial=False)

    async def _compile_and_respond(
        self, request: DiaryRequestMessage, partial: bool = False
    ) -> DiaryResponseMessage:
        """답변을 컴파일하여 일기를 생성하고 응답을 반환한다.

        Args:
            request: 요청 메시지.
            partial: 부분 컴파일 여부 (타임아웃 시 True).

        Returns:
            컴파일된 일기가 포함된 응답 메시지.
        """
        # 세션 완료 처리 및 사용자 답변 수집
        user_answers = await self._conversation.complete_session(
            request.sessionId
        )

        if not user_answers:
            return self._build_failed_response(
                request, "컴파일할 답변이 없습니다"
            )

        # 세션에서 플래너 엔트리 복원
        session = await self._conversation.get_session(request.sessionId)
        planner_entries_data = session.context.get("planner_entries", [])
        planner_entries = [
            PlannerEntry(**entry) for entry in planner_entries_data
        ]

        # 일기 컴파일 (Bedrock 호출)
        compiled_content = await self._compile_diary(
            planner_entries=planner_entries,
            user_answers=user_answers,
        )

        # 벡터 DB에 저장
        try:
            await self._rag.store(
                user_id=request.userId,
                content=compiled_content,
                collection="diaries",
                metadata={
                    "created_at": datetime.now(timezone.utc).isoformat(),
                },
            )
            logger.info("일기 벡터 DB 저장 완료")
        except Exception:
            logger.exception("일기 벡터 DB 저장 실패 (계속 진행)")

        status = (
            DiarySessionStatus.TIMEOUT
            if partial
            else DiarySessionStatus.COMPLETED
        )

        return DiaryResponseMessage(
            action=DiaryResponseAction.COMPLETED,
            sessionId=request.sessionId,
            userId=request.userId,
            status=status,
            compiledContent=compiled_content,
            currentTurn=session.current_turn,
            maxTurns=session.max_turns,
            completedAt=datetime.now(timezone.utc),
        )

    async def _generate_question(
        self,
        session_id: str,
        system_prompt: str,
        messages: list[dict],
    ) -> str:
        """Bedrock을 호출하여 질문을 생성한다.

        Args:
            session_id: 세션 ID (로깅용).
            system_prompt: 시스템 프롬프트.
            messages: 대화 히스토리.

        Returns:
            생성된 질문 텍스트.

        Raises:
            BedrockInvocationError: Bedrock 호출 실패 시.
        """
        if not messages:
            # 첫 질문: 시스템 프롬프트만으로 생성
            initial_messages = [
                {
                    "role": "user",
                    "content": "오늘 하루에 대한 일기를 시작하겠습니다. 첫 번째 질문을 해주세요.",
                }
            ]
            response = await self._bedrock.invoke_with_messages(
                system_prompt=system_prompt,
                messages=initial_messages,
            )
        else:
            response = await self._bedrock.invoke_with_messages(
                system_prompt=system_prompt,
                messages=messages,
            )

        return response

    async def _compile_diary(
        self,
        planner_entries: list[PlannerEntry],
        user_answers: list[str],
    ) -> str:
        """사용자 답변을 종합하여 일기를 컴파일한다.

        Args:
            planner_entries: 당일 플래너 엔트리 리스트.
            user_answers: 사용자 답변 리스트.

        Returns:
            컴파일된 일기 텍스트.
        """
        compile_prompt = build_compile_prompt(
            planner_entries=planner_entries,
            user_answers=user_answers,
        )

        try:
            compiled = await self._bedrock.invoke(compile_prompt)
            return compiled
        except BedrockInvocationError:
            logger.exception("일기 컴파일 Bedrock 호출 실패, 답변 연결로 대체")
            # 폴백: 답변을 단순 연결
            return "\n\n".join(user_answers)

    def _build_failed_response(
        self, request: DiaryRequestMessage, error_message: str
    ) -> DiaryResponseMessage:
        """실패 응답 메시지를 생성한다.

        Args:
            request: 원본 요청 메시지.
            error_message: 에러 설명 메시지.

        Returns:
            상태가 FAILED인 응답 메시지.
        """
        logger.error(f"일기 처리 실패: {error_message}")
        return DiaryResponseMessage(
            action=DiaryResponseAction.FAILED,
            sessionId=request.sessionId,
            userId=request.userId,
            status=DiarySessionStatus.FAILED,
            errorMessage=error_message,
            completedAt=datetime.now(timezone.utc),
        )

    async def _publish_response(
        self, response: DiaryResponseMessage
    ) -> None:
        """응답 메시지를 SQS 큐로 발행한다.

        Args:
            response: 발행할 일기 응답 메시지.
        """
        success = await self._publisher.publish_with_retry(
            self._response_queue_url, response
        )
        if success:
            logger.info(
                f"일기 응답 발행 완료: action={response.action.value}, "
                f"status={response.status.value}",
            )
        else:
            logger.error(
                "일기 응답 발행 실패 (재시도 소진)",
                extra={"session_id": response.sessionId},
            )
