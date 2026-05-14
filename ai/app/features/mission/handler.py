"""미션 생성 핸들러.

서브 노드 기반 미션 생성 흐름:
1. 요청 파싱
2. 겹치는 노드 ID 추출
3. ChromaDB에서 노드 상세 정보 검색 (RAG - Retrieval)
4. 프롬프트 증강 (RAG - Augmented)
5. Bedrock 호출 (RAG - Generation)
6. 응답 파싱 → SQS 발행
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from app.bedrock.client import BedrockClient
from app.common.exceptions import BedrockInvocationError
from app.common.logging import get_logger, logging_context
from app.config import Settings
from app.features.mission.models import (
    Mission,
    MissionRequestMessage,
    MissionResponseAction,
    MissionResponseMessage,
    MissionStatus,
)
from app.features.mission.parser import parse_mission_response
from app.features.mission.prompt import build_mission_prompt
from app.features.mission.search import MissionSearch
from app.sqs.publisher import SQSPublisher

logger = get_logger(__name__)


class MissionHandler:
    """미션 생성 핸들러."""

    def __init__(
        self,
        bedrock_client: BedrockClient,
        publisher: SQSPublisher,
        mission_search: MissionSearch,
        settings: Settings,
    ) -> None:
        self._bedrock = bedrock_client
        self._publisher = publisher
        self._mission_search = mission_search
        self._settings = settings
        self._response_queue_url = settings.sqs_mission_response_queue

    async def handle(self, message: dict) -> None:
        """미션 생성 요청을 처리한다."""
        request = MissionRequestMessage(**message)

        with logging_context(correlation_id=request.matchId, feature="mission"):
            logger.info(
                "미션 생성 요청 수신",
                extra={
                    "match_id": request.matchId,
                    "user_a": request.userAId,
                    "user_b": request.userBId,
                    "time_slot": request.timeSlot,
                },
            )

            try:
                response = await asyncio.wait_for(
                    self._generate_mission(request),
                    timeout=self._settings.request_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.error("미션 생성 타임아웃")
                response = self._build_failed_response(
                    request, f"요청 타임아웃 ({self._settings.request_timeout_seconds}초 초과)"
                )
            except Exception:
                logger.exception("미션 생성 중 예상치 못한 예외 발생")
                response = self._build_failed_response(request, "내부 서버 에러")

            await self._publish_response(response)

    async def _generate_mission(
        self, request: MissionRequestMessage
    ) -> MissionResponseMessage:
        """미션 생성 핵심 로직."""

        # 1. 겹치는 노드 ID 추출
        overlapping_ids = self._mission_search.find_overlapping_nodes(
            request.userARoute.subNodeIds,
            request.userBRoute.subNodeIds,
        )

        if not overlapping_ids:
            return self._build_failed_response(request, "겹치는 동선 노드가 없습니다")

        # 2. ChromaDB에서 노드 상세 정보 검색
        nodes = await self._mission_search.search_by_node_ids(overlapping_ids)

        if not nodes:
            return self._build_failed_response(request, "노드 상세 정보를 찾을 수 없습니다")

        # 3. 프롬프트 증강
        prompt = build_mission_prompt(nodes=nodes, request=request)

        # 4. Bedrock 호출 (1회 재시도)
        raw_response = await self._invoke_bedrock(prompt)
        if raw_response is None:
            logger.info("미션 생성 재시도 (1회)")
            raw_response = await self._invoke_bedrock(prompt)

        if raw_response is None:
            return self._build_failed_response(request, "Bedrock 호출 실패")

        # 5. 응답 파싱
        mission = self._try_parse(raw_response)
        if mission is None:
            return self._build_failed_response(request, "미션 응답 파싱 실패")

        return MissionResponseMessage(
            action=MissionResponseAction.MISSION_GENERATED,
            status=MissionStatus.SUCCESS,
            matchId=request.matchId,
            mission=mission,
            completedAt=datetime.now(timezone.utc),
        )

    async def _invoke_bedrock(self, prompt: str) -> str | None:
        """Bedrock API 호출. 실패 시 None 반환."""
        try:
            return await self._bedrock.invoke(prompt)
        except BedrockInvocationError as e:
            logger.warning(f"Bedrock 호출 실패: {e.detail}")
            return None

    def _try_parse(self, raw_response: str) -> Mission | None:
        """응답 파싱 시도. 실패 시 None 반환."""
        try:
            return parse_mission_response(raw_response)
        except (ValueError, Exception) as e:
            logger.warning(f"미션 응답 파싱 실패: {e}")
            return None

    def _build_failed_response(
        self, request: MissionRequestMessage, error_message: str
    ) -> MissionResponseMessage:
        """실패 응답 생성."""
        logger.error(f"미션 생성 실패: {error_message}")
        return MissionResponseMessage(
            action=MissionResponseAction.MISSION_GENERATED,
            status=MissionStatus.FAILED,
            matchId=request.matchId,
            mission=None,
            completedAt=datetime.now(timezone.utc),
            errorMessage=error_message,
        )

    async def _publish_response(self, response: MissionResponseMessage) -> None:
        """응답을 SQS로 발행."""
        success = await self._publisher.publish_with_retry(
            self._response_queue_url, response
        )
        if success:
            logger.info(f"미션 응답 발행 완료: status={response.status.value}")
        else:
            logger.error("미션 응답 발행 실패 (재시도 소진)")
