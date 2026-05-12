"""일기(Diary) E2E 테스트.

전체 흐름: START_SESSION → QUESTION 수신 → ANSWER 전송 → QUESTION 수신 → COMPLETE → 최종 결과

EC2에서 실행:
    python3 test_diary_e2e.py
"""

from __future__ import annotations

import asyncio
import json
import time

import boto3

from app.config import get_settings
from app.bedrock.client import BedrockClient
from app.conversation.manager import ConversationManager
from app.features.diary.handler import DiaryHandler
from app.rag.embeddings import EmbeddingGenerator
from app.rag.pipeline import RAGPipeline
from app.rag.vector_store import VectorStore
from app.sqs.publisher import SQSPublisher

settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)

SESSION_ID = "diary-e2e-test-001"
USER_ID = "user-test-001"


def send_message(queue_url: str, message: dict) -> str:
    response = sqs.send_message(
        QueueUrl=queue_url,
        MessageBody=json.dumps(message, ensure_ascii=False),
    )
    return response["MessageId"]


def receive_message(queue_url: str, wait: int = 10) -> dict | None:
    response = sqs.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=wait,
    )
    messages = response.get("Messages", [])
    if not messages:
        return None
    msg = messages[0]
    sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=msg["ReceiptHandle"])
    return json.loads(msg["Body"])


async def main():
    print("🚀 일기 E2E 테스트 시작")
    print(f"   Request Queue: {settings.sqs_diary_request_queue}")
    print(f"   Response Queue: {settings.sqs_diary_response_queue}")

    # 핸들러 초기화
    bedrock_client = BedrockClient(settings)
    publisher = SQSPublisher(region=settings.aws_region)
    conversation_manager = ConversationManager(
        max_turns=settings.conversation_max_turns,
        session_timeout_minutes=settings.conversation_session_timeout_minutes,
    )
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    rag_pipeline = RAGPipeline(embedding_generator, vector_store, settings)
    handler = DiaryHandler(
        bedrock_client, publisher, conversation_manager, rag_pipeline, settings
    )

    # === 1. START_SESSION ===
    print("\n" + "=" * 60)
    print("📤 [1단계] START_SESSION 전송")
    print("=" * 60)

    start_msg = {
        "action": "START_SESSION",
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
        "requestedAt": "2024-05-08T22:00:00Z",
    }

    print(f"   플래너 엔트리: {len(start_msg['plannerEntries'])}개")
    start = time.time()
    await handler.handle(start_msg)
    elapsed = time.time() - start
    print(f"   ✅ 처리 완료 ({elapsed:.1f}초)")

    resp = receive_message(settings.sqs_diary_response_queue)
    if resp:
        print(f"   응답 action: {resp.get('action')}")
        print(f"   질문: {resp.get('question', '(없음)')[:80]}...")
    else:
        print("   ❌ 응답 없음")
        return

    # === 2. ANSWER ===
    print("\n" + "=" * 60)
    print("📤 [2단계] ANSWER 전송")
    print("=" * 60)

    answer_msg = {
        "action": "ANSWER",
        "sessionId": SESSION_ID,
        "userId": USER_ID,
        "userMessage": "오늘 소프트웨어공학 수업에서 팀 프로젝트 발표가 있었는데 잘 끝나서 기분이 좋았어요. 점심은 학식 먹었고, 오후에는 캡스톤 팀플 회의했어요.",
        "requestedAt": "2024-05-08T22:01:00Z",
    }

    print(f"   답변: {answer_msg['userMessage'][:50]}...")
    start = time.time()
    await handler.handle(answer_msg)
    elapsed = time.time() - start
    print(f"   ✅ 처리 완료 ({elapsed:.1f}초)")

    resp = receive_message(settings.sqs_diary_response_queue)
    if resp:
        print(f"   응답 action: {resp.get('action')}")
        if resp.get("action") == "QUESTION":
            print(f"   후속 질문: {resp.get('question', '(없음)')[:80]}...")
        elif resp.get("action") == "COMPLETED":
            print(f"   일기: {resp.get('compiledContent', '(없음)')[:100]}...")
    else:
        print("   ❌ 응답 없음")

    # === 3. COMPLETE ===
    print("\n" + "=" * 60)
    print("📤 [3단계] COMPLETE 전송")
    print("=" * 60)

    complete_msg = {
        "action": "COMPLETE",
        "sessionId": SESSION_ID,
        "userId": USER_ID,
        "requestedAt": "2024-05-08T22:02:00Z",
    }

    start = time.time()
    await handler.handle(complete_msg)
    elapsed = time.time() - start
    print(f"   ✅ 처리 완료 ({elapsed:.1f}초)")

    resp = receive_message(settings.sqs_diary_response_queue)
    if resp:
        print(f"   응답 action: {resp.get('action')}")
        print(f"   상태: {resp.get('status')}")
        content = resp.get("compiledContent", "")
        if content:
            print(f"   일기 (앞 200자):\n   {content[:200]}")
        else:
            print(f"   에러: {resp.get('errorMessage', '(없음)')}")
    else:
        print("   ❌ 응답 없음")

    print("\n" + "=" * 60)
    print("🎉 일기 E2E 테스트 완료!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
