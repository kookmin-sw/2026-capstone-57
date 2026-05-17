"""SQS 라우터 단위 테스트."""

import json
from unittest.mock import AsyncMock

import pytest

from app.sqs.router import MessageRouter


@pytest.fixture
def router() -> MessageRouter:
    """빈 MessageRouter 인스턴스를 반환한다."""
    return MessageRouter()


@pytest.fixture
def mock_handler() -> AsyncMock:
    """비동기 핸들러 mock을 반환한다."""
    return AsyncMock()


def _make_sqs_message(body: dict | str | None = None) -> dict:
    """테스트용 SQS 메시지 딕셔너리를 생성한다."""
    msg: dict = {
        "MessageId": "test-message-id-123",
        "ReceiptHandle": "test-receipt-handle",
    }
    if body is not None:
        if isinstance(body, dict):
            msg["Body"] = json.dumps(body)
        else:
            msg["Body"] = body
    return msg


class TestMessageRouterRegistration:
    """핸들러 등록 관련 테스트."""

    def test_register_handler(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """핸들러를 등록하면 registered_actions에 포함된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)
        assert "GENERATE_QUIZ" in router.registered_actions

    def test_register_multiple_handlers(self, router: MessageRouter) -> None:
        """여러 핸들러를 등록할 수 있다."""
        router.register_handler("GENERATE_QUIZ", AsyncMock())
        router.register_handler("START_SESSION", AsyncMock())
        router.register_handler("ANSWER", AsyncMock())

        assert len(router.registered_actions) == 3
        assert "GENERATE_QUIZ" in router.registered_actions
        assert "START_SESSION" in router.registered_actions
        assert "ANSWER" in router.registered_actions

    def test_register_duplicate_action_overwrites(
        self, router: MessageRouter
    ) -> None:
        """동일한 action에 대해 중복 등록하면 마지막 핸들러로 덮어쓴다."""
        handler1 = AsyncMock()
        handler2 = AsyncMock()

        router.register_handler("GENERATE_QUIZ", handler1)
        router.register_handler("GENERATE_QUIZ", handler2)

        assert len(router.registered_actions) == 1


class TestMessageRouterRouting:
    """메시지 라우팅 관련 테스트."""

    @pytest.mark.asyncio
    async def test_route_valid_message(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """유효한 메시지는 올바른 핸들러로 라우팅된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        body = {"action": "GENERATE_QUIZ", "matchId": "match-123"}
        message = _make_sqs_message(body)

        await router.route("quiz-requests", message)

        mock_handler.assert_called_once_with(body)

    @pytest.mark.asyncio
    async def test_route_to_correct_handler(
        self, router: MessageRouter
    ) -> None:
        """여러 핸들러 중 action에 맞는 핸들러만 호출된다."""
        quiz_handler = AsyncMock()
        retro_handler = AsyncMock()

        router.register_handler("GENERATE_QUIZ", quiz_handler)
        router.register_handler("START_SESSION", retro_handler)

        body = {"action": "START_SESSION", "sessionId": "session-456"}
        message = _make_sqs_message(body)

        await router.route("retro-requests", message)

        retro_handler.assert_called_once_with(body)
        quiz_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_route_passes_full_body_to_handler(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """핸들러에 파싱된 전체 메시지 본문이 전달된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        body = {
            "action": "GENERATE_QUIZ",
            "matchId": "match-123",
            "targetProfile": {"name": "테스트"},
        }
        message = _make_sqs_message(body)

        await router.route("quiz-requests", message)

        called_body = mock_handler.call_args[0][0]
        assert called_body["matchId"] == "match-123"
        assert called_body["targetProfile"]["name"] == "테스트"


class TestMessageRouterErrorHandling:
    """에러 처리 관련 테스트 - 예외 전파 없음."""

    @pytest.mark.asyncio
    async def test_invalid_json_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """유효하지 않은 JSON은 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        message = _make_sqs_message("not valid json {{{")

        # 예외가 발생하지 않아야 한다
        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_empty_body_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """빈 Body는 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        message = {"MessageId": "test-id", "ReceiptHandle": "handle"}
        # Body 필드 없음

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_missing_action_field_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """action 필드가 없는 메시지는 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        body = {"matchId": "match-123"}  # action 필드 없음
        message = _make_sqs_message(body)

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_unknown_action_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """등록되지 않은 action은 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        body = {"action": "UNKNOWN_ACTION", "data": "test"}
        message = _make_sqs_message(body)

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_non_dict_json_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """JSON 배열 등 dict가 아닌 본문은 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        message = _make_sqs_message("[1, 2, 3]")

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_empty_string_body_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """빈 문자열 Body는 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        message = _make_sqs_message("")

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_null_action_does_not_raise(
        self, router: MessageRouter, mock_handler: AsyncMock
    ) -> None:
        """action이 null인 메시지는 예외 없이 폐기된다."""
        router.register_handler("GENERATE_QUIZ", mock_handler)

        body = {"action": None, "data": "test"}
        message = _make_sqs_message(body)

        await router.route("quiz-requests", message)

        mock_handler.assert_not_called()

    @pytest.mark.asyncio
    async def test_handler_exception_propagates(
        self, router: MessageRouter
    ) -> None:
        """핸들러에서 발생한 예외는 상위로 전파된다."""
        failing_handler = AsyncMock(
            side_effect=RuntimeError("handler failed")
        )
        router.register_handler("GENERATE_QUIZ", failing_handler)

        body = {"action": "GENERATE_QUIZ", "matchId": "match-123"}
        message = _make_sqs_message(body)

        with pytest.raises(RuntimeError, match="handler failed"):
            await router.route("quiz-requests", message)
