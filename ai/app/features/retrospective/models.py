"""회고 메시지 모델.

회고(Retrospective) 기능의 SQS 요청/응답 메시지 모델을 정의한다.
멀티턴 대화를 통해 만남에 대한 회고글을 작성하는 흐름을 지원한다.

Requirements: 6.1
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class RetroRequestAction(str, Enum):
    """회고 요청 action 타입."""

    START_SESSION = "START_SESSION"
    ANSWER = "ANSWER"
    COMPLETE = "COMPLETE"


class RetroResponseAction(str, Enum):
    """회고 응답 action 타입."""

    QUESTION = "QUESTION"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class RetroSessionStatus(str, Enum):
    """회고 세션 상태."""

    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    TIMEOUT = "TIMEOUT"
    FAILED = "FAILED"


class MeetingContext(BaseModel):
    """만남 맥락 정보.

    회고 작성 시 참조할 만남에 대한 기본 정보를 포함한다.

    Attributes:
        matchedUserId: 매칭된 상대 유저 ID
        matchedUserName: 매칭된 상대 유저 이름
        meetingDate: 만남 날짜
        meetingLocation: 만남 장소
    """

    matchedUserId: str
    matchedUserName: str
    meetingDate: str
    meetingLocation: str | None = None


class RetroRequestMessage(BaseModel):
    """회고 요청 메시지 (SQS 수신).

    Backend에서 AI Service로 전송되는 회고 관련 요청 메시지.
    action에 따라 세션 시작, 답변 전달, 완료 신호를 구분한다.

    Attributes:
        action: 요청 유형 (START_SESSION, ANSWER, COMPLETE)
        sessionId: 대화 세션 고유 식별자
        userId: 요청 사용자 ID
        meetingContext: 만남 맥락 (START_SESSION 시 필수)
        userMessage: 사용자 답변 (ANSWER 시 필수)
        requestedAt: 요청 시각
    """

    action: RetroRequestAction
    sessionId: str
    userId: str
    meetingContext: MeetingContext | None = None
    userMessage: str | None = None
    requestedAt: datetime = Field(default_factory=lambda: datetime.utcnow())


class RetroResponseMessage(BaseModel):
    """회고 응답 메시지 (SQS 발행).

    AI Service에서 Backend로 전송되는 회고 관련 응답 메시지.
    action에 따라 질문 전달, 완료 결과, 실패 알림을 구분한다.

    Attributes:
        action: 응답 유형 (QUESTION, COMPLETED, FAILED)
        sessionId: 대화 세션 고유 식별자
        userId: 대상 사용자 ID
        status: 세션 상태
        question: AI가 생성한 질문 (QUESTION 시)
        currentTurn: 현재 턴 번호
        maxTurns: 최대 턴 수
        compiledContent: 컴파일된 최종 회고글 (COMPLETED 시)
        completedAt: 완료 시각
        errorMessage: 에러 설명 (FAILED 시)
    """

    action: RetroResponseAction
    sessionId: str
    userId: str
    status: RetroSessionStatus
    question: str | None = None
    currentTurn: int = 0
    maxTurns: int = 5
    compiledContent: str | None = None
    completedAt: datetime | None = None
    errorMessage: str | None = None
