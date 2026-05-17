"""action 기반 메시지 라우팅.

SQS에서 수신된 메시지의 `action` 필드를 기반으로
등록된 핸들러로 라우팅한다.

역직렬화 실패 또는 알 수 없는 action의 경우
에러를 로깅하고 메시지를 폐기한다 (예외 전파 없음).

사용 예시:
    from app.sqs.router import MessageRouter

    router = MessageRouter()
    router.register_handler("GENERATE_QUIZ", quiz_handler.handle)
    router.register_handler("START_SESSION", retro_handler.handle)

    # SQS Poller에서 호출
    await router.route("quiz-requests", raw_sqs_message)
"""

from __future__ import annotations

import json
from collections.abc import Callable, Coroutine
from typing import Any

from app.common.exceptions import MessageParseError
from app.common.logging import get_logger

logger = get_logger(__name__)

# Type alias for async handler: receives parsed message body dict
ActionHandler = Callable[[dict[str, Any]], Coroutine[Any, Any, None]]


class MessageRouter:
    """메시지 action 필드 기반 라우팅.

    SQS 메시지의 Body를 JSON으로 파싱한 뒤 `action` 필드를 추출하여
    등록된 핸들러로 전달한다.

    설계 원칙:
    - 역직렬화 실패 시 에러 로깅 후 메시지 폐기 (예외 전파 없음)
    - 등록되지 않은 action 수신 시 경고 로깅 후 메시지 폐기
    - 핸들러 실행 중 예외 발생 시 에러 로깅 후 예외 재전파 (상위에서 처리)
    """

    def __init__(self) -> None:
        self._handlers: dict[str, ActionHandler] = {}

    def register_handler(self, action: str, handler: ActionHandler) -> None:
        """action별 핸들러를 등록한다.

        동일한 action에 대해 중복 등록하면 마지막 핸들러로 덮어쓴다.

        Args:
            action: 라우팅 키 (예: "GENERATE_QUIZ", "START_SESSION")
            handler: 해당 action의 메시지를 처리할 비동기 함수.
                     파싱된 메시지 본문 dict를 인자로 받는다.
        """
        self._handlers[action] = handler
        logger.info(f"핸들러 등록: action='{action}'")

    async def route(self, queue_name: str, message: dict[str, Any]) -> None:
        """SQS 메시지를 적절한 핸들러로 라우팅한다.

        1. 메시지 Body를 JSON으로 파싱
        2. `action` 필드 추출
        3. 등록된 핸들러 호출

        역직렬화 실패 또는 action 필드 누락 시 에러를 로깅하고
        메시지를 폐기한다 (예외를 전파하지 않음).

        핸들러 실행 중 발생한 예외는 로깅 후 재전파하여
        상위 레이어(Poller)에서 메시지 삭제 여부를 결정하도록 한다.

        Args:
            queue_name: 메시지가 수신된 큐 이름 (로깅용).
            message: SQS 원본 메시지 딕셔너리 (Body 필드 포함).
        """
        # 1. 메시지 Body 추출 및 JSON 파싱
        body = self._parse_message_body(message)
        if body is None:
            # 파싱 실패 - 이미 로깅됨, 메시지 폐기
            return

        # 2. action 필드 추출
        action = body.get("action")
        if not action:
            logger.error(
                "메시지에 'action' 필드가 없습니다. 메시지를 폐기합니다.",
                extra={
                    "error_type": "MissingActionField",
                    "error_detail": f"queue={queue_name}, keys={list(body.keys())}",
                },
            )
            return

        # 3. 등록된 핸들러 조회
        handler = self._handlers.get(action)
        if handler is None:
            logger.warning(
                f"등록되지 않은 action입니다. 메시지를 폐기합니다: "
                f"action='{action}', queue='{queue_name}'"
            )
            return

        # 4. 핸들러 실행
        await handler(body)

    def _parse_message_body(
        self, message: dict[str, Any]
    ) -> dict[str, Any] | None:
        """SQS 메시지의 Body를 JSON으로 파싱한다.

        파싱 실패 시 에러를 로깅하고 None을 반환한다.

        Args:
            message: SQS 원본 메시지 딕셔너리.

        Returns:
            파싱된 메시지 본문 dict, 또는 실패 시 None.
        """
        raw_body = message.get("Body", "")

        if not raw_body:
            logger.error(
                "메시지에 Body가 없습니다. 메시지를 폐기합니다.",
                extra={
                    "error_type": "EmptyMessageBody",
                    "error_detail": f"MessageId={message.get('MessageId', 'unknown')}",
                },
            )
            return None

        try:
            parsed = json.loads(raw_body)
        except (json.JSONDecodeError, TypeError) as exc:
            # 역직렬화 실패 - 로깅 후 메시지 폐기
            snippet = str(raw_body)[:200]
            parse_error = MessageParseError(raw_snippet=snippet)
            logger.error(
                f"메시지 역직렬화 실패. 메시지를 폐기합니다: {parse_error}",
                extra={
                    "error_type": "MessageParseError",
                    "error_detail": str(exc),
                },
            )
            return None

        if not isinstance(parsed, dict):
            logger.error(
                "메시지 본문이 JSON 객체가 아닙니다. 메시지를 폐기합니다.",
                extra={
                    "error_type": "InvalidMessageFormat",
                    "error_detail": f"type={type(parsed).__name__}",
                },
            )
            return None

        return parsed

    @property
    def registered_actions(self) -> list[str]:
        """등록된 모든 action 목록을 반환한다."""
        return list(self._handlers.keys())
