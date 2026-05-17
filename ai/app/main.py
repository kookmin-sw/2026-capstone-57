"""FastAPI 앱 진입점.

AI 서비스의 메인 애플리케이션을 정의한다.
lifespan 이벤트 핸들러를 통해 컴포넌트 초기화/정리를 관리한다.

시작 시:
- Settings 로드
- BedrockClient, SQSPublisher, ConversationManager, RAG 컴포넌트 초기화
- 공유 의존성을 app.state에 등록 (HTTP 라우터에서 사용)
- 기능별 핸들러 생성 및 MessageRouter 등록
- SQS Poller 시작 (퀴즈 큐만 폴링 — 일기/회고는 HTTP로 전환)
- 세션 만료 정리 주기적 태스크 등록

종료 시:
- SQS Poller 중지
- 세션 정리 태스크 취소

Requirements: 1.6, 3.1, 3.4, 3.8
"""

from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware

from app.bedrock.client import BedrockClient
from app.config import get_settings
from app.conversation.manager import ConversationManager
from app.features.diary.router import router as diary_router
from app.features.mission.handler import MissionHandler
from app.features.mission.router import router as mission_router_http
from app.features.mission.search import MissionSearch
from app.features.quiz.handler import QuizHandler
from app.features.retrospective.router import router as retro_router
from app.health import router as health_router
from app.rag.embeddings import EmbeddingGenerator
from app.rag.pipeline import RAGPipeline
from app.rag.vector_store import VectorStore
from app.sqs.poller import SQSPoller
from app.sqs.publisher import SQSPublisher
from app.sqs.router import MessageRouter

logger = logging.getLogger(__name__)


async def _session_cleanup_loop(
    conversation_manager: ConversationManager,
    interval_seconds: float = 60.0,
) -> None:
    """만료된 세션을 주기적으로 정리하는 백그라운드 태스크.

    Args:
        conversation_manager: 세션 관리자 인스턴스.
        interval_seconds: 정리 주기 (초). 기본 60초.
    """
    while True:
        try:
            await asyncio.sleep(interval_seconds)
            cleaned = await conversation_manager.cleanup_expired()
            if cleaned > 0:
                logger.info(f"세션 정리 완료: {cleaned}개 만료 세션 제거")
        except asyncio.CancelledError:
            logger.info("세션 정리 태스크 취소됨")
            break
        except Exception:
            logger.exception("세션 정리 중 예외 발생")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """앱 라이프사이클 관리.

    시작 시 모든 컴포넌트를 초기화하고 SQS 폴링을 시작한다.
    종료 시 폴링을 중지하고 리소스를 정리한다.
    """
    # === Startup ===
    settings = get_settings()
    logger.info("AI 서비스 시작: 컴포넌트 초기화 중...")

    # 핵심 인프라 초기화
    bedrock_client = BedrockClient(settings)

    publisher = SQSPublisher(
        region=settings.aws_region,
        max_retries=settings.sqs_publish_max_retries,
    )

    conversation_manager = ConversationManager(
        max_turns=settings.conversation_max_turns,
        session_timeout_minutes=settings.conversation_session_timeout_minutes,
    )

    # RAG 컴포넌트 초기화
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    rag_pipeline = RAGPipeline(embedding_generator, vector_store, settings)

    # 공유 의존성을 app.state에 등록 (HTTP 라우터에서 사용)
    app.state.settings = settings
    app.state.bedrock_client = bedrock_client
    app.state.conversation_manager = conversation_manager
    app.state.rag_pipeline = rag_pipeline
    app.state.embedding_generator = embedding_generator
    app.state.vector_store = vector_store

    # 기능별 핸들러 생성
    quiz_handler = QuizHandler(bedrock_client, publisher, settings)

    # 미션 검색 및 핸들러 초기화 (기존 embedding_generator, vector_store 재사용)
    mission_search = MissionSearch(embedding_generator, vector_store, settings)
    mission_handler = MissionHandler(
        bedrock_client, publisher, mission_search, settings
    )

    # 퀴즈 MessageRouter 생성
    quiz_router = MessageRouter()
    quiz_router.register_handler("GENERATE_QUIZ", quiz_handler.handle)

    # 미션 MessageRouter 생성
    mission_router = MessageRouter()
    mission_router.register_handler("GENERATE_MISSION", mission_handler.handle)

    # SQS Poller 생성 및 큐 등록
    poller = SQSPoller(
        region=settings.aws_region,
        poll_interval=settings.sqs_poll_interval_seconds,
        max_concurrent=settings.sqs_max_concurrent_messages,
    )

    poller.register_queue(
        settings.sqs_quiz_request_queue,
        lambda msg: quiz_router.route("quiz-requests", msg),
    )

    poller.register_queue(
        settings.sqs_mission_request_queue,
        lambda msg: mission_router.route("mission-requests", msg),
    )

    # SQS Poller 시작
    await poller.start()

    # 세션 만료 정리 주기적 태스크 시작
    cleanup_task = asyncio.create_task(
        _session_cleanup_loop(conversation_manager),
        name="session-cleanup",
    )

    logger.info("AI 서비스 시작 완료: 모든 컴포넌트 초기화됨")

    yield

    # === Shutdown ===
    logger.info("AI 서비스 종료: 리소스 정리 중...")

    # 세션 정리 태스크 취소
    cleanup_task.cancel()
    try:
        await cleanup_task
    except asyncio.CancelledError:
        pass

    # SQS Poller 중지
    await poller.stop()

    logger.info("AI 서비스 종료 완료")


app = FastAPI(
    title="AI Service",
    version="0.1.0",
    description="Bedrock 기반 퀴즈/회고/일기 생성 서비스",
    lifespan=lifespan,
)


class RequestBodyLogMiddleware(BaseHTTPMiddleware):
    """수신된 HTTP 요청의 바디를 로깅하는 미들웨어."""

    async def dispatch(self, request: Request, call_next):
        body = await request.body()
        logger.info(
            "HTTP 요청 수신: %s %s, body=%s",
            request.method,
            request.url.path,
            body.decode("utf-8", errors="replace")[:2000],
        )
        response = await call_next(request)
        return response


app.add_middleware(RequestBodyLogMiddleware)

app.include_router(health_router)
app.include_router(diary_router)
app.include_router(retro_router)
app.include_router(mission_router_http)
