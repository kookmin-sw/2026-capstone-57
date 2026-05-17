"""퀴즈 생성 핸들러.

퀴즈 생성 전체 흐름을 구현한다:
요청 수신 → 프롬프트 생성 → Bedrock 호출 → 응답 파싱 → 결과 발행

첫 번째 실패 시 1회 재시도, 재시도 실패 시 폴백 퀴즈를 사용한다.
요청당 설정 가능한 타임아웃을 적용한다.

Requirements: 4.1, 4.6, 9.1, 9.4
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from app.bedrock.client import BedrockClient
from app.common.exceptions import BedrockInvocationError
from app.common.logging import get_logger, logging_context
from app.config import Settings
from app.features.quiz.models import (
    Quiz,
    QuizRequestMessage,
    QuizResponseMessage,
    QuizResponseAction,
    QuizStatus,
)
from app.features.quiz.parser import generate_fallback_quiz, parse_quiz_response
from app.features.quiz.prompt import build_quiz_prompt
from app.sqs.publisher import SQSPublisher

logger = get_logger(__name__)


class QuizHandler:
    """퀴즈 생성 핸들러.

    SQS에서 수신한 퀴즈 요청을 처리하여 Bedrock을 통해 퀴즈를 생성하고,
    결과를 응답 큐로 발행한다.

    Args:
        bedrock_client: Bedrock API 클라이언트.
        publisher: SQS 메시지 발행기.
        settings: 애플리케이션 설정.
    """

    def __init__(
        self,
        bedrock_client: BedrockClient,
        publisher: SQSPublisher,
        settings: Settings,
    ) -> None:
        self._bedrock = bedrock_client
        self._publisher = publisher
        self._settings = settings
        self._response_queue_url = settings.sqs_quiz_response_queue

    async def handle(self, message: dict) -> None:
        """퀴즈 생성 요청을 처리한다.

        전체 흐름:
        1. 요청 메시지 파싱
        2. 프로필 기반 프롬프트 생성
        3. Bedrock 호출 (실패 시 1회 재시도)
        4. 응답 파싱 (실패 시 폴백 퀴즈 사용)
        5. 결과 응답 큐로 발행

        요청당 타임아웃을 적용하여 무한 대기를 방지한다.

        Args:
            message: SQS에서 수신한 원시 메시지 딕셔너리.
        """
        request = QuizRequestMessage(**message)

        with logging_context(correlation_id=request.matchId, feature="quiz"):
            logger.info(
                "퀴즈 생성 요청 수신",
                extra={
                    "match_id": request.matchId,
                    "target_user_id": request.targetUserId,
                },
            )

            try:
                response = await asyncio.wait_for(
                    self._generate_quiz(request),
                    timeout=self._settings.request_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.error(
                    "퀴즈 생성 타임아웃",
                    extra={"timeout_seconds": self._settings.request_timeout_seconds},
                )
                response = self._build_failed_response(
                    request,
                    f"요청 타임아웃 ({self._settings.request_timeout_seconds}초 초과)",
                )
            except Exception:
                logger.exception("퀴즈 생성 중 예상치 못한 예외 발생")
                response = self._build_failed_response(
                    request,
                    "내부 서버 에러가 발생했습니다",
                )

            await self._publish_response(response)

    async def _generate_quiz(
        self, request: QuizRequestMessage
    ) -> QuizResponseMessage:
        """퀴즈 생성 핵심 로직.

        Bedrock을 호출하여 퀴즈를 생성하고, 실패 시 재시도 및 폴백을 적용한다.

        Args:
            request: 퀴즈 생성 요청 메시지.

        Returns:
            퀴즈 응답 메시지.
        """
        prompt = build_quiz_prompt(request.targetProfile)

        # 첫 번째 시도
        raw_response = await self._invoke_bedrock(prompt)

        if raw_response is not None:
            questions = self._try_parse(raw_response)
            if questions is not None:
                return self._build_success_response(request, questions)

        # 재시도 (1회)
        logger.info("퀴즈 생성 재시도 (1회)")
        raw_response = await self._invoke_bedrock(prompt)

        if raw_response is not None:
            questions = self._try_parse(raw_response)
            if questions is not None:
                return self._build_success_response(request, questions)

        # 폴백 퀴즈 사용
        logger.info("폴백 퀴즈 생성")
        fallback_questions = generate_fallback_quiz(request.targetProfile)
        return self._build_response(
            request, QuizStatus.FALLBACK, fallback_questions
        )

    async def _invoke_bedrock(self, prompt: str) -> str | None:
        """Bedrock API를 호출하고 실패 시 None을 반환한다.

        Args:
            prompt: Bedrock에 전송할 프롬프트.

        Returns:
            모델 응답 텍스트, 실패 시 None.
        """
        try:
            return await self._bedrock.invoke(prompt)
        except BedrockInvocationError as e:
            logger.warning(f"Bedrock 호출 실패: {e.detail}")
            return None

    def _try_parse(self, raw_response: str) -> list | None:
        """응답 파싱을 시도하고 실패 시 None을 반환한다.

        Args:
            raw_response: Bedrock 원시 응답 텍스트.

        Returns:
            파싱된 QuizQuestion 리스트, 실패 시 None.
        """
        try:
            return parse_quiz_response(raw_response)
        except (ValueError, Exception) as e:
            logger.warning(f"퀴즈 응답 파싱 실패: {e}")
            return None

    def _build_success_response(
        self, request: QuizRequestMessage, questions: list
    ) -> QuizResponseMessage:
        """성공 응답 메시지를 생성한다."""
        return self._build_response(request, QuizStatus.SUCCESS, questions)

    def _build_response(
        self,
        request: QuizRequestMessage,
        status: QuizStatus,
        questions: list,
    ) -> QuizResponseMessage:
        """퀴즈 응답 메시지를 생성한다.

        Args:
            request: 원본 요청 메시지.
            status: 응답 상태 (SUCCESS, FALLBACK, FAILED).
            questions: 퀴즈 문제 리스트.

        Returns:
            QuizResponseMessage 인스턴스.
        """
        now = datetime.now(timezone.utc)
        quiz = Quiz(
            matchId=request.matchId,
            targetUserId=request.targetUserId,
            questions=questions,
            createdAt=now,
        )
        return QuizResponseMessage(
            action=QuizResponseAction.QUIZ_GENERATED,
            status=status,
            matchId=request.matchId,
            requesterId=request.requesterId,
            targetUserId=request.targetUserId,
            quiz=quiz,
            questionCount=len(questions),
            completedAt=now,
        )

    def _build_failed_response(
        self, request: QuizRequestMessage, error_message: str
    ) -> QuizResponseMessage:
        """실패 응답 메시지를 생성한다.

        Args:
            request: 원본 요청 메시지.
            error_message: 에러 설명 메시지.

        Returns:
            상태가 FAILED인 QuizResponseMessage 인스턴스.
        """
        logger.error(f"퀴즈 생성 실패: {error_message}")
        return QuizResponseMessage(
            action=QuizResponseAction.QUIZ_GENERATED,
            status=QuizStatus.FAILED,
            matchId=request.matchId,
            requesterId=request.requesterId,
            targetUserId=request.targetUserId,
            quiz=None,
            questionCount=0,
            completedAt=datetime.now(timezone.utc),
        )

    async def _publish_response(self, response: QuizResponseMessage) -> None:
        """응답 메시지를 SQS 큐로 발행한다.

        Args:
            response: 발행할 퀴즈 응답 메시지.
        """
        success = await self._publisher.publish_with_retry(
            self._response_queue_url, response
        )
        if success:
            logger.info(
                f"퀴즈 응답 발행 완료: status={response.status.value}",
            )
        else:
            logger.error(
                "퀴즈 응답 발행 실패 (재시도 소진)",
                extra={"match_id": response.matchId},
            )
