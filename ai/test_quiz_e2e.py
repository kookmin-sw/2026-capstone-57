"""퀴즈 생성 E2E 테스트.

전체 흐름: SQS 전송 → 폴링 → 파싱 → Bedrock 호출 → 퀴즈 생성 → 응답 큐 발행

EC2에서 실행:
    python3 test_quiz_e2e.py
"""

import asyncio
import json
import time

import boto3

from app.config import get_settings
from app.bedrock.client import BedrockClient
from app.features.quiz.handler import QuizHandler
from app.sqs.publisher import SQSPublisher


settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)


# 테스트 메시지
QUIZ_REQUEST = {
    "action": "GENERATE_QUIZ",
    "matchId": "e2e-test-001",
    "requesterId": "user-requester-001",
    "targetUserId": "user-target-001",
    "requestedAt": "2024-05-08T12:00:00Z",
    "targetProfile": {
        "name": "황찬우",
        "nickname": "바보",
        "university": "국민대학교",
        "major": "소프트웨어",
        "hobbies": ["게임", "야구", "축구"],
        "interests": ["AI", "게임"],
        "personalityType": ["INTP"],
    },
}


def step_1_send_message():
    """1단계: Request Queue에 메시지 전송."""
    print("\n" + "=" * 60)
    print("📤 [1단계] Quiz Request Queue에 메시지 전송")
    print("=" * 60)

    response = sqs.send_message(
        QueueUrl=settings.sqs_quiz_request_queue,
        MessageBody=json.dumps(QUIZ_REQUEST, ensure_ascii=False),
    )
    print(f"   ✅ 전송 성공 - MessageId: {response['MessageId']}")
    return response["MessageId"]


def step_2_receive_message():
    """2단계: Request Queue에서 메시지 수신 (폴링 시뮬레이션)."""
    print("\n" + "=" * 60)
    print("📥 [2단계] Quiz Request Queue에서 메시지 수신")
    print("=" * 60)

    response = sqs.receive_message(
        QueueUrl=settings.sqs_quiz_request_queue,
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
    print(f"   Target: {body['targetProfile']['name']}")

    # 메시지 삭제 (처리 완료)
    sqs.delete_message(
        QueueUrl=settings.sqs_quiz_request_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("   🗑️ 큐에서 삭제 완료")

    return body


async def step_3_process_quiz(message: dict):
    """3단계: 핸들러로 퀴즈 생성 (Bedrock 호출 포함)."""
    print("\n" + "=" * 60)
    print("🧠 [3단계] 퀴즈 생성 처리 (Bedrock 호출)")
    print("=" * 60)

    bedrock_client = BedrockClient(settings)
    publisher = SQSPublisher(region=settings.aws_region)
    handler = QuizHandler(bedrock_client, publisher, settings)

    print("   프롬프트 생성 중...")
    print("   Bedrock 호출 중... (최대 30초 소요)")

    start = time.time()
    await handler.handle(message)
    elapsed = time.time() - start

    print(f"   ✅ 처리 완료 ({elapsed:.1f}초 소요)")


def step_4_check_response():
    """4단계: Response Queue에서 결과 확인."""
    print("\n" + "=" * 60)
    print("📬 [4단계] Quiz Response Queue에서 결과 확인")
    print("=" * 60)

    response = sqs.receive_message(
        QueueUrl=settings.sqs_quiz_response_queue,
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

    quiz = body.get("quiz")
    if quiz and quiz.get("questions"):
        questions = quiz["questions"]
        print(f"   문제 수: {len(questions)}개")
        print()
        for i, q in enumerate(questions, 1):
            print(f"   Q{i}. {q['questionText']}")
            for j, choice in enumerate(q["choices"]):
                marker = "✓" if j == q["correctIndex"] else " "
                print(f"       [{marker}] {choice}")
            print(f"       설명: {q['explanation']}")
            print()
    else:
        print(f"   (퀴즈 데이터 없음 - status: {body.get('status')})")
        if body.get("status") == "FAILED":
            print(f"   에러: 응답에 quiz 필드 없음")

    # 응답 메시지 삭제
    sqs.delete_message(
        QueueUrl=settings.sqs_quiz_response_queue,
        ReceiptHandle=msg["ReceiptHandle"],
    )
    print("   🗑️ 응답 큐에서 삭제 완료")


async def main():
    print("🚀 퀴즈 생성 E2E 테스트 시작")
    print(f"   Region: {settings.aws_region}")
    print(f"   Request Queue: {settings.sqs_quiz_request_queue}")
    print(f"   Response Queue: {settings.sqs_quiz_response_queue}")
    print(f"   Bedrock Model: {settings.bedrock_model_id}")

    # 1. 메시지 전송
    step_1_send_message()

    # 2. 메시지 수신
    message = step_2_receive_message()
    if not message:
        print("\n❌ 테스트 실패: 메시지를 수신하지 못했습니다.")
        return

    # 3. 퀴즈 생성 처리
    await step_3_process_quiz(message)

    # 4. 응답 확인
    step_4_check_response()

    print("\n" + "=" * 60)
    print("🎉 E2E 테스트 완료!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
