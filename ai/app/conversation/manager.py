"""멀티턴 대화 세션 관리.

인메모리 저장소를 사용하여 세션별 메시지 히스토리를 관리하며,
설정 가능한 최대 턴 수와 세션 타임아웃을 적용한다.
"""

from __future__ import annotations

from datetime import datetime, timedelta

from app.common.exceptions import SessionNotFoundError
from app.common.logging import get_logger
from app.conversation.models import (
    ConversationMessage,
    MessageRole,
    Session,
    SessionStatus,
)

logger = get_logger(__name__)


class ConversationManager:
    """멀티턴 대화 세션 관리.

    인메모리 dict 기반 세션 저장소를 사용하며,
    세션 생성, 메시지 추가, 히스토리 조회, 세션 완료, 만료 정리를 지원한다.

    Attributes:
        max_turns: 세션당 최대 턴 수 (초과 시 강제 완료)
        session_timeout_minutes: 세션 타임아웃 (분 단위, 비활성 세션 자동 정리)
    """

    def __init__(self, max_turns: int = 5, session_timeout_minutes: int = 30) -> None:
        self._sessions: dict[str, Session] = {}
        self.max_turns = max_turns
        self.session_timeout_minutes = session_timeout_minutes

    async def create_session(
        self,
        session_id: str,
        user_id: str,
        feature: str,
        system_prompt: str,
        context: dict | None = None,
    ) -> Session:
        """새 대화 세션을 생성한다.

        Args:
            session_id: 고유 세션 식별자
            user_id: 사용자 ID
            feature: 기능 유형 ("retrospective" | "diary")
            system_prompt: 시스템 프롬프트
            context: 세션에 연결할 추가 컨텍스트 데이터

        Returns:
            생성된 Session 객체
        """
        now = datetime.utcnow()
        session = Session(
            session_id=session_id,
            user_id=user_id,
            feature=feature,
            system_prompt=system_prompt,
            context=context or {},
            max_turns=self.max_turns,
            created_at=now,
            last_activity=now,
        )
        self._sessions[session_id] = session

        logger.info(
            "세션 생성",
            extra={
                "correlation_id": session_id,
                "feature": feature,
            },
        )
        return session

    async def add_message(
        self,
        session_id: str,
        role: MessageRole,
        content: str,
    ) -> Session:
        """세션에 메시지를 추가한다.

        사용자 메시지 추가 시 턴 수를 증가시키며,
        최대 턴 수 초과 시 세션을 강제 완료 상태로 전환한다.

        Args:
            session_id: 대상 세션 ID
            role: 메시지 역할 (user, assistant)
            content: 메시지 내용

        Returns:
            업데이트된 Session 객체

        Raises:
            SessionNotFoundError: 세션이 존재하지 않을 때
        """
        session = self._get_session(session_id)

        message = ConversationMessage(
            role=role,
            content=content,
            timestamp=datetime.utcnow(),
        )
        session.messages.append(message)
        session.last_activity = datetime.utcnow()

        # 사용자 메시지일 때만 턴 수 증가
        if role == MessageRole.USER:
            session.current_turn += 1

            # 최대 턴 수 초과 시 강제 완료
            if session.current_turn >= session.max_turns:
                session.status = SessionStatus.FORCE_COMPLETED
                logger.info(
                    "최대 턴 수 도달로 세션 강제 완료",
                    extra={
                        "correlation_id": session_id,
                        "feature": session.feature,
                        "current_turn": session.current_turn,
                        "max_turns": session.max_turns,
                    },
                )

        return session

    async def get_history(self, session_id: str) -> list[dict]:
        """세션의 전체 메시지 히스토리를 반환한다.

        Bedrock API 호출에 사용할 수 있는 형식으로 반환한다.
        시스템 프롬프트는 포함하지 않으며, user/assistant 메시지만 반환한다.

        Args:
            session_id: 대상 세션 ID

        Returns:
            메시지 히스토리 리스트 (각 항목은 {"role": str, "content": str})

        Raises:
            SessionNotFoundError: 세션이 존재하지 않을 때
        """
        session = self._get_session(session_id)
        return [
            {"role": msg.role.value, "content": msg.content}
            for msg in session.messages
            if msg.role != MessageRole.SYSTEM
        ]

    async def get_session(self, session_id: str) -> Session:
        """세션 객체를 반환한다.

        Args:
            session_id: 대상 세션 ID

        Returns:
            Session 객체

        Raises:
            SessionNotFoundError: 세션이 존재하지 않을 때
        """
        return self._get_session(session_id)

    async def complete_session(self, session_id: str) -> list[str]:
        """세션을 완료 처리하고 사용자 답변 목록을 반환한다.

        세션 상태를 COMPLETED로 변경하고,
        사용자가 제공한 모든 답변(user 메시지)을 순서대로 반환한다.

        Args:
            session_id: 대상 세션 ID

        Returns:
            사용자 답변 문자열 리스트

        Raises:
            SessionNotFoundError: 세션이 존재하지 않을 때
        """
        session = self._get_session(session_id)

        if session.status == SessionStatus.ACTIVE:
            session.status = SessionStatus.COMPLETED

        session.last_activity = datetime.utcnow()

        user_answers = [
            msg.content
            for msg in session.messages
            if msg.role == MessageRole.USER
        ]

        logger.info(
            "세션 완료",
            extra={
                "correlation_id": session_id,
                "feature": session.feature,
                "total_turns": session.current_turn,
                "status": session.status.value,
            },
        )

        return user_answers

    async def cleanup_expired(self) -> int:
        """만료된 세션을 정리한다.

        session_timeout_minutes 이상 비활성인 세션을 찾아
        TIMEOUT 상태로 변경하고 저장소에서 제거한다.

        Returns:
            정리된 세션 수
        """
        now = datetime.utcnow()
        timeout_threshold = now - timedelta(minutes=self.session_timeout_minutes)

        expired_ids = [
            sid
            for sid, session in self._sessions.items()
            if session.status == SessionStatus.ACTIVE
            and session.last_activity < timeout_threshold
        ]

        for sid in expired_ids:
            session = self._sessions[sid]
            session.status = SessionStatus.TIMEOUT
            logger.info(
                "세션 타임아웃으로 만료 처리",
                extra={
                    "correlation_id": sid,
                    "feature": session.feature,
                    "last_activity": session.last_activity.isoformat(),
                },
            )
            del self._sessions[sid]

        if expired_ids:
            logger.info(f"만료 세션 {len(expired_ids)}개 정리 완료")

        return len(expired_ids)

    def is_session_force_completed(self, session_id: str) -> bool:
        """세션이 최대 턴 수 초과로 강제 완료되었는지 확인한다.

        Args:
            session_id: 대상 세션 ID

        Returns:
            강제 완료 여부
        """
        session = self._sessions.get(session_id)
        if session is None:
            return False
        return session.status == SessionStatus.FORCE_COMPLETED

    def _get_session(self, session_id: str) -> Session:
        """내부 세션 조회 헬퍼.

        Raises:
            SessionNotFoundError: 세션이 존재하지 않을 때
        """
        session = self._sessions.get(session_id)
        if session is None:
            raise SessionNotFoundError(session_id)
        return session
