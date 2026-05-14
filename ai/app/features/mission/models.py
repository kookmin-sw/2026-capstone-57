"""미션 메시지 모델.

서브 노드 기반 미션 생성을 위한 요청/응답 메시지 스키마.
Backend에서 겹치는 서브 노드 ID를 보내면, AI 서버가 ChromaDB에서
상세 정보를 검색하여 미션을 생성한다.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Optional

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
    FAILED = "FAILED"


class BuildingInfo(BaseModel):
    """건물 정보."""

    id: str
    name: str


class RouteInfo(BaseModel):
    """사용자 동선 정보."""

    fromBuilding: BuildingInfo
    toBuilding: BuildingInfo
    subNodeIds: list[str] = Field(default_factory=list)


class Mission(BaseModel):
    """생성된 미션."""

    location: str
    activity: str
    description: str
    selectedNodeId: str


class MissionRequestMessage(BaseModel):
    """미션 생성 요청 메시지 (SQS 수신)."""

    action: MissionAction = MissionAction.GENERATE_MISSION
    matchId: str
    userAId: str
    userBId: str
    timeSlot: str
    userARoute: RouteInfo
    userBRoute: RouteInfo
    requestedAt: datetime


class MissionResponseMessage(BaseModel):
    """미션 생성 응답 메시지 (SQS 발행)."""

    action: MissionResponseAction = MissionResponseAction.MISSION_GENERATED
    status: MissionStatus
    matchId: str
    mission: Optional[Mission] = None
    completedAt: datetime
    errorMessage: Optional[str] = None


class NodeIndexRequest(BaseModel):
    """장소 인덱싱 요청 (HTTP)."""

    nodeId: str
    name: str
    typeActivity: str
    description: str
    operatingHours: Optional[str] = None
