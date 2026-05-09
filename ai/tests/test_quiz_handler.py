"""퀴즈 핸들러 단위 테스트.

QuizHandler의 전체 흐름을 검증한다:
- 정상 퀴즈 생성 (SUCCESS)
- Bedrock 실패 후 재시도 성공
- 파싱 실패 후 폴백 (FALLBACK)
- Bedrock 완전 실패 시 폴백
- 타임아웃 시 FAILED 응답
- 예상치 못한 예외 시 FAILED 응답
"""

import json
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.bedrock.client import BedrockClient
from app.common.exceptions import BedrockInvocationError
from app.config import Settings
from app.features.quiz.handler import QuizHandler
from app.features.quiz.models import QuizStatus
from app.sqs.publisher import SQSPublisher


@pytest.fixture
def settings():
    """테스트용 Settings 인스턴스."""
    return Settings(
        aws_region="ap-northeast-2",
        sqs_quiz_response_queue="http://localhost:4566/000000000000/quiz-responses",
        request_timeout_seconds=10,
        bedrock_model_id="test-model",
        bedrock_timeout_seconds=5,
    )


@pytest.fixture
def mock_bedrock():
    """Mock BedrockClient."""
    client = AsyncMock(spec=BedrockClient)
    return client


@pytest.fixture
def mock_publisher():
    """Mock SQSPublisher."""
    publisher = AsyncMock(spec=SQSPublisher)
    publisher.publish_with_retry = AsyncMock(return_value=True)
    return publisher


@pytest.fixture
def handler(mock_bedrock, mock_publisher, settings):
    """QuizHandler 인스턴스."""
    return QuizHandler(
        bedrock_client=mock_bedrock,
        publisher=mock_publisher,
        settings=settings,
    )


@pytest.fixture
def valid_request_message():
    """유효한 퀴즈 요청 메시지 딕셔너리."""
    return {
        "action": "GENERATE_QUIZ",
        "matchId": "match-123",
        "requesterId": "user-001",
        "targetUserId": "user-002",
        "requestedAt": "2024-06-01T12:00:00Z",
        "targetProfile": {
            "name": "홍길동",
            "nickname": "길동이",
            "university": "서울대학교",
            "major": "컴퓨터공학",
            "hobbies": ["독서", "등산"],
            "interests": ["AI", "음악"],
            "personalityType": ["INTJ"],
        },
    }


def _make_valid_bedrock_response():
    """유효한 Bedrock 퀴즈 응답 JSON 문자열."""
    return json.dumps(
        {
            "questions": [
                {
                    "questionText": f"질문 {i+1}",
                    "choices": ["A", "B", "C", "D"],
                    "correctIndex": i % 4,
                    "explanation": f"설명 {i+1}",
                }
                for i in range(5)
            ]
        }
    )


@pytest.mark.asyncio
async def test_handle_success(handler, mock_bedrock, mock_publisher, valid_request_message):
    """정상적인 퀴즈 생성 흐름 - SUCCESS 응답."""
    mock_bedrock.invoke = AsyncMock(return_value=_make_valid_bedrock_response())

    await handler.handle(valid_request_message)

    # Bedrock이 1회 호출됨
    mock_bedrock.invoke.assert_called_once()

    # 응답이 발행됨
    mock_publisher.publish_with_retry.assert_called_once()
    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]

    assert response_msg.status == QuizStatus.SUCCESS
    assert response_msg.matchId == "match-123"
    assert response_msg.requesterId == "user-001"
    assert response_msg.targetUserId == "user-002"
    assert response_msg.quiz is not None
    assert len(response_msg.quiz.questions) == 5


