"""미션 생성 E2E 테스트 (서브 노드 기반).

실제 Backend와 통신하는 것처럼 SQS를 통해 데이터를 주고받는 테스트.
사전에 seed_venues.py로 노드 데이터가 시딩되어 있어야 함.

흐름:
1. SQS 미션 요청 큐에 메시지 전송 (Backend 역할)
2. AI 서버가 폴링하여 처리 (또는 직접 핸들러 호출)
3. SQS 미션 응답 큐에서 결과 수신 (Backend 역할)

EC2에서 실행:
    python3 test_mission_e2e.py
"""

from __future__ import annotations

import asyncio
import json
import time

import boto3

from app.bedrock.client import BedrockClient
from app.config import get_settings
from app.features.mission.handler import MissionHandler
from app.features.mission.search import MissionSearch
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import VectorStore
from app.sqs.publisher import SQSPublisher

settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)

# Backend가 보내는 미션 요청 메시지 (실제 형식 그대로)
MISSION_REQUEST = {
    "action": "GENERATE_MISSION",
    "matchId": "e2e-mission-test-001",
    "userAId": "user-a-001",
    "userBId": "user-b-001",
    "timeSlot": "14:00-14:30",
    "userARoute": {
        "fromBuilding": {"id": "bld-001", "name": "북악관"},
        "toBuilding": {"id": "bld-002", "name": "미래관"},
        "subNodeIds": ["uuid-1", "uuid-14", "uuid-109", "uuid-110"],
    },
    "userBRoute": {
        "fromBuilding": {"id": "bld-003", "name": "복지관"},
        "toBuilding": {"id": "bld-002", "name": "미래관"},
        "subNodeIds": ["uuid-15", "uuid-14", "uuid-104", "uuid-109", "uuid-110"],
    },
    "requestedAt": "2026-05-14T12:00:00Z",
}


def step_1_send_request():
    """[Backend 역할] 미션 요청을 SQS에 전송."""
    print("\n" + "=" * 60)
    print("� [1단계] Backend → SQS 미션 요청 전송")
    print("=" * 60)
    print(f"   Queue: {settings.sqs_mission_request_queue}")
    print(f"   MatchId: {MISSION_REQUEST['matchId']}")
    print(f"   TimeSlot: {MISSION_REQUEST['timeSlot']}")
    print(f"   UserA: {MISSION_REQUEST['userARoute']['fromBuilding']['name']} → {MISSION_REQUEST['userARoute']['toBuilding']['name']}")
    print(f"   UserB: {MISSION_REQUEST['userBRoute']['fromBuilding']['name']} → {MISSION_REQUEST['userBRoute']['toBuilding']['name']}")
    print(f"   UserA 노드: {MISSION_REQUEST['userARoute']['subNodeIds']}")
    print(f"   UserB 노드: {MISSION_REQUEST['userBRoute']['subNodeIds']}")

    response = sqs.send_message(
        QueueUrl=settings.sqs_mission_request_queue,
        MessageBody=json.dumps(MISSION_REQUEST, ensure_ascii=False),
    )
    print(f"\n   ✅ 전송 성공 - MessageId: {response['MessageId']}")


def step_2_receive_and_process():
    """[AI 서버 역할] SQS에서 수신 → 핸들러 처리."""
    print("\n" + "=" * 60)
    print("🧠 [2단계] AI 서버: SQS 수신 → 미션 생성")
    print("=" * 60)

    # SQS에서 메시지 수신
    response = sqs.receive_message(
        QueueUrl=settings.sqs_mission_request_queue,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=10,
    )

    messages = response.get("Messages", [])
    if not messages:
        print("   ❌ 메시지 없음 (타임아웃)")
        return False

    msg = messages[0]
    body = json.loads(msg["Body"])
    print(f"   ✅ 메시지 수신 완료")

    # 메시지 삭제
    sqs.delete_message(
        QueueUrl=settings.sqs_mission_request_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )

    # 핸들러로 처리
    print("   겹치는 노드 검색 중...")
    print("   ChromaDB 조회 중...")
    print("   Bedrock 호출 중...")

    bedrock_client = BedrockClient(settings)
    publisher = SQSPublisher(region=settings.aws_region)
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    mission_search = MissionSearch(embedding_generator, vector_store, settings)
    handler = MissionHandler(bedrock_client, publisher, mission_search, settings)

    start = time.time()
    asyncio.run(_run_handler(handler, body))
    elapsed = time.time() - start
    print(f"   ✅ 처리 완료 ({elapsed:.1f}초 소요)")
    return True


async def _run_handler(handler, message):
    await handler.handle(message)


def step_3_receive_response():
    """[Backend 역할] SQS 응답 큐에서 미션 결과 수신."""
    print("\n" + "=" * 60)
    print("� [3단계] Backend ← SQS 미션 응답 수신")
    print("=" * 60)

    time.sleep(2)
    response = sqs.receive_message(
        QueueUrl=settings.sqs_mission_response_queue,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=10,
    )

    messages = response.get("Messages", [])
    if not messages:
        print("   ❌ 응답 메시지 없음")
        return

    msg = messages[0]
    body = json.loads(msg["Body"])

    print(f"\n   [응답 JSON]")
    print(json.dumps(body, ensure_ascii=False, indent=2))

    # 메시지 삭제
    sqs.delete_message(
        QueueUrl=settings.sqs_mission_response_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print(f"\n   🗑️ 응답 큐에서 삭제 완료")


def main():
    print("🚀 미션 생성 E2E 테스트")
    print(f"   Region: {settings.aws_region}")
    print(f"   Request Queue: {settings.sqs_mission_request_queue}")
    print(f"   Response Queue: {settings.sqs_mission_response_queue}")
    print(f"   Bedrock Model: {settings.bedrock_model_id}")

    # 1. Backend가 미션 요청 전송
    step_1_send_request()

    # 2. AI 서버가 수신 → 처리
    if not step_2_receive_and_process():
        print("\n❌ 테스트 실패")
        return

    # 3. Backend가 응답 수신
    step_3_receive_response()

    print("\n" + "=" * 60)
    print("🎉 미션 생성 E2E 테스트 완료!")
    print("=" * 60)


if __name__ == "__main__":
    main()
