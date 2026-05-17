"""공통 예외 및 구조화된 로깅 모듈 테스트."""

import json
import logging

import pytest

from app.common.exceptions import (
    AIServiceError,
    BedrockInvocationError,
    MessageParseError,
    PublishError,
    SessionExpiredError,
    SessionNotFoundError,
)
from app.common.logging import (
    JSONFormatter,
    get_correlation_id,
    get_feature,
    get_logger,
    logging_context,
    set_correlation_id,
    set_feature,
)


# ============================================================
# 예외 클래스 테스트
# ============================================================


class TestAIServiceError:
    """AIServiceError 기본 예외 테스트."""

    def test_default_message(self):
        err = AIServiceError()
        assert str(err) == "AI 서비스 에러가 발생했습니다"
        assert err.message == "AI 서비스 에러가 발생했습니다"

    def test_custom_message(self):
        err = AIServiceError("커스텀 에러 메시지")
        assert str(err) == "커스텀 에러 메시지"

    def test_is_exception(self):
        err = AIServiceError()
        assert isinstance(err, Exception)


class TestBedrockInvocationError:
    """BedrockInvocationError 테스트."""

    def test_attributes(self):
        err = BedrockInvocationError(
            model_id="anthropic.claude-3-sonnet", detail="Timeout after 30s"
        )
        assert err.model_id == "anthropic.claude-3-sonnet"
        assert err.detail == "Timeout after 30s"

    def test_str_contains_model_and_detail(self):
        err = BedrockInvocationError(
            model_id="anthropic.claude-3-sonnet", detail="Timeout after 30s"
        )
        result = str(err)
        assert "anthropic.claude-3-sonnet" in result
        assert "Timeout after 30s" in result

    def test_inherits_ai_service_error(self):
        err = BedrockInvocationError(model_id="test", detail="test")
        assert isinstance(err, AIServiceError)


class TestMessageParseError:
    """MessageParseError 테스트."""

    def test_attributes(self):
        err = MessageParseError(raw_snippet='{"invalid": json}')
        assert err.raw_snippet == '{"invalid": json}'

    def test_truncates_long_snippet(self):
        long_message = "x" * 500
        err = MessageParseError(raw_snippet=long_message)
        assert len(err.raw_snippet) == 200

    def test_str_contains_snippet(self):
        err = MessageParseError(raw_snippet="bad data")
        result = str(err)
        assert "bad data" in result

    def test_inherits_ai_service_error(self):
        err = MessageParseError(raw_snippet="test")
        assert isinstance(err, AIServiceError)


class TestSessionNotFoundError:
    """SessionNotFoundError 테스트."""

    def test_attributes(self):
        err = SessionNotFoundError(session_id="sess-123")
        assert err.session_id == "sess-123"

    def test_str_contains_session_id(self):
        err = SessionNotFoundError(session_id="sess-abc-456")
        result = str(err)
        assert "sess-abc-456" in result

    def test_inherits_ai_service_error(self):
        err = SessionNotFoundError(session_id="test")
        assert isinstance(err, AIServiceError)


class TestSessionExpiredError:
    """SessionExpiredError 테스트."""

    def test_attributes(self):
        err = SessionExpiredError(session_id="sess-expired-789")
        assert err.session_id == "sess-expired-789"

    def test_str_contains_session_id(self):
        err = SessionExpiredError(session_id="sess-expired-789")
        result = str(err)
        assert "sess-expired-789" in result

    def test_inherits_ai_service_error(self):
        err = SessionExpiredError(session_id="test")
        assert isinstance(err, AIServiceError)


class TestPublishError:
    """PublishError 테스트."""

    def test_attributes(self):
        err = PublishError(queue_name="quiz-response-queue")
        assert err.queue_name == "quiz-response-queue"

    def test_str_contains_queue_name(self):
        err = PublishError(queue_name="quiz-response-queue")
        result = str(err)
        assert "quiz-response-queue" in result

    def test_inherits_ai_service_error(self):
        err = PublishError(queue_name="test")
        assert isinstance(err, AIServiceError)


