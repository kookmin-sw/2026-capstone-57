"""SQS Poller 단위 테스트.

aioboto3 클라이언트를 mock하여 SQSPoller의 동작을 검증한다.
"""

import asyncio
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.sqs.poller import SQSPoller


class TestSQSPollerInit:
    """SQSPoller 초기화 테스트."""

    def test_default_values(self):
        """기본값으로 초기화된다."""
        poller = SQSPoller()
        assert poller._region == "ap-northeast-2"
        assert poller._poll_interval == 1.0
        assert poller._max_concurrent == 5
        assert not poller.is_running

    def test_custom_values(self):
        """사용자 지정 값으로 초기화된다."""
        poller = SQSPoller(
            region="us-east-1",
            poll_interval=2.0,
            max_concurrent=3,
        )
        assert poller._region == "us-east-1"
        assert poller._poll_interval == 2.0
        assert poller._max_concurrent == 3

    def test_max_concurrent_clamped_to_10(self):
        """max_concurrent는 10을 초과할 수 없다."""
        poller = SQSPoller(max_concurrent=20)
        assert poller._max_concurrent == 10

    def test_max_concurrent_clamped_to_1(self):
        """max_concurrent는 1 미만일 수 없다."""
        poller = SQSPoller(max_concurrent=0)
        assert poller._max_concurrent == 1

    def test_endpoint_url_stored(self):
        """endpoint_url이 저장된다."""
        poller = SQSPoller(endpoint_url="http://localhost:4566")
        assert poller._endpoint_url == "http://localhost:4566"


class TestSQSPollerRegisterQueue:
    """큐 등록 테스트."""

    def test_register_queue(self):
        """큐와 핸들러를 등록할 수 있다."""
        poller = SQSPoller()

        async def handler(msg):
            pass

        poller.register_queue("https://sqs.../test-queue", handler)
        assert "https://sqs.../test-queue" in poller._queues

    def test_register_multiple_queues(self):
        """여러 큐를 등록할 수 있다."""
        poller = SQSPoller()

        async def handler1(msg):
            pass

        async def handler2(msg):
            pass

        poller.register_queue("https://sqs.../queue-1", handler1)
        poller.register_queue("https://sqs.../queue-2", handler2)
        assert len(poller._queues) == 2


class TestSQSPollerStartStop:
    """시작/종료 테스트."""

    @pytest.mark.asyncio
    async def test_start_without_queues_does_nothing(self):
        """등록된 큐가 없으면 시작하지 않는다."""
        poller = SQSPoller()
        await poller.start()
        assert not poller.is_running

    @pytest.mark.asyncio
    async def test_stop_when_not_running(self):
        """실행 중이 아닐 때 stop()은 안전하게 반환된다."""
        poller = SQSPoller()
        await poller.stop()  # 예외 없이 완료되어야 함

    @pytest.mark.asyncio
    async def test_start_sets_running(self):
        """start() 호출 후 is_running이 True가 된다."""
        poller = SQSPoller(poll_interval=0.1)

        async def handler(msg):
            pass

        poller.register_queue("https://sqs.../test-queue", handler)

        # Mock the session to avoid real AWS calls
        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = AsyncMock(
            return_value={"Messages": []}
        )

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            assert poller.is_running

            await poller.stop()
            assert not poller.is_running

    @pytest.mark.asyncio
    async def test_start_twice_is_idempotent(self):
        """이미 실행 중일 때 start()를 다시 호출해도 안전하다."""
        poller = SQSPoller(poll_interval=0.1)

        async def handler(msg):
            pass

        poller.register_queue("https://sqs.../test-queue", handler)

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = AsyncMock(
            return_value={"Messages": []}
        )

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await poller.start()  # 두 번째 호출은 무시됨
            assert len(poller._tasks) == 1

            await poller.stop()

    @pytest.mark.asyncio
    async def test_creates_task_per_queue(self):
        """등록된 큐 수만큼 태스크가 생성된다."""
        poller = SQSPoller(poll_interval=0.1)

        async def handler(msg):
            pass

        poller.register_queue("https://sqs.../queue-1", handler)
        poller.register_queue("https://sqs.../queue-2", handler)
        poller.register_queue("https://sqs.../queue-3", handler)

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = AsyncMock(
            return_value={"Messages": []}
        )

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            assert len(poller._tasks) == 3

            await poller.stop()


