"""app/config.py 설정 모듈 단위 테스트."""

from app.config import Settings, get_settings


class TestSettings:
    """Settings 클래스 테스트."""

    def test_instantiate_without_env_vars(self):
        """환경 변수 없이 Settings를 인스턴스화할 수 있어야 한다."""
        s = Settings()
        assert s is not None

    def test_aws_defaults(self):
        """AWS 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.aws_region == "ap-northeast-2"

    def test_bedrock_defaults(self):
        """Bedrock 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.bedrock_model_id == "anthropic.claude-3-sonnet-20240229-v1:0"
        assert s.bedrock_max_tokens == 4096
        assert s.bedrock_timeout_seconds == 30
        assert s.bedrock_embedding_model_id == "amazon.titan-embed-text-v2:0"

    def test_sqs_defaults(self):
        """SQS 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.sqs_quiz_request_queue == "quiz-generation-requests"
        assert s.sqs_quiz_response_queue == "quiz-generation-responses"
        assert s.sqs_retro_request_queue == "retro-session-requests"
        assert s.sqs_retro_response_queue == "retro-session-responses"
        assert s.sqs_diary_request_queue == "diary-session-requests"
        assert s.sqs_diary_response_queue == "diary-session-responses"
        assert s.sqs_max_concurrent_messages == 5
        assert s.sqs_poll_interval_seconds == 1.0
        assert s.sqs_publish_max_retries == 3

    def test_conversation_defaults(self):
        """대화 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.conversation_max_turns == 5
        assert s.conversation_session_timeout_minutes == 30

    def test_rag_defaults(self):
        """RAG 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.chroma_persist_directory == "./data/chroma"
        assert s.rag_top_k == 3

    def test_server_defaults(self):
        """서버 관련 기본값이 올바르게 설정되어야 한다."""
        s = Settings()
        assert s.server_port == 8081
        assert s.request_timeout_seconds == 60

    def test_non_credential_fields_not_none(self):
        """모든 필드는 None이 아니어야 한다."""
        s = Settings()
        for field_name in Settings.model_fields:
            value = getattr(s, field_name)
            assert value is not None, f"{field_name} should not be None"

    def test_env_override(self, monkeypatch):
        """환경 변수로 설정을 오버라이드할 수 있어야 한다."""
        monkeypatch.setenv("AWS_REGION", "us-east-1")
        monkeypatch.setenv("SERVER_PORT", "9090")
        monkeypatch.setenv("BEDROCK_MAX_TOKENS", "2048")

        s = Settings()
        assert s.aws_region == "us-east-1"
        assert s.server_port == 9090
        assert s.bedrock_max_tokens == 2048


class TestGetSettings:
    """get_settings() 함수 테스트."""

    def test_returns_settings_instance(self):
        """Settings 인스턴스를 반환해야 한다."""
        get_settings.cache_clear()
        s = get_settings()
        assert isinstance(s, Settings)

    def test_returns_cached_instance(self):
        """동일한 캐시된 인스턴스를 반환해야 한다."""
        get_settings.cache_clear()
        s1 = get_settings()
        s2 = get_settings()
        assert s1 is s2
