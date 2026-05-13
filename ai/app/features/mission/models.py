"""미션 메시지 모델.

매칭된 두 사용자에게 오프라인 미션을 생성하기 위한 요청/응답 메시지 스키마.
Requirements: 1.3, 1.4, 6.1, 6.2
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class MissionAction(str, Enum):
    """미션 요청 action 타입."""

    GENERATE_MISSION = "GENERATE_MISSION"


class MissionResponseAction(str, Enum):
    """미션 응답 action 타입."""

    MISSION_GENERATED = "MISSION_GENERATED"


class MissionStatus(str, Enum):
    """미션 생성 결과 상태."""

    SUCCESS = "SUCCESS"
    FALLBACK = "FALLBACK"
    FAILED = "FAILED"


class IntersectionInfo(BaseModel):
    """동선 교집합 정보."""

    timeSlots: list[str] = Field(default_factory=list)
    buildingIds: list[str] = Field(default_factory=list)
    dayOfWeek: int = Field(..., ge=1, le=7)


class UserProfile(BaseModel):
    """사용자 프로필 (미션 생성용)."""

    userId: str
    interests: list[str] = Field(default_factory=list)
    personalityType: list[str] = Field(default_factory=list)


class Mission(BaseModel):
    """생성된 미션."""

    placeName: str
    activity: str
    recommendedTime: str
    description: str


class VenueResult(BaseModel):
    """ChromaDB 벡터 검색 결과 장소."""

    venueId: str
    document: str
    metadata: dict = Field(default_factory=dict)
    distance: float | None = None


class MissionRequestMessage(BaseModel):
    """미션 생성 요청 메시지 (SQS 수신).

    Backend에서 4단계 해금 시 발행하는 미션 생성 요청.
    """

    action: MissionAction = MissionAction.GENERATE_MISSION
    matchId: str
    requesterId: str
    targetUserId: str
    intersectionInfo: IntersectionInfo
    userProfiles: list[UserProfile] = Field(..., min_length=2, max_length=2)
    requestedAt: datetime


class MissionResponseMessage(BaseModel):
    """미션 생성 응답 메시지 (SQS 발행).

    미션 생성 결과를 Backend에 전달하는 응답.
    """

    action: MissionResponseAction = MissionResponseAction.MISSION_GENERATED
    status: MissionStatus
    matchId: str
    mission: Mission | None = None
    completedAt: datetime
