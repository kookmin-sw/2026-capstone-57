"""BedrockClient 단위 테스트.

moto 및 직접 mock을 사용하여 BedrockClient의 핵심 동작을 검증한다.
"""

import asyncio
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.bedrock.client import BedrockClient
from app.common.exceptions import BedrockInvocationError
from app.config import Settings


@pytest.fixture
def settings() -> Settings:
    """테스트용 Settings 인스턴스."""
    return Settings(
        aws_region="us-east-1",
        bedrock_model_id="anthropic.claude-3-sonnet-20240229-v1:0",
        bedrock_max_tokens=1024,
        bedrock_timeout_seconds=10,
    )


@pytest.fixture
def client(settings: Settings) -> BedrockClient:
    """테스트용 BedrockClient 인스턴스."""
    return BedrockClient(settings)


def _make_bedrock_response(text: str) -> dict:
    """Bedrock Claude Messages API 응답 형식을 생성한다."""
    return {
        "content": [{"type": "text", "text": text}],
        "model": "anthropic.claude-3-sonnet-20240229-v1:0",
        "stop_reason": "end_turn",
    }


class TestBedrockClientInit:
    """BedrockClient 초기화 테스트."""

    def test_uses_settings_values(self, settings: Settings) -> None:
        client = BedrockClient(settings)
        assert client.model_id == settings.bedrock_model_id
        assert client.max_tokens == settings.bedrock_max_tokens
        assert client.timeout_seconds == settings.bedrock_timeout_seconds

    def test_default_settings(self) -> None:
        default_settings = Settings()
        client = BedrockClient(default_settings)
        assert client.model_id == "anthropic.claude-3-sonnet-20240229-v1:0"
        assert client.max_tokens == 4096
        assert client.timeout_seconds == 30


class TestInvoke:
    """invoke 메서드 테스트."""

    async def test_invoke_returns_text(self, client: BedrockClient) -> None:
        expected = "안녕하세요! 도움이 필요하신가요?"
        response_body = json.dumps(_make_bedrock_response(expected)).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            result = await client.invoke("안녕하세요")

        assert result == expected

    async def test_invoke_with_custom_max_tokens(
        self, client: BedrockClient
    ) -> None:
        response_body = json.dumps(
            _make_bedrock_response("response")
        ).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            result = await client.invoke("test", max_tokens=512)

        # Verify the body sent to invoke_model
        call_kwargs = mock_client.invoke_model.call_args[1]
        body = json.loads(call_kwargs["body"])
        assert body["max_tokens"] == 512
        assert result == "response"

    async def test_invoke_raises_on_api_error(
        self, client: BedrockClient
    ) -> None:
        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            side_effect=Exception("AccessDeniedException")
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            with pytest.raises(BedrockInvocationError) as exc_info:
                await client.invoke("test")

        assert "AccessDeniedException" in exc_info.value.detail
        assert exc_info.value.model_id == client.model_id

    async def test_invoke_raises_on_timeout(
        self, client: BedrockClient
    ) -> None:
        client.timeout_seconds = 0.1

        async def slow_invoke(**kwargs):
            await asyncio.sleep(1.0)
            return {}

        mock_client = AsyncMock()
        mock_client.invoke_model = slow_invoke
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            with pytest.raises(BedrockInvocationError) as exc_info:
                await client.invoke("test")

        assert "타임아웃" in exc_info.value.detail

    async def test_invoke_raises_on_empty_content(
        self, client: BedrockClient
    ) -> None:
        response_body = json.dumps({"content": []}).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            with pytest.raises(BedrockInvocationError) as exc_info:
                await client.invoke("test")

        assert "content가 없습니다" in exc_info.value.detail


class TestInvokeWithMessages:
    """invoke_with_messages 메서드 테스트."""

    async def test_sends_system_prompt_and_messages(
        self, client: BedrockClient
    ) -> None:
        expected = "후속 응답입니다."
        response_body = json.dumps(_make_bedrock_response(expected)).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        messages = [
            {"role": "user", "content": "첫 번째 질문"},
            {"role": "assistant", "content": "첫 번째 답변"},
            {"role": "user", "content": "두 번째 질문"},
        ]

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            result = await client.invoke_with_messages(
                system_prompt="당신은 도움이 되는 어시스턴트입니다.",
                messages=messages,
            )

        assert result == expected

        # Verify request body structure
        call_kwargs = mock_client.invoke_model.call_args[1]
        body = json.loads(call_kwargs["body"])
        assert body["system"] == "당신은 도움이 되는 어시스턴트입니다."
        assert len(body["messages"]) == 3
        assert body["messages"][0]["role"] == "user"
        assert body["messages"][2]["content"] == "두 번째 질문"

    async def test_no_system_key_when_prompt_is_none(
        self, client: BedrockClient
    ) -> None:
        """system_prompt가 빈 문자열이면 body에 system 키가 없어야 한다."""
        response_body = json.dumps(
            _make_bedrock_response("response")
        ).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            # invoke uses _call_model with system_prompt=None
            await client.invoke("test")

        call_kwargs = mock_client.invoke_model.call_args[1]
        body = json.loads(call_kwargs["body"])
        assert "system" not in body


class TestInvokeWithRetry:
    """invoke_with_retry 메서드 테스트."""

    async def test_returns_on_first_success(
        self, client: BedrockClient
    ) -> None:
        response_body = json.dumps(
            _make_bedrock_response("success")
        ).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=response_body)

        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            return_value={"body": mock_body}
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            result = await client.invoke_with_retry("test", max_retries=2)

        assert result == "success"
        # Should only be called once on success
        assert mock_client.invoke_model.call_count == 1

    async def test_retries_on_failure_then_succeeds(
        self, client: BedrockClient
    ) -> None:
        success_body = json.dumps(
            _make_bedrock_response("retry success")
        ).encode()

        mock_body = AsyncMock()
        mock_body.read = AsyncMock(return_value=success_body)

        call_count = 0

        async def side_effect(**kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                raise Exception("Temporary failure")
            return {"body": mock_body}

        mock_client = AsyncMock()
        mock_client.invoke_model = side_effect
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            result = await client.invoke_with_retry("test", max_retries=1)

        assert result == "retry success"
        assert call_count == 2

    async def test_raises_after_all_retries_exhausted(
        self, client: BedrockClient
    ) -> None:
        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            side_effect=Exception("Persistent failure")
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            with pytest.raises(BedrockInvocationError) as exc_info:
                await client.invoke_with_retry("test", max_retries=2)

        assert "Persistent failure" in exc_info.value.detail

    async def test_retry_with_zero_retries(
        self, client: BedrockClient
    ) -> None:
        """max_retries=0이면 재시도 없이 한 번만 시도한다."""
        mock_client = AsyncMock()
        mock_client.invoke_model = AsyncMock(
            side_effect=Exception("Single attempt failure")
        )
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=None)

        with patch.object(
            client._session, "client", return_value=mock_client
        ):
            with pytest.raises(BedrockInvocationError):
                await client.invoke_with_retry("test", max_retries=0)

        # Only one call should have been made
        assert mock_client.invoke_model.call_count == 1
