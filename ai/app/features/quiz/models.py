"""퀴즈 메시지 모델.

기존 Java 구현의 JSON 스키마와 동일한 필드명 및 구조를 유지한다.
Requirements: 4.5, 4.7
"""

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class QuizAction(str, Enum):
    """퀴즈 요청 action 타입."""

    GENERATE_QUIZ = "GENERATE_QUIZ"


class QuizResponseAction(str, Enum):
    """퀴즈 응답 action 타입."""

    QUIZ_GENERATED = "QUIZ_GENERATED"


class QuizStatus(str, Enum):
    """퀴즈 생성 결과 상태."""

    SUCCESS = "SUCCESS"
    FALLBACK = "FALLBACK"
    FAILED = "FAILED"


class TargetProfile(BaseModel):
    """퀴즈 대상 유저 프로필."""

    name: str
    nickname: str | None = None
    university: str | None = None
    major: str | None = None
    hobbies: list[str] = Field(default_factory=list)
    interests: list[str] = Field(default_factory=list)
    personalityType: list[str] = Field(default_factory=list)


class QuizQuestion(BaseModel):
    """퀴즈 문제."""

    questionText: str
    choices: list[str] = Field(..., min_length=4, max_length=4)
    correctIndex: int = Field(..., ge=0, le=3)
    explanation: str


class Quiz(BaseModel):
    """퀴즈 응답에 포함되는 퀴즈 데이터."""

    matchId: str
    targetUserId: str
    questions: list[QuizQuestion]
    createdAt: datetime


class QuizRequestMessage(BaseModel):
    """퀴즈 생성 요청 메시지 (SQS 수신).

    기존 Java 구현의 QuizRequestMessage JSON 스키마와 동일한 구조.
    """

    action: QuizAction = QuizAction.GENERATE_QUIZ
    matchId: str
    requesterId: str
    targetUserId: str
    requestedAt: datetime
    targetProfile: TargetProfile


class QuizResponseMessage(BaseModel):
    """퀴즈 생성 응답 메시지 (SQS 발행).

    기존 Java 구현의 QuizResponseMessage JSON 스키마와 동일한 구조.
    """

    action: QuizResponseAction = QuizResponseAction.QUIZ_GENERATED
    status: QuizStatus
    matchId: str
    requesterId: str
    targetUserId: str
    quiz: Quiz | None = None
    questionCount: int = 5
    completedAt: datetime
