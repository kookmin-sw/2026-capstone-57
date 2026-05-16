"""퀴즈 생성 핸들러.

퀴즈 생성 전체 흐름:
요청 수신 → 프롬프트 생성 → Bedrock 호출 → 응답 파싱 → 결과 발행

userId 기반 단순화된 스키마 사용.
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
    QuizAction,
    QuizRequestMessage,
    QuizResponseMessage,
    QuizStatus,
)
from app.features.quiz.parser import generate_fallback_quiz, parse_quiz_response
from app.features.quiz.prompt import build_quiz_prompt
from app.sqs.publisher import SQSPublisher

logger = get_logger(__name__)


class QuizHandler:
    """퀴즈 생성 핸들러."""

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
        """퀴즈 생성 요청을 처리한다."""
        request = QuizRequestMessage(**message)

        with logging_context(correlation_id=request.userId, feature="quiz"):
            logger.info(
                "퀴즈 생성 요청 수신",
                extra={"user_id": request.userId},
            )

            try:
                response = await asyncio.wait_for(
                    self._generate_quiz(request),
                    timeout=self._settings.request_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.error("퀴즈 생성 타임아웃")
                response = self._build_failed_response(request)
            except Exception:
                logger.exception("퀴즈 생성 중 예상치 못한 예외 발생")
                response = self._build_failed_response(request)

            await self._publish_response(response)

    async def _generate_quiz(
        self, request: QuizRequestMessage
    ) -> QuizResponseMessage:
        """퀴즈 생성 핵심 로직."""
        prompt = build_quiz_prompt(request.userProfile)

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

        # 폴백 퀴즈
        logger.info("폴백 퀴즈 생성")
        fallback_questions = generate_fallback_quiz(request.userProfile)
        return self._build_success_response(request, fallback_questions)

    async def _invoke_bedrock(self, prompt: str) -> str | None:
        """Bedrock API 호출. 실패 시 None."""
        try:
            return await self._bedrock.invoke(prompt)
        except BedrockInvocationError as e:
            logger.warning(f"Bedrock 호출 실패: {e.detail}")
            return None

    def _try_parse(self, raw_response: str) -> list | None:
        """응답 파싱 시도. 실패 시 None."""
        try:
            return parse_quiz_response(raw_response)
        except (ValueError, Exception) as e:
            logger.warning(f"퀴즈 응답 파싱 실패: {e}")
            return None

    def _build_success_response(
        self, request: QuizRequestMessage, questions: list
    ) -> QuizResponseMessage:
        """성공 응답 생성."""
        now = datetime.now(timezone.utc)
        quiz = Quiz(
            userId=request.userId,
            questions=questions,
            createdAt=now,
        )
        return QuizResponseMessage(
            action=QuizAction.GENERATE_QUIZ,
            status=QuizStatus.SUCCESS,
            userId=request.userId,
            quiz=quiz,
            questionCount=len(questions),
            completedAt=now,
        )

    def _build_failed_response(
        self, request: QuizRequestMessage
    ) -> QuizResponseMessage:
        """실패 응답 생성."""
        return QuizResponseMessage(
            action=QuizAction.GENERATE_QUIZ,
            status=QuizStatus.FAILED,
            userId=request.userId,
            quiz=None,
            questionCount=0,
            completedAt=datetime.now(timezone.utc),
        )

    async def _publish_response(self, response: QuizResponseMessage) -> None:
        """응답을 SQS로 발행."""
        success = await self._publisher.publish_with_retry(
            self._response_queue_url, response
        )
        if success:
            logger.info(f"퀴즈 응답 발행 완료: status={response.status.value}")
        else:
            logger.error("퀴즈 응답 발행 실패 (재시도 소진)")
