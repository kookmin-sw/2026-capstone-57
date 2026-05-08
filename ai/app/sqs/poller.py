"""asyncio 기반 SQS 메시지 폴러.

각 기능별 큐를 독립적인 asyncio 태스크로 동시에 폴링하며,
설정 가능한 폴링 간격과 최대 동시 메시지 수를 지원한다.

사용 예시:
    from app.sqs.poller import SQSPoller
    from app.config import get_settings

    settings = get_settings()
    poller = SQSPoller(
        region=settings.aws_region,
        poll_interval=settings.sqs_poll_interval_seconds,
        max_concurrent=settings.sqs_max_concurrent_messages,
    )
    poller.register_queue("https://sqs.../quiz-requests", handle_quiz)
    await poller.start()
"""

import asyncio
from collections.abc import Callable, Coroutine
from typing import Any

import aioboto3

from app.common.logging import get_logger

logger = get_logger(__name__)

# Type alias for async message handler: receives raw message dict
MessageHandler = Callable[[dict[str, Any]], Coroutine[Any, Any, None]]


class SQSPoller:
    """asyncio 기반 SQS 메시지 폴러.

    큐별 독립 asyncio 태스크를 생성하여 동시에 폴링하며,
    graceful shutdown을 지원한다.

    Args:
        region: AWS 리전 이름.
        poll_interval: 폴링 간격 (초). 메시지가 없을 때 대기 시간.
        max_concurrent: 큐당 한 번에 수신할 최대 메시지 수 (1-10).
        aws_access_key_id: AWS 액세스 키 (선택, None이면 기본 자격증명 체인 사용).
        aws_secret_access_key: AWS 시크릿 키 (선택).
        endpoint_url: SQS 엔드포인트 URL (선택, 로컬 테스트용).
    """

    def __init__(
        self,
        region: str = "ap-northeast-2",
        poll_interval: float = 1.0,
        max_concurrent: int = 5,
        aws_access_key_id: str | None = None,
        aws_secret_access_key: str | None = None,
        endpoint_url: str | None = None,
    ) -> None:
        self._region = region
        self._poll_interval = poll_interval
        self._max_concurrent = min(max(max_concurrent, 1), 10)
        self._aws_access_key_id = aws_access_key_id
        self._aws_secret_access_key = aws_secret_access_key
        self._endpoint_url = endpoint_url

        # queue_url -> handler mapping
        self._queues: dict[str, MessageHandler] = {}
        # Running polling tasks
        self._tasks: list[asyncio.Task[None]] = []
        self._running = False
        self._session: aioboto3.Session | None = None

    def register_queue(self, queue_url: str, handler: MessageHandler) -> None:
        """폴링할 큐와 메시지 핸들러를 등록한다.

        Args:
            queue_url: SQS 큐 URL.
            handler: 메시지를 처리할 비동기 콜백 함수.
                     dict 형태의 메시지 본문을 인자로 받는다.
        """
        self._queues[queue_url] = handler
        logger.info(f"큐 등록: {queue_url}")

    async def start(self) -> None:
        """등록된 모든 큐에 대한 폴링 태스크를 시작한다.

        각 큐마다 독립적인 asyncio 태스크를 생성하여 동시에 폴링한다.
        이미 실행 중이면 아무 작업도 하지 않는다.
        """
        if self._running:
            logger.warning("SQS Poller가 이미 실행 중입니다.")
            return

        if not self._queues:
            logger.warning("등록된 큐가 없습니다. 폴링을 시작하지 않습니다.")
            return

        self._running = True

        session_kwargs: dict[str, Any] = {"region_name": self._region}
        if self._aws_access_key_id:
            session_kwargs["aws_access_key_id"] = self._aws_access_key_id
        if self._aws_secret_access_key:
            session_kwargs["aws_secret_access_key"] = self._aws_secret_access_key
        self._session = aioboto3.Session(**session_kwargs)

        for queue_url, handler in self._queues.items():
            task = asyncio.create_task(
                self._poll_queue(queue_url, handler),
                name=f"sqs-poll-{queue_url.split('/')[-1]}",
            )
            self._tasks.append(task)

        logger.info(
            f"SQS Poller 시작: {len(self._tasks)}개 큐 폴링 중",
        )

    async def stop(self) -> None:
        """모든 폴링 태스크를 중지하고 리소스를 정리한다.

        진행 중인 메시지 처리가 완료될 때까지 대기한 후 종료한다.
        """
        if not self._running:
            return

        logger.info("SQS Poller 종료 시작...")
        self._running = False

        # 모든 태스크가 자연스럽게 종료될 때까지 대기
        if self._tasks:
            await asyncio.gather(*self._tasks, return_exceptions=True)
            self._tasks.clear()

        self._session = None
        logger.info("SQS Poller 종료 완료.")

    @property
    def is_running(self) -> bool:
        """폴러가 현재 실행 중인지 반환한다."""
        return self._running

    async def _poll_queue(
        self, queue_url: str, handler: MessageHandler
    ) -> None:
        """단일 큐에 대한 폴링 루프.

        _running이 False가 될 때까지 반복적으로 메시지를 수신하고 처리한다.

        Args:
            queue_url: 폴링할 SQS 큐 URL.
            handler: 수신된 메시지를 처리할 비동기 핸들러.
        """
        queue_name = queue_url.split("/")[-1]
        logger.info(f"큐 폴링 시작: {queue_name}")

        assert self._session is not None

        client_kwargs: dict[str, Any] = {
            "region_name": self._region,
        }
        if self._endpoint_url:
            client_kwargs["endpoint_url"] = self._endpoint_url

        async with self._session.client("sqs", **client_kwargs) as sqs_client:
            while self._running:
                try:
                    response = await sqs_client.receive_message(
                        QueueUrl=queue_url,
                        MaxNumberOfMessages=self._max_concurrent,
                        WaitTimeSeconds=int(self._poll_interval),
                    )

                    messages = response.get("Messages", [])

                    if not messages:
                        # 메시지가 없으면 짧은 대기 후 재시도
                        await asyncio.sleep(self._poll_interval)
                        continue

                    # 수신된 메시지를 동시에 처리
                    tasks = [
                        self._process_message(
                            sqs_client, queue_url, message, handler
                        )
                        for message in messages
                    ]
                    await asyncio.gather(*tasks, return_exceptions=True)

                except asyncio.CancelledError:
                    logger.info(f"큐 폴링 취소됨: {queue_name}")
                    break
                except Exception:
                    logger.exception(
                        f"큐 폴링 중 예상치 못한 오류 발생: {queue_name}"
                    )
                    # 에러 발생 시 잠시 대기 후 재시도
                    await asyncio.sleep(self._poll_interval)

        logger.info(f"큐 폴링 종료: {queue_name}")

    async def _process_message(
        self,
        sqs_client: Any,
        queue_url: str,
        message: dict[str, Any],
        handler: MessageHandler,
    ) -> None:
        """단일 메시지를 처리하고 성공 시 큐에서 삭제한다.

        핸들러에서 예외가 발생하면 로깅하고 메시지를 삭제하지 않는다
        (SQS visibility timeout 후 재처리 가능).

        Args:
            sqs_client: aioboto3 SQS 클라이언트.
            queue_url: 메시지가 수신된 큐 URL.
            message: SQS 메시지 딕셔너리.
            handler: 메시지 처리 핸들러.
        """
        receipt_handle = message.get("ReceiptHandle", "")
        message_id = message.get("MessageId", "unknown")

        try:
            await handler(message)

            # 처리 성공 시 메시지 삭제
            await sqs_client.delete_message(
                QueueUrl=queue_url,
                ReceiptHandle=receipt_handle,
            )
        except Exception:
            logger.exception(
                f"메시지 처리 실패 (MessageId: {message_id})",
            )