# ============================================================
# 구조화된 로깅 테스트
# ============================================================


class TestJSONFormatter:
    """JSONFormatter 테스트."""

    def setup_method(self):
        """각 테스트 전에 컨텍스트 초기화."""
        set_correlation_id(None)
        set_feature(None)

    def _make_log_record(self, message: str, level: int = logging.INFO, **kwargs):
        """테스트용 LogRecord 생성."""
        record = logging.LogRecord(
            name="test",
            level=level,
            pathname="test.py",
            lineno=1,
            msg=message,
            args=(),
            exc_info=None,
        )
        for key, value in kwargs.items():
            setattr(record, key, value)
        return record

    def test_outputs_valid_json(self):
        formatter = JSONFormatter()
        record = self._make_log_record("테스트 메시지")
        output = formatter.format(record)
        parsed = json.loads(output)
        assert isinstance(parsed, dict)

    def test_includes_timestamp(self):
        formatter = JSONFormatter()
        record = self._make_log_record("테스트")
        output = json.loads(formatter.format(record))
        assert "timestamp" in output
        # ISO 8601 형식 확인
        assert "T" in output["timestamp"]

    def test_includes_level(self):
        formatter = JSONFormatter()
        record = self._make_log_record("테스트", level=logging.ERROR)
        output = json.loads(formatter.format(record))
        assert output["level"] == "ERROR"

    def test_includes_message(self):
        formatter = JSONFormatter()
        record = self._make_log_record("Bedrock API 호출 실패")
        output = json.loads(formatter.format(record))
        assert output["message"] == "Bedrock API 호출 실패"

    def test_includes_correlation_id_from_context(self):
        formatter = JSONFormatter()
        set_correlation_id("match-uuid-123")
        record = self._make_log_record("테스트")
        output = json.loads(formatter.format(record))
        assert output["correlation_id"] == "match-uuid-123"

    def test_excludes_correlation_id_when_not_set(self):
        formatter = JSONFormatter()
        record = self._make_log_record("테스트")
        output = json.loads(formatter.format(record))
        assert "correlation_id" not in output

    def test_includes_feature_from_context(self):
        formatter = JSONFormatter()
        set_feature("quiz")
        record = self._make_log_record("테스트")
        output = json.loads(formatter.format(record))
        assert output["feature"] == "quiz"

    def test_excludes_feature_when_not_set(self):
        formatter = JSONFormatter()
        record = self._make_log_record("테스트")
        output = json.loads(formatter.format(record))
        assert "feature" not in output

    def test_includes_error_type_from_exception(self):
        formatter = JSONFormatter()
        try:
            raise BedrockInvocationError(model_id="claude", detail="timeout")
        except BedrockInvocationError:
            import sys

            record = self._make_log_record("에러 발생")
            record.exc_info = sys.exc_info()

        output = json.loads(formatter.format(record))
        assert output["error_type"] == "BedrockInvocationError"
        assert "timeout" in output["error_detail"]

    def test_includes_attempt_from_extra(self):
        formatter = JSONFormatter()
        record = self._make_log_record("재시도 중", attempt=2)
        output = json.loads(formatter.format(record))
        assert output["attempt"] == 2

    def test_includes_error_type_from_extra(self):
        formatter = JSONFormatter()
        record = self._make_log_record(
            "에러", error_type="BedrockInvocationError", error_detail="Timeout"
        )
        output = json.loads(formatter.format(record))
        assert output["error_type"] == "BedrockInvocationError"
        assert output["error_detail"] == "Timeout"

    def test_full_log_format_matches_design(self):
        """설계 문서의 로그 포맷 예시와 일치하는지 확인."""
        formatter = JSONFormatter()
        set_correlation_id("match-uuid-or-session-uuid")
        set_feature("quiz")
        record = self._make_log_record(
            "Bedrock API 호출 실패",
            level=logging.ERROR,
            error_type="BedrockInvocationError",
            error_detail="Timeout after 30s",
            attempt=2,
        )
        output = json.loads(formatter.format(record))

        assert output["level"] == "ERROR"
        assert output["message"] == "Bedrock API 호출 실패"
        assert output["correlation_id"] == "match-uuid-or-session-uuid"
        assert output["feature"] == "quiz"
        assert output["error_type"] == "BedrockInvocationError"
        assert output["error_detail"] == "Timeout after 30s"
        assert output["attempt"] == 2


