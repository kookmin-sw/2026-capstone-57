"""일기(Diary) E2E 테스트 (HTTP 방식).

전체 흐름: POST /start → 질문 수신 → POST /answer → 질문 수신 → POST /complete → 최종 결과

실행:
    pytest test_diary_e2e.py -v
"""

from __future__ import annotations

import asyncio
from unittest.mock import AsyncMock, patch

import pytest
import httpx
from httpx import ASGITransport

from app.main import app


SESSION_ID = "diary-e2e-test-001"
USER_ID = "user-test-001"


@pytest.fixture
def mock_bedrock():
    """Bedrock 클라이언트를 모킹한다."""
    with patch("app.bedrock.client.BedrockClient") as mock_cls:
        instance = mock_cls.return_value
        instance.invoke_with_messages = AsyncMock(
            return_value="오늘 하루 어떤 일이 있었나요?"
        )
        instance.invoke = AsyncMock(
            return_value="오늘은 소프트웨어공학 수업에서 발표를 했고, 점심은 학식을 먹었다."
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
async def test_diary_full_flow(mock_bedrock, mock_rag):
    """일기 전체 흐름 E2E 테스트: start → answer → complete."""
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
            "plannerEntries": [
                {
                    "dayOfWeek": 4,
                    "startTime": "09:00",
                    "endTime": "10:30",
                    "location": "미래관 301호",
                    "name": "소프트웨어공학",
                    "type": "CLASS",
                },
                {
                    "dayOfWeek": 4,
                    "startTime": "12:00",
                    "endTime": "13:00",
                    "location": "학생식당",
                    "name": "점심",
                    "type": "FREE",
                },
                {
                    "dayOfWeek": 4,
                    "startTime": "14:00",
                    "endTime": "16:00",
                    "location": "도서관",
                    "name": "캡스톤 팀플",
                    "type": "ACTIVITY",
                },
            ],
        }

        resp = await client.post("/api/diary/start", json=start_payload)
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
            "userMessage": "오늘 소프트웨어공학 수업에서 팀 프로젝트 발표가 있었는데 잘 끝나서 기분이 좋았어요.",
        }

        resp = await client.post("/api/diary/answer", json=answer_payload)
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

        resp = await client.post("/api/diary/complete", json=complete_payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["action"] == "COMPLETED"
        assert data["sessionId"] == SESSION_ID
        assert data["status"] == "COMPLETED"
        assert data["compiledContent"] is not None
        assert len(data["compiledContent"]) > 0


@pytest.mark.asyncio
async def test_diary_answer_without_session(mock_bedrock, mock_rag):
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

        resp = await client.post("/api/diary/answer", json=answer_payload)
        assert resp.status_code == 404