class TestSQSPollerMessageProcessing:
    """메시지 처리 테스트."""

    @pytest.mark.asyncio
    async def test_processes_message_from_queue(self):
        """큐에서 메시지를 수신하고 핸들러로 전달한다."""
        received_messages: list[dict] = []

        async def handler(msg):
            received_messages.append(msg)

        test_message = {
            "MessageId": "msg-001",
            "ReceiptHandle": "receipt-001",
            "Body": json.dumps({"action": "TEST", "data": "hello"}),
        }

        call_count = 0

        async def mock_receive(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return {"Messages": [test_message]}
            return {"Messages": []}

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = mock_receive
        mock_sqs_client.delete_message = AsyncMock()

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.3)
            await poller.stop()

        assert len(received_messages) == 1
        assert received_messages[0]["Body"] == json.dumps(
            {"action": "TEST", "data": "hello"}
        )

    @pytest.mark.asyncio
    async def test_deletes_message_after_successful_processing(self):
        """처리 성공 후 메시지를 큐에서 삭제한다."""

        async def handler(msg):
            pass  # 성공적으로 처리

        test_message = {
            "MessageId": "msg-001",
            "ReceiptHandle": "receipt-001",
            "Body": json.dumps({"action": "TEST"}),
        }

        call_count = 0

        async def mock_receive(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return {"Messages": [test_message]}
            return {"Messages": []}

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = mock_receive
        mock_sqs_client.delete_message = AsyncMock()

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.3)
            await poller.stop()

        mock_sqs_client.delete_message.assert_called_once_with(
            QueueUrl="https://sqs.../test-queue",
            ReceiptHandle="receipt-001",
        )

    @pytest.mark.asyncio
    async def test_handler_exception_does_not_crash_poller(self):
        """핸들러에서 예외가 발생해도 폴러는 계속 동작한다."""
        call_count = 0

        async def handler(msg):
            nonlocal call_count
            call_count += 1
            body = json.loads(msg["Body"])
            if body["action"] == "FAIL":
                raise ValueError("의도적 에러")

        messages = [
            {
                "MessageId": "msg-001",
                "ReceiptHandle": "receipt-001",
                "Body": json.dumps({"action": "FAIL"}),
            },
            {
                "MessageId": "msg-002",
                "ReceiptHandle": "receipt-002",
                "Body": json.dumps({"action": "OK"}),
            },
        ]

        receive_call_count = 0

        async def mock_receive(*args, **kwargs):
            nonlocal receive_call_count
            receive_call_count += 1
            if receive_call_count == 1:
                return {"Messages": messages}
            return {"Messages": []}

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = mock_receive
        mock_sqs_client.delete_message = AsyncMock()

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.3)
            await poller.stop()

        # 폴러가 크래시하지 않고 두 메시지 모두 처리 시도
        assert call_count == 2
        # 실패한 메시지는 삭제되지 않음 (OK 메시지만 삭제)
        mock_sqs_client.delete_message.assert_called_once_with(
            QueueUrl="https://sqs.../test-queue",
            ReceiptHandle="receipt-002",
        )

    @pytest.mark.asyncio
    async def test_does_not_delete_message_on_handler_failure(self):
        """핸들러 실패 시 메시지를 삭제하지 않는다."""

        async def handler(msg):
            raise RuntimeError("처리 실패")

        test_message = {
            "MessageId": "msg-001",
            "ReceiptHandle": "receipt-001",
            "Body": json.dumps({"action": "TEST"}),
        }

        call_count = 0

        async def mock_receive(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return {"Messages": [test_message]}
            return {"Messages": []}

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = mock_receive
        mock_sqs_client.delete_message = AsyncMock()

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.3)
            await poller.stop()

        # 핸들러 실패 시 메시지 삭제하지 않음
        mock_sqs_client.delete_message.assert_not_called()

    @pytest.mark.asyncio
    async def test_graceful_shutdown(self):
        """stop() 호출 시 폴링이 중지되고 리소스가 정리된다."""

        async def handler(msg):
            pass

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = AsyncMock(
            return_value={"Messages": []}
        )

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            assert poller.is_running

            await asyncio.sleep(0.2)
            await poller.stop()

            assert not poller.is_running
            assert len(poller._tasks) == 0
            assert poller._session is None

    @pytest.mark.asyncio
    async def test_multiple_messages_processed_concurrently(self):
        """여러 메시지가 동시에 처리된다."""
        processing_order: list[str] = []

        async def handler(msg):
            msg_id = msg["MessageId"]
            processing_order.append(f"start-{msg_id}")
            await asyncio.sleep(0.05)
            processing_order.append(f"end-{msg_id}")

        messages = [
            {
                "MessageId": f"msg-{i:03d}",
                "ReceiptHandle": f"receipt-{i:03d}",
                "Body": json.dumps({"action": "TEST", "index": i}),
            }
            for i in range(3)
        ]

        call_count = 0

        async def mock_receive(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return {"Messages": messages}
            return {"Messages": []}

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = mock_receive
        mock_sqs_client.delete_message = AsyncMock()

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1, max_concurrent=5)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.5)
            await poller.stop()

        # 3개 메시지 모두 처리됨
        assert len([x for x in processing_order if x.startswith("end-")]) == 3

    @pytest.mark.asyncio
    async def test_uses_configured_max_messages(self):
        """설정된 max_concurrent 값이 receive_message에 전달된다."""

        async def handler(msg):
            pass

        mock_sqs_client = AsyncMock()
        mock_sqs_client.receive_message = AsyncMock(
            return_value={"Messages": []}
        )

        mock_context = AsyncMock()
        mock_context.__aenter__ = AsyncMock(return_value=mock_sqs_client)
        mock_context.__aexit__ = AsyncMock(return_value=None)

        poller = SQSPoller(poll_interval=0.1, max_concurrent=7)
        poller.register_queue("https://sqs.../test-queue", handler)

        with patch("aioboto3.Session") as mock_session_cls:
            mock_session = MagicMock()
            mock_session.client.return_value = mock_context
            mock_session_cls.return_value = mock_session

            await poller.start()
            await asyncio.sleep(0.3)
            await poller.stop()

        # receive_message가 MaxNumberOfMessages=7로 호출됨
        calls = mock_sqs_client.receive_message.call_args_list
        assert len(calls) > 0
        assert calls[0].kwargs["MaxNumberOfMessages"] == 7
