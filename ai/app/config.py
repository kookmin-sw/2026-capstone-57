"""pydantic-settings 기반 설정 모듈.

환경 변수와 .env 파일에서 설정을 로드하며,
로컬 개발을 위한 합리적인 기본값을 제공한다.

사용 예시:
    from app.config import get_settings

    settings = get_settings()
    print(settings.aws_region)
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """AI 서비스 전체 설정.

    환경 변수 또는 .env 파일에서 값을 로드한다.
    AWS 자격증명을 제외한 모든 필드는 합리적인 기본값을 가진다.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # AWS
    aws_region: str = "ap-northeast-2"

    # Bedrock
    bedrock_model_id: str = "anthropic.claude-3-sonnet-20240229-v1:0"
    bedrock_max_tokens: int = 4096
    bedrock_timeout_seconds: int = 30
    bedrock_embedding_model_id: str = "amazon.titan-embed-text-v2:0"

    # SQS Queues
    sqs_quiz_request_queue: str = "quiz-generation-requests"
    sqs_quiz_response_queue: str = "quiz-generation-responses"
    sqs_retro_request_queue: str = "retro-session-requests"
    sqs_retro_response_queue: str = "retro-session-responses"
    sqs_diary_request_queue: str = "diary-session-requests"
    sqs_diary_response_queue: str = "diary-session-responses"
    sqs_max_concurrent_messages: int = 5
    sqs_poll_interval_seconds: float = 1.0
    sqs_publish_max_retries: int = 3

    # Conversation
    conversation_max_turns: int = 5
    conversation_session_timeout_minutes: int = 30

    # RAG
    chroma_persist_directory: str = "./data/chroma"
    rag_top_k: int = 3

    # Server
    server_port: int = 8081

    # Request timeout
    request_timeout_seconds: int = 60


@lru_cache
def get_settings() -> Settings:
    """캐시된 Settings 인스턴스를 반환한다.

    FastAPI의 Depends()와 함께 사용하여 의존성 주입에 활용할 수 있다.

    사용 예시:
        @app.get("/example")
        def example(settings: Settings = Depends(get_settings)):
            return {"region": settings.aws_region}
    """
    return Settings()
