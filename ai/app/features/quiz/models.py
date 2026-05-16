"""퀴즈 메시지 모델.

userId 기반 단순화된 퀴즈 요청/응답 스키마.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field, field_validator


class QuizAction(str, Enum):
    """퀴즈 요청 action 타입."""

    GENERATE_QUIZ = "GENERATE_QUIZ"


class QuizStatus(str, Enum):
    """퀴즈 생성 결과 상태."""

    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class UserProfile(BaseModel):
    """퀴즈 대상 유저 프로필."""

    name: str
    nickname: str | None = None
    university: str | None = None
    major: str | None = None
    hobbies: list[str] = Field(default_factory=list)
    interests: list[str] = Field(default_factory=list)
    personalityType: str | None = None

    @field_validator("hobbies", "interests", mode="before")
    @classmethod
    def ensure_list(cls, v):
        """문자열이나 None이 들어오면 리스트로 변환."""
        if v is None:
            return []
        if isinstance(v, str):
            return [v]
        return v


class QuizQuestion(BaseModel):
    """퀴즈 문제."""

    questionText: str
    choices: list[str] = Field(..., min_length=4, max_length=4)
    correctIndex: int = Field(..., ge=0, le=3)


class Quiz(BaseModel):
    """퀴즈 응답에 포함되는 퀴즈 데이터."""

    userId: str
    questions: list[QuizQuestion]
    createdAt: datetime


class QuizRequestMessage(BaseModel):
    """퀴즈 생성 요청 메시지 (SQS 수신)."""

    action: QuizAction = QuizAction.GENERATE_QUIZ
    userId: str
    requestedAt: datetime | None = None
    userProfile: UserProfile


class QuizResponseMessage(BaseModel):
    """퀴즈 생성 응답 메시지 (SQS 발행)."""

    action: QuizAction = QuizAction.GENERATE_QUIZ
    status: QuizStatus
    userId: str
    quiz: Quiz | None = None
    questionCount: int = 5
    completedAt: datetime