class TestLoggingContext:
    """logging_context 컨텍스트 매니저 테스트."""

    def setup_method(self):
        """각 테스트 전에 컨텍스트 초기화."""
        set_correlation_id(None)
        set_feature(None)

    def test_sets_correlation_id(self):
        with logging_context(correlation_id="test-123"):
            assert get_correlation_id() == "test-123"

    def test_sets_feature(self):
        with logging_context(feature="retrospective"):
            assert get_feature() == "retrospective"

    def test_restores_previous_values(self):
        set_correlation_id("original-id")
        set_feature("original-feature")

        with logging_context(correlation_id="new-id", feature="new-feature"):
            assert get_correlation_id() == "new-id"
            assert get_feature() == "new-feature"

        assert get_correlation_id() == "original-id"
        assert get_feature() == "original-feature"

    def test_restores_on_exception(self):
        set_correlation_id("before")

        with pytest.raises(ValueError):
            with logging_context(correlation_id="during"):
                raise ValueError("test error")

        assert get_correlation_id() == "before"

    def test_nested_contexts(self):
        with logging_context(correlation_id="outer", feature="quiz"):
            assert get_correlation_id() == "outer"
            with logging_context(correlation_id="inner", feature="diary"):
                assert get_correlation_id() == "inner"
                assert get_feature() == "diary"
            assert get_correlation_id() == "outer"
            assert get_feature() == "quiz"


class TestGetLogger:
    """get_logger 함수 테스트."""

    def setup_method(self):
        """각 테스트 전에 컨텍스트 초기화."""
        set_correlation_id(None)
        set_feature(None)

    def test_returns_logger(self):
        logger = get_logger("test.module")
        assert isinstance(logger, logging.Logger)
        assert logger.name == "test.module"

    def test_logger_has_json_formatter(self):
        logger = get_logger("test.json_format")
        assert len(logger.handlers) > 0
        handler = logger.handlers[0]
        assert isinstance(handler.formatter, JSONFormatter)

    def test_no_duplicate_handlers(self):
        """get_logger를 여러 번 호출해도 핸들러가 중복 추가되지 않는다."""
        logger = get_logger("test.no_dup")
        handler_count = len(logger.handlers)
        get_logger("test.no_dup")
        assert len(logger.handlers) == handler_count

    def test_logger_outputs_json(self, capsys):
        """로거가 실제로 JSON을 출력하는지 확인."""
        logger = get_logger("test.output_json")
        set_correlation_id("corr-456")
        set_feature("diary")

        logger.info("테스트 로그 메시지")

        captured = capsys.readouterr()
        parsed = json.loads(captured.err)
        assert parsed["message"] == "테스트 로그 메시지"
        assert parsed["correlation_id"] == "corr-456"
        assert parsed["feature"] == "diary"
        assert parsed["level"] == "INFO"

    def test_logger_with_extra_fields(self, capsys):
        """extra 필드를 통해 attempt 등을 전달할 수 있다."""
        logger = get_logger("test.extra_fields")

        logger.warning(
            "재시도 중",
            extra={"attempt": 3, "error_type": "TimeoutError", "error_detail": "30s"},
        )

        captured = capsys.readouterr()
        parsed = json.loads(captured.err)
        assert parsed["attempt"] == 3
        assert parsed["error_type"] == "TimeoutError"
        assert parsed["error_detail"] == "30s"
