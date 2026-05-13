"""회고(Retrospective) E2E 테스트 (HTTP 방식).

전체 흐름: POST /start → 질문 수신 → POST /answer → 질문 수신 → POST /complete → 최종 결과

실행:
    pytest test_retro_e2e.py -v
"""

from __future__ import annotations

import asyncio
from unittest.mock import AsyncMock, patch

import pytest
import httpx
from httpx import ASGITransport

from app.main import app


SESSION_ID = "retro-e2e-test-001"
USER_ID = "user-test-001"


@pytest.fixture
def mock_bedrock():
    """Bedrock 클라이언트를 모킹한다."""
    with patch("app.bedrock.client.BedrockClient") as mock_cls:
        instance = mock_cls.return_value
        instance.invoke_with_messages = AsyncMock(
            return_value="만남은 어떤 분위기였나요?"
        )
        instance.invoke = AsyncMock(
            return_value="카페에서 커피를 마시며 즐거운 대화를 나눴다."
        )
        yield instance


@pytest.fixture
def mock_rag():
    """RAG 파이프라인을 모킹한다."""
    with patch("app.rag.pipeline.RAGPipeline") as mock_cls:
        instance = mock_cls.return_value
        instance.search = AsyncMock(return_value=[])
        instance.store = AsyncMock(return_value=None)
        yield instance


@pytest.mark.asyncio
async def test_retro_full_flow(mock_bedrock, mock_rag):
    """회고 전체 흐름 E2E 테스트: start → answer → complete."""
    # app.state에 모킹된 의존성 설정
    app.state.bedrock_client = mock_bedrock
    app.state.rag_pipeline = mock_rag

    from app.config import get_settings
    from app.conversation.manager import ConversationManager

    settings = get_settings()
    app.state.settings = settings
    app.state.conversation_manager = ConversationManager(
        max_turns=settings.conversation_max_turns,
        session_timeout_minutes=settings.conversation_session_timeout_minutes,
    )

    transport = ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        # === 1. START ===
        start_payload = {
            "sessionId": SESSION_ID,
            "userId": USER_ID,
            "meetingContext": {
                "matchedUserId": "user-matched-001",
                "matchedUserName": "김민수",
                "meetingDate": "2024-05-01",
                "meetingLocation": "강남역 카페",
            },
        }

        resp = await client.post("/api/retro/start", json=start_payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "QUESTION"
        assert data["sessionId"] == SESSION_ID
        assert data["userId"] == USER_ID
        assert data["status"] == "IN_PROGRESS"
        assert data["question"] is not None
        assert len(data["question"]) > 0

        # === 2. ANSWER ===
        answer_payload = {
            "sessionId": SESSION_ID,
            "userId": USER_ID,
            "userMessage": "카페에서 커피 마시면서 이야기했어요. 서로 취미가 비슷해서 대화가 잘 통했습니다.",
        }

        resp = await client.post("/api/retro/answer", json=answer_payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "QUESTION"
        assert data["sessionId"] == SESSION_ID
        assert data["status"] == "IN_PROGRESS"
        assert data["question"] is not None

        # === 3. COMPLETE ===
        complete_payload = {
            "sessionId": SESSION_ID,
            "userId": USER_ID,
        }

        resp = await client.post("/api/retro/complete", json=complete_payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "COMPLETED"
        assert data["sessionId"] == SESSION_ID
        assert data["status"] == "COMPLETED"
        assert data["compiledContent"] is not None
        assert len(data["compiledContent"]) > 0


@pytest.mark.asyncio
async def test_retro_answer_without_session(mock_bedrock, mock_rag):
    """존재하지 않는 세션에 답변 시 404 반환."""
    app.state.bedrock_client = mock_bedrock
    app.state.rag_pipeline = mock_rag

    from app.config import get_settings
    from app.conversation.manager import ConversationManager

    settings = get_settings()
    app.state.settings = settings
    app.state.conversation_manager = ConversationManager(
        max_turns=settings.conversation_max_turns,
        session_timeout_minutes=settings.conversation_session_timeout_minutes,
    )

    transport = ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        answer_payload = {
            "sessionId": "nonexistent-session",
            "userId": USER_ID,
            "userMessage": "테스트 답변",
        }

        resp = await client.post("/api/retro/answer", json=answer_payload)
        assert resp.status_code == 404
