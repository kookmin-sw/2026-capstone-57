"""구조화된 JSON 로깅 설정.

Python logging 모듈을 JSON 포맷으로 출력하도록 구성한다.
correlation_id, feature, error_type 등 추적에 필요한 필드를 포함한다.
"""

import json
import logging
from contextvars import ContextVar
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Any, Generator

# 요청 스코프에서 correlation_id를 전파하기 위한 ContextVar
_correlation_id_var: ContextVar[str | None] = ContextVar(
    "correlation_id", default=None
)
_feature_var: ContextVar[str | None] = ContextVar("feature", default=None)


def get_correlation_id() -> str | None:
    """현재 컨텍스트의 correlation_id를 반환한다."""
    return _correlation_id_var.get()


def set_correlation_id(correlation_id: str | None) -> None:
    """현재 컨텍스트에 correlation_id를 설정한다."""
    _correlation_id_var.set(correlation_id)


def get_feature() -> str | None:
    """현재 컨텍스트의 feature를 반환한다."""
    return _feature_var.get()


def set_feature(feature: str | None) -> None:
    """현재 컨텍스트에 feature를 설정한다."""
    _feature_var.set(feature)


@contextmanager
def logging_context(
    correlation_id: str | None = None,
    feature: str | None = None,
) -> Generator[None, None, None]:
    """로깅 컨텍스트를 설정하는 컨텍스트 매니저.

    요청 처리 스코프 동안 correlation_id와 feature를 설정하고,
    스코프 종료 시 이전 값으로 복원한다.

    Args:
        correlation_id: 요청 추적을 위한 상관 ID (matchId 또는 sessionId)
        feature: 기능 식별자 (예: "quiz", "retrospective", "diary")

    Example:
        with logging_context(correlation_id="session-123", feature="quiz"):
            logger.info("퀴즈 생성 시작")
    """
    old_correlation_id = _correlation_id_var.get()
    old_feature = _feature_var.get()

    _correlation_id_var.set(correlation_id)
    _feature_var.set(feature)
    try:
        yield
    finally:
        _correlation_id_var.set(old_correlation_id)
        _feature_var.set(old_feature)


class JSONFormatter(logging.Formatter):
    """구조화된 JSON 로그 포맷터.

    로그 레코드를 JSON 형식으로 변환하며, 다음 필드를 포함한다:
    - timestamp: ISO 8601 형식의 타임스탬프
    - level: 로그 레벨 (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    - message: 로그 메시지
    - correlation_id: 요청 추적 ID (컨텍스트에서 가져옴)
    - feature: 기능 식별자 (컨텍스트에서 가져옴)
    - error_type: 예외 클래스 이름 (예외 발생 시)
    - error_detail: 예외 메시지 (예외 발생 시)
    - attempt: 재시도 횟수 (extra에서 가져옴)
    """

    def format(self, record: logging.LogRecord) -> str:
        log_entry: dict[str, Any] = {
            "timestamp": datetime.fromtimestamp(
                record.created, tz=timezone.utc
            ).isoformat(),
            "level": record.levelname,
            "message": record.getMessage(),
        }

        # 컨텍스트에서 correlation_id와 feature 가져오기
        correlation_id = _correlation_id_var.get()
        if correlation_id is not None:
            log_entry["correlation_id"] = correlation_id

        feature = _feature_var.get()
        if feature is not None:
            log_entry["feature"] = feature

        # 예외 정보 포함
        if record.exc_info and record.exc_info[1] is not None:
            exc = record.exc_info[1]
            log_entry["error_type"] = type(exc).__name__
            log_entry["error_detail"] = str(exc)

        # extra 필드에서 추가 정보 가져오기
        if hasattr(record, "error_type") and record.error_type:  # type: ignore[attr-defined]
            log_entry["error_type"] = record.error_type  # type: ignore[attr-defined]

        if hasattr(record, "error_detail") and record.error_detail:  # type: ignore[attr-defined]
            log_entry["error_detail"] = record.error_detail  # type: ignore[attr-defined]

        if hasattr(record, "attempt") and record.attempt is not None:  # type: ignore[attr-defined]
            log_entry["attempt"] = record.attempt  # type: ignore[attr-defined]

        return json.dumps(log_entry, ensure_ascii=False)


def get_logger(name: str) -> logging.Logger:
    """구조화된 JSON 로깅이 설정된 로거를 반환한다.

    Args:
        name: 로거 이름 (보통 모듈의 __name__)

    Returns:
        JSON 포맷터가 적용된 logging.Logger 인스턴스
    """
    logger = logging.getLogger(name)

    # 이미 핸들러가 설정되어 있으면 중복 추가 방지
    if not logger.handlers:
        handler = logging.StreamHandler()
        handler.setFormatter(JSONFormatter())
        logger.addHandler(handler)
        logger.setLevel(logging.DEBUG)
        # 부모 로거로 전파 방지 (중복 출력 방지)
        logger.propagate = False

    return logger
