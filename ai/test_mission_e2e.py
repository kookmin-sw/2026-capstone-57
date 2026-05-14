"""미션 생성 E2E 테스트 (서브 노드 기반).

전체 흐름:
1. 서브 노드 인덱싱 (HTTP)
2. SQS 전송 → 핸들러 처리 (겹치는 노드 검색 → Bedrock 호출) → 응답 확인

사전 조건:
    - AI 서비스가 실행 중이어야 함 (포트 8000)
    - AWS 자격증명 설정 완료

EC2에서 실행:
    python3 test_mission_e2e.py
"""

from __future__ import annotations

import asyncio
import json
import time

import boto3
import requests

from app.bedrock.client import BedrockClient
from app.config import get_settings
from app.features.mission.handler import MissionHandler
from app.features.mission.search import MissionSearch
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import VectorStore
from app.sqs.publisher import SQSPublisher

settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)

BASE_URL = "http://localhost:8000"

# 테스트용 서브 노드 데이터
TEST_NODES = [
    {
        "nodeId": "node-001",
        "name": "용두리 벤치",
        "typeActivity": "BENCH",
        "description": "캠퍼스 내 자연 속 벤치, 산책하며 대화하기 좋은 곳",
        "operatingHours": "상시 개방",
    },
    {
        "nodeId": "node-002",
        "name": "북악관 카페",
        "typeActivity": "CAFE",
        "description": "북악관 1층 카페, 테이크아웃 가능",
        "operatingHours": "월-금 08:00-21:00",
    },
    {
        "nodeId": "node-003",
        "name": "예대 매점",
        "typeActivity": "CONVENIENCE_STORE",
        "description": "예술대학 근처 매점, 간식과 음료 구매 가능",
        "operatingHours": "월-금 09:00-18:00",
    },
]

# 테스트 미션 요청 메시지
MISSION_REQUEST = {
    "action": "GENERATE_MISSION",
    "matchId": "e2e-mission-test-001",
    "userAId": "user-a-001",
    "userBId": "user-b-001",
    "timeSlot": "14:00-14:30",
    "userARoute": {
        "fromBuilding": {"id": "bld-001", "name": "공학관"},
        "toBuilding": {"id": "bld-002", "name": "북악관"},
        "subNodeIds": ["node-001", "node-002", "node-003"],
    },
    "userBRoute": {
        "fromBuilding": {"id": "bld-003", "name": "도서관"},
        "toBuilding": {"id": "bld-002", "name": "북악관"},
        "subNodeIds": ["node-004", "node-002", "node-003"],
    },
    "requestedAt": "2026-05-14T12:00:00Z",
}


def step_0_index_nodes():
    """0단계: 서브 노드를 ChromaDB에 인덱싱."""
    print("\n" + "=" * 60)
    print("📍 [0단계] 서브 노드 인덱싱 (HTTP)")
    print("=" * 60)

    for node in TEST_NODES:
        response = requests.post(
            f"{BASE_URL}/api/campus-nodes/index",
            json=node,
        )
        if response.status_code == 200:
            print(f"   ✅ {node['name']} 인덱싱 완료")
        else:
            print(f"   ❌ {node['name']} 인덱싱 실패: {response.status_code}")
            print(f"      {response.text}")
            return False

    return True


def step_1_send_message():
    """1단계: Mission Request Queue에 메시지 전송."""
    print("\n" + "=" * 60)
    print("📤 [1단계] Mission Request Queue에 메시지 전송")
    print("=" * 60)

    response = sqs.send_message(
        QueueUrl=settings.sqs_mission_request_queue,
        MessageBody=json.dumps(MISSION_REQUEST, ensure_ascii=False),
    )
    print(f"   ✅ 전송 성공 - MessageId: {response['MessageId']}")
    return response["MessageId"]


def step_2_receive_message():
    """2단계: Mission Request Queue에서 메시지 수신."""
    print("\n" + "=" * 60)
    print("📥 [2단계] Mission Request Queue에서 메시지 수신")
    print("=" * 60)

    response = sqs.receive_message(
        QueueUrl=settings.sqs_mission_request_queue,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=10,
    )

    messages = response.get("Messages", [])
    if not messages:
        print("   ❌ 메시지 없음 (타임아웃)")
        return None

    msg = messages[0]
    body = json.loads(msg["Body"])
    print(f"   ✅ 수신 성공 - MessageId: {msg['MessageId']}")
    print(f"   Action: {body['action']}")
    print(f"   MatchId: {body['matchId']}")
    print(f"   TimeSlot: {body['timeSlot']}")
    print(f"   UserA 노드: {body['userARoute']['subNodeIds']}")
    print(f"   UserB 노드: {body['userBRoute']['subNodeIds']}")

    sqs.delete_message(
        QueueUrl=settings.sqs_mission_request_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("   🗑️ 큐에서 삭제 완료")
    return body


async def step_3_process_mission(message: dict):
    """3단계: 핸들러로 미션 생성."""
    print("\n" + "=" * 60)
    print("🧠 [3단계] 미션 생성 처리 (노드 검색 + Bedrock 호출)")
    print("=" * 60)

    bedrock_client = BedrockClient(settings)
    publisher = SQSPublisher(region=settings.aws_region)
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    mission_search = MissionSearch(embedding_generator, vector_store, settings)
    handler = MissionHandler(bedrock_client, publisher, mission_search, settings)

    print("   겹치는 노드 검색 중...")
    print("   Bedrock 호출 중...")

    start = time.time()
    await handler.handle(message)
    elapsed = time.time() - start
    print(f"   ✅ 처리 완료 ({elapsed:.1f}초 소요)")


def step_4_check_response():
    """4단계: Mission Response Queue에서 결과 확인."""
    print("\n" + "=" * 60)
    print("📬 [4단계] Mission Response Queue에서 결과 확인")
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

    print(f"\n[응답 JSON]")
    print(json.dumps(body, ensure_ascii=False, indent=2))

    sqs.delete_message(
        QueueUrl=settings.sqs_mission_response_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("\n   🗑️ 응답 큐에서 삭제 완료")


async def main():
    print("🚀 미션 생성 E2E 테스트 (서브 노드 기반)")
    print(f"   Region: {settings.aws_region}")
    print(f"   Request Queue: {settings.sqs_mission_request_queue}")
    print(f"   Response Queue: {settings.sqs_mission_response_queue}")

    # 0. 노드 인덱싱
    if not step_0_index_nodes():
        print("\n❌ 노드 인덱싱 실패. 테스트 중단.")
        return

    # 1. 메시지 전송
    step_1_send_message()

    # 2. 메시지 수신
    message = step_2_receive_message()
    if not message:
        print("\n❌ 테스트 실패: 메시지를 수신하지 못했습니다.")
        return

    # 3. 미션 생성 처리
    await step_3_process_mission(message)

    # 4. 응답 확인
    step_4_check_response()

    print("\n" + "=" * 60)
    print("🎉 미션 생성 E2E 테스트 완료!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
