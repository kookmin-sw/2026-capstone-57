# SQS 연결 확인 스크립트 (IAM Role 기반)
import boto3
from app.config import get_settings

settings = get_settings()

sqs = boto3.client("sqs", region_name=settings.aws_region)

queue_url = settings.sqs_quiz_request_queue

try:
    if queue_url.startswith("https://"):
        response = sqs.get_queue_attributes(
            QueueUrl=queue_url,
            AttributeNames=["QueueArn"],
        )
        arn = response["Attributes"]["QueueArn"]
        print(f"✅ 큐 연결 성공: {queue_url}")
        print(f"   ARN: {arn}")
    else:
        response = sqs.get_queue_url(QueueName=queue_url)
        print(f"✅ 큐 연결 성공: {response['QueueUrl']}")
except Exception as e:
    print(f"❌ 연결 실패: {e}")
