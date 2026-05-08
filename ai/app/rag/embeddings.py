"""임베딩 생성 (Amazon Titan Embeddings).

Bedrock Runtime API를 통해 Amazon Titan Embeddings 모델을 호출하여
텍스트를 벡터 임베딩으로 변환한다.

사용 예시:
    from app.rag.embeddings import EmbeddingGenerator
    from app.config import get_settings

    settings = get_settings()
    generator = EmbeddingGenerator(settings)

    # 단일 텍스트 임베딩 생성
    vector = await generator.generate("안녕하세요")

    # 여러 텍스트 임베딩 일괄 생성
    vectors = await generator.generate_batch(["텍스트1", "텍스트2"])
"""

import asyncio
import json
import logging

import aioboto3

from app.common.exceptions import BedrockInvocationError
from app.config import Settings

logger = logging.getLogger(__name__)


class EmbeddingGenerator:
    """Amazon Titan Embeddings를 사용한 텍스트 임베딩 생성기.

    Bedrock Runtime API를 통해 Amazon Titan Text Embeddings V2 모델을
    호출하여 텍스트를 고차원 벡터로 변환한다.

    Attributes:
        model_id: 사용할 임베딩 모델 ID
        timeout_seconds: API 호출 타임아웃 (초)
    """

    def __init__(self, settings: Settings) -> None:
        self.model_id = settings.bedrock_embedding_model_id
        self.timeout_seconds = settings.bedrock_timeout_seconds
        self._region = settings.aws_region
        self._session = aioboto3.Session(region_name=self._region)

    async def generate(self, text: str) -> list[float]:
        """단일 텍스트의 임베딩 벡터를 생성한다.

        Args:
            text: 임베딩을 생성할 텍스트

        Returns:
            텍스트의 임베딩 벡터 (float 리스트)

        Raises:
            BedrockInvocationError: API 호출 실패 또는 타임아웃 시
        """
        body = {
            "inputText": text,
        }

        try:
            result = await asyncio.wait_for(
                self._invoke_embedding_api(body),
                timeout=self.timeout_seconds,
            )
        except asyncio.TimeoutError:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=f"임베딩 생성 타임아웃 ({self.timeout_seconds}초 초과)",
            )
        except BedrockInvocationError:
            raise
        except Exception as e:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=f"임베딩 생성 실패: {e}",
            )

        return result

    async def generate_batch(self, texts: list[str]) -> list[list[float]]:
        """여러 텍스트의 임베딩 벡터를 일괄 생성한다.

        각 텍스트에 대해 개별적으로 임베딩을 생성한다.
        하나의 텍스트가 실패하면 전체 배치가 실패한다.

        Args:
            texts: 임베딩을 생성할 텍스트 리스트

        Returns:
            각 텍스트에 대한 임베딩 벡터 리스트

        Raises:
            BedrockInvocationError: API 호출 실패 시
        """
        results: list[list[float]] = []
        for text in texts:
            embedding = await self.generate(text)
            results.append(embedding)
        return results

    async def _invoke_embedding_api(self, body: dict) -> list[float]:
        """Bedrock Runtime API를 호출하여 임베딩을 생성한다.

        Args:
            body: 요청 본문 딕셔너리

        Returns:
            임베딩 벡터 (float 리스트)

        Raises:
            BedrockInvocationError: API 에러 발생 시
        """
        try:
            async with self._session.client(
                "bedrock-runtime",
                region_name=self._region,
            ) as client:
                response = await client.invoke_model(
                    modelId=self.model_id,
                    contentType="application/json",
                    accept="application/json",
                    body=json.dumps(body),
                )
                response_body = await response["body"].read()
                result = json.loads(response_body)

                if "embedding" in result:
                    return result["embedding"]

                raise BedrockInvocationError(
                    model_id=self.model_id,
                    detail="응답에 embedding 필드가 없습니다",
                )
        except BedrockInvocationError:
            raise
        except Exception as e:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=str(e),
            )
