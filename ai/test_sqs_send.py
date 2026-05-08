# SQS 메시지 전송 테스트 (퀴즈 요청)
import json
import boto3
from app.config import get_settings

settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)

# 퀴즈 생성 요청 메시지
message = {
    "action": "GENERATE_QUIZ",
    "matchId": "test-match-001",
    "requesterId": "user-requester-001",
    "targetUserId": "user-target-001",
    "requestedAt": "2024-05-08T12:00:00Z",
    "targetProfile": {
        "name": "홍길동",
        "nickname": "길동이",
        "university": "국민대학교",
        "major": "컴퓨터공학과",
        "hobbies": ["독서", "게임", "운동"],
        "interests": ["AI", "백엔드 개발"],
        "personalityType": ["INTJ"]
    }
}

queue_url = settings.sqs_quiz_request_queue

try:
    response = sqs.send_message(
        QueueUrl=queue_url,
        MessageBody=json.dumps(message, ensure_ascii=False),
    )
    message_id = response["MessageId"]
    print(f"✅ 메시지 전송 성공!")
    print(f"   Queue: {queue_url}")
    print(f"   MessageId: {message_id}")
    print(f"   내용: {message['action']} - {message['targetProfile']['name']}")
except Exception as e:
    print(f"❌ 메시지 전송 실패: {e}")
