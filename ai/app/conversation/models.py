"""대화 관련 Pydantic 모델."""

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class MessageRole(str, Enum):
    """대화 메시지의 역할."""

    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


class ConversationMessage(BaseModel):
    """대화 내 개별 메시지."""

    role: MessageRole
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class SessionStatus(str, Enum):
    """대화 세션의 상태."""

    ACTIVE = "active"
    COMPLETED = "completed"
    TIMEOUT = "timeout"
    FORCE_COMPLETED = "force_completed"


class Session(BaseModel):
    """멀티턴 대화 세션."""

    session_id: str
    user_id: str
    feature: str  # "retrospective" | "diary"
    system_prompt: str
    messages: list[ConversationMessage] = []
    context: dict = {}
    status: SessionStatus = SessionStatus.ACTIVE
    current_turn: int = 0
    max_turns: int = 5
    created_at: datetime = Field(default_factory=datetime.utcnow)
    last_activity: datetime = Field(default_factory=datetime.utcnow)
