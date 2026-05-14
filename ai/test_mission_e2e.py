"""미션 생성 E2E 테스트.

전체 흐름: SQS 전송 → 핸들러 처리 (ChromaDB 검색 → Bedrock 호출) → 응답 확인

사전 조건:
    - scripts/seed_venues.py 실행하여 장소 데이터 시딩 완료
    - AWS 자격증명 설정 완료
    - .env 파일에 SQS 큐 URL 설정 완료

EC2에서 실행:
    python3 test_mission_e2e.py

Requirements: 1.1, 1.2, 4.1
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


# 테스트 메시지
MISSION_REQUEST = {
    "action": "GENERATE_MISSION",
    "matchId": "e2e-mission-test-001",
    "requesterId": "user-requester-001",
    "targetUserId": "user-target-001",
    "intersectionInfo": {
        "timeSlots": ["14:00-15:00", "16:00-17:00"],
        "buildingIds": ["building-welfare", "building-building-mirae"],
        "dayOfWeek": 3,
    },
    "userProfiles": [
        {
            "userId": "user-requester-001",
            "interests": ["커피", "독서", "산책"],
            "personalityType": ["INFP"],
        },
        {
            "userId": "user-target-001",
            "interests": ["게임", "운동", "음악"],
            "personalityType": ["ENTP"],
        },
    ],
    "requestedAt": "2024-05-08T12:00:00Z",
}


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
    """2단계: Mission Request Queue에서 메시지 수신 (폴링 시뮬레이션)."""
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
    print(f"   TimeSlots: {body['intersectionInfo']['timeSlots']}")
    print(f"   Buildings: {body['intersectionInfo']['buildingIds']}")

    # 메시지 삭제 (처리 완료)
    sqs.delete_message(
        QueueUrl=settings.sqs_mission_request_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("   🗑️ 큐에서 삭제 완료")

    return body


async def step_3_process_mission(message: dict):
    """3단계: 핸들러로 미션 생성 (ChromaDB 검색 → Bedrock 호출)."""
    print("\n" + "=" * 60)
    print("🧠 [3단계] 미션 생성 처리 (ChromaDB 검색 + Bedrock 호출)")
    print("=" * 60)

    print(f"   [DEBUG] Response Queue URL: '{settings.sqs_mission_response_queue}'")

    # 인프라 초기화
    bedrock_client = BedrockClient(settings)
    publisher = SQSPublisher(region=settings.aws_region)
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    mission_search = MissionSearch(embedding_generator, vector_store, settings)

    handler = MissionHandler(bedrock_client, publisher, mission_search, settings)

    print("   ChromaDB 장소 검색 중...")
    print("   프롬프트 증강 중...")
    print("   Bedrock 호출 중... (최대 60초 소요)")

    start = time.time()
    await handler.handle(message)
    elapsed = time.time() - start

    print(f"   ✅ 처리 완료 ({elapsed:.1f}초 소요)")

    # 발행 확인은 step_4에서 수행 (여기서 수신하면 step_4에서 못 받음)
    print("   응답 큐 발행 완료 - step 4에서 확인 예정")


def step_4_check_response():
    """4단계: Mission Response Queue에서 결과 확인."""
    print("\n" + "=" * 60)
    print("📬 [4단계] Mission Response Queue에서 결과 확인")
    print("=" * 60)

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

    print(f"   ✅ 응답 수신!")
    print(f"   Status: {body.get('status')}")
    print(f"   MatchId: {body.get('matchId')}")
    print(f"   CompletedAt: {body.get('completedAt')}")

    mission = body.get("mission")
    if mission:
        print()
        print(f"   🎯 생성된 미션:")
        print(f"      장소: {mission.get('placeName')}")
        print(f"      활동: {mission.get('activity')}")
        print(f"      추천 시간: {mission.get('recommendedTime')}")
        print(f"      설명: {mission.get('description')}")
    else:
        print(f"   (미션 데이터 없음 - status: {body.get('status')})")
        if body.get("status") == "FAILED":
            print("   에러: 응답에 mission 필드 없음")

    # 응답 메시지 삭제
    sqs.delete_message(
        QueueUrl=settings.sqs_mission_response_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("   🗑️ 응답 큐에서 삭제 완료")


async def main():
    print("🚀 미션 생성 E2E 테스트 시작")
    print(f"   Region: {settings.aws_region}")
    print(f"   Request Queue: {settings.sqs_mission_request_queue}")
    print(f"   Response Queue: {settings.sqs_mission_response_queue}")
    print(f"   Bedrock Model: {settings.bedrock_model_id}")
    print(f"   Embedding Model: {settings.bedrock_embedding_model_id}")
    print(f"   ChromaDB: {settings.chroma_persist_directory}")

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
