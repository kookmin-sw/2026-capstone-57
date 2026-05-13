"""미션 생성 핸들러.

미션 생성 전체 흐름을 구현한다:
요청 수신 → ChromaDB 검색 → 프롬프트 증강 → Bedrock 호출 → 응답 파싱 → 결과 발행

ChromaDB 실패 시 동선 정보 기반 폴백, Bedrock 실패 시 1회 재시도,
재시도 실패 시 검색 결과 기반 폴백 미션을 사용한다.
요청당 설정 가능한 타임아웃을 적용한다.

Requirements: 4.1, 5.1, 5.2, 5.3, 5.4, 5.5
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
from app.features.mission.parser import generate_fallback_mission, parse_mission_response
from app.features.mission.prompt import build_mission_prompt
from app.features.mission.search import MissionSearch
from app.sqs.publisher import SQSPublisher

logger = get_logger(__name__)


class MissionHandler:
    """미션 생성 핸들러.

    SQS에서 수신한 미션 요청을 처리하여 ChromaDB 검색 및 Bedrock을 통해
    오프라인 미션을 생성하고, 결과를 응답 큐로 발행한다.

    Args:
        bedrock_client: Bedrock API 클라이언트.
        publisher: SQS 메시지 발행기.
        mission_search: ChromaDB 기반 장소 검색 모듈.
        settings: 애플리케이션 설정.
    """

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
        """미션 생성 요청을 처리한다.

        전체 흐름:
        1. 요청 메시지 파싱
        2. ChromaDB 벡터 검색 (상위 5개 장소)
        3. 프롬프트 증강 → Bedrock 호출 (실패 시 1회 재시도)
        4. 응답 파싱 (실패 시 폴백 미션 사용)
        5. 결과 응답 큐로 발행

        요청당 타임아웃을 적용하여 무한 대기를 방지한다.

        Args:
            message: SQS에서 수신한 원시 메시지 딕셔너리.
        """
        request = MissionRequestMessage(**message)

        with logging_context(correlation_id=request.matchId, feature="mission"):
            logger.info(
                "미션 생성 요청 수신",
                extra={
                    "match_id": request.matchId,
                    "requester_id": request.requesterId,
                    "target_user_id": request.targetUserId,
                },
            )

            try:
                response = await asyncio.wait_for(
                    self._generate_mission(request),
                    timeout=self._settings.request_timeout_seconds,
                )
            except asyncio.TimeoutError:
                logger.error(
                    "미션 생성 타임아웃",
                    extra={"timeout_seconds": self._settings.request_timeout_seconds},
                )
                response = self._build_failed_response(
                    request,
                    f"요청 타임아웃 ({self._settings.request_timeout_seconds}초 초과)",
                )
            except Exception:
                logger.exception("미션 생성 중 예상치 못한 예외 발생")
                response = self._build_failed_response(
                    request,
                    "내부 서버 에러가 발생했습니다",
                )

            await self._publish_response(response)

    async def _generate_mission(
        self, request: MissionRequestMessage
    ) -> MissionResponseMessage:
        """미션 생성 핵심 로직.

        ChromaDB에서 장소를 검색하고, Bedrock을 호출하여 미션을 생성한다.
        각 단계에서 실패 시 적절한 폴백을 적용한다.

        Args:
            request: 미션 생성 요청 메시지.

        Returns:
            미션 응답 메시지.
        """
        # 1. ChromaDB 벡터 검색
        venues = await self._search_venues(request)

        # 2. 프롬프트 증강
        prompt = build_mission_prompt(
            venues=venues,
            intersection_info=request.intersectionInfo,
            user_profiles=request.userProfiles,
        )

        # 3. Bedrock 호출 (첫 번째 시도)
        raw_response = await self._invoke_bedrock(prompt)

        if raw_response is not None:
            mission = self._try_parse(raw_response)
            if mission is not None:
                return self._build_success_response(request, mission)

        # 4. 재시도 (1회)
        logger.info("미션 생성 재시도 (1회)")
        raw_response = await self._invoke_bedrock(prompt)

        if raw_response is not None:
            mission = self._try_parse(raw_response)
            if mission is not None:
                return self._build_success_response(request, mission)

        # 5. 폴백 미션 생성
        logger.info("폴백 미션 생성")
        fallback_mission = generate_fallback_mission(
            venues=venues,
            intersection_info=request.intersectionInfo,
        )
        return self._build_response(request, MissionStatus.FALLBACK, fallback_mission)

    async def _search_venues(self, request: MissionRequestMessage) -> list:
        """ChromaDB에서 장소를 검색한다.

        검색 실패 시 빈 리스트를 반환하여 동선 정보 기반 폴백이
        적용되도록 한다.

        Args:
            request: 미션 생성 요청 메시지.

        Returns:
            VenueResult 리스트 (실패 시 빈 리스트).
        """
        try:
            venues = await self._mission_search.search_venues(
                intersection_info=request.intersectionInfo,
                user_profiles=request.userProfiles,
            )
            logger.info(
                "장소 검색 완료: results=%d",
                len(venues),
            )
            return venues
        except Exception:
            logger.exception("ChromaDB 장소 검색 실패, 동선 정보 기반 폴백 적용")
            return []

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

    def _try_parse(self, raw_response: str) -> Mission | None:
        """응답 파싱을 시도하고 실패 시 None을 반환한다.

        Args:
            raw_response: Bedrock 원시 응답 텍스트.

        Returns:
            파싱된 Mission 객체, 실패 시 None.
        """
        try:
            return parse_mission_response(raw_response)
        except (ValueError, Exception) as e:
            logger.warning(f"미션 응답 파싱 실패: {e}")
            return None

    def _build_success_response(
        self, request: MissionRequestMessage, mission: Mission
    ) -> MissionResponseMessage:
        """성공 응답 메시지를 생성한다."""
        return self._build_response(request, MissionStatus.SUCCESS, mission)

    def _build_response(
        self,
        request: MissionRequestMessage,
        status: MissionStatus,
        mission: Mission,
    ) -> MissionResponseMessage:
        """미션 응답 메시지를 생성한다.

        Args:
            request: 원본 요청 메시지.
            status: 응답 상태 (SUCCESS, FALLBACK, FAILED).
            mission: 생성된 미션 객체.

        Returns:
            MissionResponseMessage 인스턴스.
        """
        return MissionResponseMessage(
            action=MissionResponseAction.MISSION_GENERATED,
            status=status,
            matchId=request.matchId,
            mission=mission,
            completedAt=datetime.now(timezone.utc),
        )

    def _build_failed_response(
        self, request: MissionRequestMessage, error_message: str
    ) -> MissionResponseMessage:
        """실패 응답 메시지를 생성한다.

        Args:
            request: 원본 요청 메시지.
            error_message: 에러 설명 메시지.

        Returns:
            상태가 FAILED인 MissionResponseMessage 인스턴스.
        """
        logger.error(f"미션 생성 실패: {error_message}")
        return MissionResponseMessage(
            action=MissionResponseAction.MISSION_GENERATED,
            status=MissionStatus.FAILED,
            matchId=request.matchId,
            mission=None,
            completedAt=datetime.now(timezone.utc),
        )

    async def _publish_response(self, response: MissionResponseMessage) -> None:
        """응답 메시지를 SQS 큐로 발행한다.

        Args:
            response: 발행할 미션 응답 메시지.
        """
        success = await self._publisher.publish_with_retry(
            self._response_queue_url, response
        )
        if success:
            logger.info(
                f"미션 응답 발행 완료: status={response.status.value}",
            )
        else:
            logger.error(
                "미션 응답 발행 실패 (재시도 소진)",
                extra={"match_id": response.matchId},
            )
