"""일기 메시지 모델.

일기(Diary) 기능의 SQS 요청/응답 메시지 모델을 정의한다.
멀티턴 대화를 통해 플래너 데이터를 기반으로 일기를 작성하는 흐름을 지원한다.

Requirements: 7.1
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class DiaryRequestAction(str, Enum):
    """일기 요청 action 타입."""

    START_SESSION = "START_SESSION"
    ANSWER = "ANSWER"
    COMPLETE = "COMPLETE"


class DiaryResponseAction(str, Enum):
    """일기 응답 action 타입."""

    QUESTION = "QUESTION"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class DiarySessionStatus(str, Enum):
    """일기 세션 상태."""

    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    TIMEOUT = "TIMEOUT"
    FAILED = "FAILED"


class PlannerEntryType(str, Enum):
    """플래너 엔트리 유형."""

    CLASS = "CLASS"
    FREE = "FREE"
    ACTIVITY = "ACTIVITY"


class PlannerEntry(BaseModel):
    """플래너 엔트리 정보.

    일기 작성 시 참조할 당일 일정 정보를 포함한다.

    Attributes:
        dayOfWeek: 요일 (1=월요일, 7=일요일)
        startTime: 시작 시간 (HH:MM 형식)
        endTime: 종료 시간 (HH:MM 형식)
        location: 장소
        name: 일정 이름
        type: 일정 유형 (CLASS, FREE, ACTIVITY)
    """

    dayOfWeek: int
    startTime: str
    endTime: str
    location: str | None = None
    name: str
    type: PlannerEntryType


class DiaryRequestMessage(BaseModel):
    """일기 요청 메시지 (SQS 수신).

    Backend에서 AI Service로 전송되는 일기 관련 요청 메시지.
    action에 따라 세션 시작, 답변 전달, 완료 신호를 구분한다.

    Attributes:
        action: 요청 유형 (START_SESSION, ANSWER, COMPLETE)
        sessionId: 대화 세션 고유 식별자
        userId: 요청 사용자 ID
        plannerEntries: 당일 플래너 엔트리 목록 (START_SESSION 시 필수)
        userMessage: 사용자 답변 (ANSWER 시 필수)
        requestedAt: 요청 시각
    """

    action: DiaryRequestAction
    sessionId: str
    userId: str
    plannerEntries: list[PlannerEntry] | None = None
    userMessage: str | None = None
    requestedAt: datetime = Field(default_factory=lambda: datetime.utcnow())


class DiaryResponseMessage(BaseModel):
    """일기 응답 메시지 (SQS 발행).

    AI Service에서 Backend로 전송되는 일기 관련 응답 메시지.
    action에 따라 질문 전달, 완료 결과, 실패 알림을 구분한다.

    Attributes:
        action: 응답 유형 (QUESTION, COMPLETED, FAILED)
        sessionId: 대화 세션 고유 식별자
        userId: 대상 사용자 ID
        status: 세션 상태
        question: AI가 생성한 질문 (QUESTION 시)
        currentTurn: 현재 턴 번호
        maxTurns: 최대 턴 수
        compiledContent: 컴파일된 최종 일기 (COMPLETED 시)
        completedAt: 완료 시각
        errorMessage: 에러 설명 (FAILED 시)
    """

    action: DiaryResponseAction
    sessionId: str
    userId: str
    status: DiarySessionStatus
    question: str | None = None
    currentTurn: int = 0
    maxTurns: int = 5
    compiledContent: str | None = None
    completedAt: datetime | None = None
    errorMessage: str | None = None