@pytest.mark.asyncio
async def test_handle_first_bedrock_fail_retry_success(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """첫 번째 Bedrock 호출 실패 후 재시도 성공."""
    mock_bedrock.invoke = AsyncMock(
        side_effect=[
            BedrockInvocationError(model_id="test", detail="timeout"),
            _make_valid_bedrock_response(),
        ]
    )

    await handler.handle(valid_request_message)

    # Bedrock이 2회 호출됨 (첫 시도 실패 + 재시도 성공)
    assert mock_bedrock.invoke.call_count == 2

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.SUCCESS


@pytest.mark.asyncio
async def test_handle_parse_fail_retry_success(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """첫 번째 응답 파싱 실패 후 재시도에서 성공."""
    mock_bedrock.invoke = AsyncMock(
        side_effect=[
            "invalid json response",
            _make_valid_bedrock_response(),
        ]
    )

    await handler.handle(valid_request_message)

    assert mock_bedrock.invoke.call_count == 2

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.SUCCESS


@pytest.mark.asyncio
async def test_handle_all_attempts_fail_fallback(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """모든 시도 실패 시 폴백 퀴즈 사용."""
    mock_bedrock.invoke = AsyncMock(
        side_effect=BedrockInvocationError(model_id="test", detail="error")
    )

    await handler.handle(valid_request_message)

    # Bedrock이 2회 호출됨 (첫 시도 + 재시도)
    assert mock_bedrock.invoke.call_count == 2

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.FALLBACK
    assert response_msg.quiz is not None
    assert len(response_msg.quiz.questions) == 5


@pytest.mark.asyncio
async def test_handle_parse_fail_both_attempts_fallback(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """두 번 모두 파싱 실패 시 폴백 퀴즈 사용."""
    mock_bedrock.invoke = AsyncMock(return_value="not valid json at all")

    await handler.handle(valid_request_message)

    assert mock_bedrock.invoke.call_count == 2

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.FALLBACK
    assert response_msg.quiz is not None
    assert len(response_msg.quiz.questions) == 5


@pytest.mark.asyncio
async def test_handle_timeout(mock_bedrock, mock_publisher, valid_request_message):
    """요청 타임아웃 시 FAILED 응답."""
    # 매우 짧은 타임아웃 설정
    settings = Settings(
        aws_region="ap-northeast-2",
        sqs_quiz_response_queue="http://localhost:4566/000000000000/quiz-responses",
        request_timeout_seconds=0,  # 즉시 타임아웃
        bedrock_model_id="test-model",
        bedrock_timeout_seconds=5,
    )
    handler = QuizHandler(
        bedrock_client=mock_bedrock,
        publisher=mock_publisher,
        settings=settings,
    )

    # Bedrock 호출이 오래 걸리도록 설정
    async def slow_invoke(*args, **kwargs):
        await asyncio.sleep(10)
        return _make_valid_bedrock_response()

    mock_bedrock.invoke = slow_invoke

    await handler.handle(valid_request_message)

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.FAILED
    assert response_msg.quiz is None


@pytest.mark.asyncio
async def test_handle_unexpected_exception(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """예상치 못한 예외 발생 시 FAILED 응답."""
    mock_bedrock.invoke = AsyncMock(side_effect=RuntimeError("unexpected"))

    await handler.handle(valid_request_message)

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]
    assert response_msg.status == QuizStatus.FAILED
    assert response_msg.quiz is None


@pytest.mark.asyncio
async def test_handle_publish_failure(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """응답 발행 실패 시에도 핸들러가 크래시하지 않음."""
    mock_bedrock.invoke = AsyncMock(return_value=_make_valid_bedrock_response())
    mock_publisher.publish_with_retry = AsyncMock(return_value=False)

    # 예외 없이 완료되어야 함
    await handler.handle(valid_request_message)

    mock_publisher.publish_with_retry.assert_called_once()


@pytest.mark.asyncio
async def test_handle_response_fields(
    handler, mock_bedrock, mock_publisher, valid_request_message
):
    """응답 메시지의 필드가 올바르게 설정되는지 확인."""
    mock_bedrock.invoke = AsyncMock(return_value=_make_valid_bedrock_response())

    await handler.handle(valid_request_message)

    call_args = mock_publisher.publish_with_retry.call_args
    response_msg = call_args[0][1]

    assert response_msg.action.value == "QUIZ_GENERATED"
    assert response_msg.matchId == "match-123"
    assert response_msg.requesterId == "user-001"
    assert response_msg.targetUserId == "user-002"
    assert response_msg.questionCount == 5
    assert response_msg.completedAt is not None
    assert response_msg.quiz.matchId == "match-123"
    assert response_msg.quiz.targetUserId == "user-002"
