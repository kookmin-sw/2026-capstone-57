# SQS 메시지 수신 테스트 (응답 큐에서 결과 확인)
import json
import boto3
from app.config import get_settings

settings = get_settings()
sqs = boto3.client("sqs", region_name=settings.aws_region)


def receive_from_queue(queue_url: str, queue_name: str, delete: bool = False):
    """큐에서 메시지를 수신하여 출력한다."""
    print(f"\n📥 {queue_name} 확인 중...")
    print(f"   URL: {queue_url}")

    try:
        response = sqs.receive_message(
            QueueUrl=queue_url,
            MaxNumberOfMessages=5,
            WaitTimeSeconds=5,  # 5초 대기 (Long Polling)
        )

        messages = response.get("Messages", [])

        if not messages:
            print(f"   (메시지 없음)")
            return

        for i, msg in enumerate(messages, 1):
            body = json.loads(msg["Body"])
            print(f"\n   [{i}] MessageId: {msg['MessageId']}")
            print(f"       내용: {json.dumps(body, ensure_ascii=False, indent=8)}")

            if delete:
                sqs.delete_message(
                    QueueUrl=queue_url,
                    ReceiptHandle=msg["ReceiptHandle"],
                )
                print(f"       🗑️ 삭제 완료")

    except Exception as e:
        print(f"   ❌ 수신 실패: {e}")


# 요청 큐 확인 (수신 후 삭제)
receive_from_queue(
    settings.sqs_mission_request_queue,
    "Mission Request Queue",
    delete=True,
)

# 응답 큐 확인 (수신 후 삭제)
receive_from_queue(
    settings.sqs_mission_response_queue,
    "Mission Response Queue",
    delete=True,
)
