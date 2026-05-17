"""SQS 응답 메시지 발행.

Pydantic 모델을 JSON 직렬화하여 지정된 SQS 큐로 전송한다.
설정 가능한 재시도 로직을 포함하며, 재시도 실패 시 에러를 로깅한다.

사용 예시:
    from app.sqs.publisher import SQSPublisher
    from app.config import get_settings

    settings = get_settings()
    publisher = SQSPublisher(
        region=settings.aws_region,
        max_retries=settings.sqs_publish_max_retries,
    )
    await publisher.publish(queue_url, response_message)
"""

from __future__ import annotations

import asyncio

import aioboto3
from pydantic import BaseModel
from typing import Any

from app.common.logging import get_logger

logger = get_logger(__name__)


class SQSPublisher:
    """SQS 응답 메시지 발행.

    Pydantic 모델을 JSON으로 직렬화하여 지정된 큐로 전송한다.
    실패 시 설정 가능한 횟수만큼 재시도하며, 재시도 소진 시 에러를 로깅한다.

    Args:
        region: AWS 리전 이름.
        max_retries: 발행 실패 시 최대 재시도 횟수 (기본 3).
        retry_base_delay: 재시도 간 기본 대기 시간 (초). 지수 백오프 적용.
        endpoint_url: SQS 엔드포인트 URL (선택, 로컬 테스트용).
    """

    def __init__(
        self,
        region: str = "ap-northeast-2",
        max_retries: int = 3,
        retry_base_delay: float = 0.5,
        endpoint_url: str | None = None,
    ) -> None:
        self._region = region
        self._max_retries = max_retries
        self._retry_base_delay = retry_base_delay
        self._endpoint_url = endpoint_url

        self._session = aioboto3.Session(region_name=self._region)

    async def publish(self, queue_url: str, message: BaseModel) -> bool:
        """메시지를 JSON 직렬화하여 큐로 전송한다.

        재시도 없이 단일 시도로 메시지를 발행한다.

        Args:
            queue_url: 대상 SQS 큐 URL.
            message: 발행할 Pydantic 모델 인스턴스.

        Returns:
            발행 성공 여부.
        """
        message_body = message.model_dump_json()
        queue_name = queue_url.split("/")[-1]

        client_kwargs: dict[str, Any] = {
            "region_name": self._region,
        }
        if self._endpoint_url:
            client_kwargs["endpoint_url"] = self._endpoint_url

        try:
            async with self._session.client("sqs", **client_kwargs) as sqs_client:
                response = await sqs_client.send_message(
                    QueueUrl=queue_url,
                    MessageBody=message_body,
                )
            msg_id = response.get("MessageId", "unknown")
            logger.info(f"메시지 발행 성공: queue={queue_name}, MessageId={msg_id}, QueueUrl={queue_url}")
            return True
        except Exception:
            logger.exception(f"메시지 발행 실패: queue={queue_name}")
            return False

    async def publish_with_retry(
        self,
        queue_url: str,
        message: BaseModel,
        max_retries: int | None = None,
    ) -> bool:
        """재시도 로직을 포함하여 메시지를 발행한다.

        지수 백오프를 적용하여 설정된 횟수만큼 재시도한다.
        모든 재시도가 실패하면 에러를 로깅하고 False를 반환한다.

        Args:
            queue_url: 대상 SQS 큐 URL.
            message: 발행할 Pydantic 모델 인스턴스.
            max_retries: 최대 재시도 횟수 (None이면 인스턴스 기본값 사용).

        Returns:
            발행 성공 여부.
        """
        retries = max_retries if max_retries is not None else self._max_retries
        queue_name = queue_url.split("/")[-1]
        message_body = message.model_dump_json()

        client_kwargs: dict[str, Any] = {
            "region_name": self._region,
        }
        if self._endpoint_url:
            client_kwargs["endpoint_url"] = self._endpoint_url

        last_exception: Exception | None = None

        for attempt in range(1, retries + 1):
            try:
                async with self._session.client(
                    "sqs", **client_kwargs
                ) as sqs_client:
                    response = await sqs_client.send_message(
                        QueueUrl=queue_url,
                        MessageBody=message_body,
                    )
                msg_id = response.get("MessageId", "unknown")
                logger.info(
                    f"메시지 발행 성공: queue={queue_name}, MessageId={msg_id}, QueueUrl={queue_url}",
                    extra={"attempt": attempt},
                )
                return True
            except Exception as exc:
                last_exception = exc
                logger.warning(
                    f"메시지 발행 실패 (시도 {attempt}/{retries}): "
                    f"queue={queue_name}",
                    extra={"attempt": attempt},
                )

                # 마지막 시도가 아니면 지수 백오프 대기
                if attempt < retries:
                    delay = self._retry_base_delay * (2 ** (attempt - 1))
                    await asyncio.sleep(delay)

        # 모든 재시도 소진
        logger.error(
            f"메시지 발행 최종 실패 (재시도 소진): queue={queue_name}",
            exc_info=last_exception,
            extra={"attempt": retries},
        )
        return False
