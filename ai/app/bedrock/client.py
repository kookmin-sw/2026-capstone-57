"""AWS Bedrock Runtime 클라이언트.

Claude 모델과의 통신을 담당하며, 단일턴/멀티턴 호출 및
재시도 로직을 제공한다.

사용 예시:
    from app.bedrock.client import BedrockClient
    from app.config import get_settings

    settings = get_settings()
    client = BedrockClient(settings)

    # 단일 프롬프트 호출
    response = await client.invoke("안녕하세요")

    # 멀티턴 대화 호출
    response = await client.invoke_with_messages(
        system_prompt="당신은 도움이 되는 어시스턴트입니다.",
        messages=[{"role": "user", "content": "안녕하세요"}],
    )
"""

import asyncio
import json
import logging

import aioboto3

from app.common.exceptions import BedrockInvocationError
from app.config import Settings

logger = logging.getLogger(__name__)


class BedrockClient:
    """AWS Bedrock Claude 모델 클라이언트.

    boto3/aioboto3를 사용하여 Bedrock Runtime API를 호출한다.
    설정 가능한 모델 ID, 최대 토큰, 타임아웃을 지원하며,
    실패 시 BedrockInvocationError를 발생시킨다.

    Attributes:
        model_id: 사용할 Bedrock 모델 ID
        max_tokens: 응답의 최대 토큰 수
        timeout_seconds: API 호출 타임아웃 (초)
        region: AWS 리전
    """

    def __init__(self, settings: Settings) -> None:
        self.model_id = settings.bedrock_model_id
        self.max_tokens = settings.bedrock_max_tokens
        self.timeout_seconds = settings.bedrock_timeout_seconds
        self._region = settings.aws_region
        self._access_key_id = settings.aws_access_key_id
        self._secret_access_key = settings.aws_secret_access_key
        session_kwargs: dict[str, str] = {"region_name": self._region}
        if self._access_key_id:
            session_kwargs["aws_access_key_id"] = self._access_key_id
        if self._secret_access_key:
            session_kwargs["aws_secret_access_key"] = self._secret_access_key
        self._session = aioboto3.Session(**session_kwargs)

    async def invoke(self, prompt: str, max_tokens: int | None = None) -> str:
        """단일 프롬프트로 Claude 모델을 호출한다.

        Messages API 형식으로 단일 user 메시지를 전송하고
        assistant 응답 텍스트를 반환한다.

        Args:
            prompt: 모델에 전송할 프롬프트 텍스트
            max_tokens: 응답 최대 토큰 수 (None이면 설정 기본값 사용)

        Returns:
            모델의 텍스트 응답

        Raises:
            BedrockInvocationError: API 호출 실패 또는 타임아웃 시
        """
        messages = [{"role": "user", "content": prompt}]
        return await self._call_model(
            messages=messages,
            system_prompt=None,
            max_tokens=max_tokens or self.max_tokens,
        )

    async def invoke_with_messages(
        self,
        system_prompt: str,
        messages: list[dict],
        max_tokens: int | None = None,
    ) -> str:
        """메시지 히스토리로 Claude 모델을 호출한다 (멀티턴).

        시스템 프롬프트와 전체 대화 히스토리를 전송하여
        맥락을 유지한 응답을 생성한다.

        Args:
            system_prompt: 시스템 프롬프트 텍스트
            messages: 대화 히스토리 (role/content 딕셔너리 리스트)
            max_tokens: 응답 최대 토큰 수 (None이면 설정 기본값 사용)

        Returns:
            모델의 텍스트 응답

        Raises:
            BedrockInvocationError: API 호출 실패 또는 타임아웃 시
        """
        return await self._call_model(
            messages=messages,
            system_prompt=system_prompt,
            max_tokens=max_tokens or self.max_tokens,
        )

    async def invoke_with_retry(
        self,
        prompt: str,
        max_retries: int = 1,
        max_tokens: int | None = None,
    ) -> str:
        """재시도 로직을 포함하여 모델을 호출한다.

        첫 번째 호출이 실패하면 지정된 횟수만큼 재시도한다.
        모든 재시도가 실패하면 마지막 예외를 발생시킨다.

        Args:
            prompt: 모델에 전송할 프롬프트 텍스트
            max_retries: 최대 재시도 횟수 (기본 1회)
            max_tokens: 응답 최대 토큰 수 (None이면 설정 기본값 사용)

        Returns:
            모델의 텍스트 응답

        Raises:
            BedrockInvocationError: 모든 재시도 실패 시
        """
        last_error: BedrockInvocationError | None = None
        attempts = 1 + max_retries

        for attempt in range(1, attempts + 1):
            try:
                return await self.invoke(prompt, max_tokens=max_tokens)
            except BedrockInvocationError as e:
                last_error = e
                logger.warning(
                    "Bedrock 호출 실패 (시도 %d/%d): %s",
                    attempt,
                    attempts,
                    e.detail,
                    extra={
                        "model_id": self.model_id,
                        "attempt": attempt,
                        "max_attempts": attempts,
                    },
                )
                if attempt < attempts:
                    await asyncio.sleep(0.5 * attempt)

        raise last_error  # type: ignore[misc]

    async def _call_model(
        self,
        messages: list[dict],
        system_prompt: str | None,
        max_tokens: int,
    ) -> str:
        """Bedrock Runtime API를 호출하는 내부 메서드.

        Claude Messages API 형식으로 요청을 구성하고,
        타임아웃을 적용하여 호출한다.

        Args:
            messages: 대화 메시지 리스트
            system_prompt: 시스템 프롬프트 (None이면 생략)
            max_tokens: 응답 최대 토큰 수

        Returns:
            모델의 텍스트 응답

        Raises:
            BedrockInvocationError: API 호출 실패 또는 타임아웃 시
        """
        body = {
            "anthropic_version": "bedrock-2023-05-31",
            "max_tokens": max_tokens,
            "messages": [
                {"role": m["role"], "content": m["content"]} for m in messages
            ],
        }

        if system_prompt:
            body["system"] = system_prompt

        try:
            response_text = await asyncio.wait_for(
                self._invoke_api(body),
                timeout=self.timeout_seconds,
            )
        except asyncio.TimeoutError:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=f"타임아웃 ({self.timeout_seconds}초 초과)",
            )
        except BedrockInvocationError:
            raise
        except Exception as e:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=str(e),
            )

        return response_text

    async def _invoke_api(self, body: dict) -> str:
        """Bedrock Runtime invoke_model API를 호출한다.

        Args:
            body: 요청 본문 딕셔너리

        Returns:
            모델 응답에서 추출한 텍스트

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

                # Claude Messages API 응답 형식에서 텍스트 추출
                if "content" in result and len(result["content"]) > 0:
                    return result["content"][0]["text"]

                raise BedrockInvocationError(
                    model_id=self.model_id,
                    detail="응답에 content가 없습니다",
                )
        except BedrockInvocationError:
            raise
        except Exception as e:
            raise BedrockInvocationError(
                model_id=self.model_id,
                detail=str(e),
            )
