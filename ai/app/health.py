"""헬스체크 엔드포인트.

SQS 연결성과 Bedrock 가용성을 확인하여 서비스 상태를 보고한다.
"""

import boto3
from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.config import Settings, get_settings

router = APIRouter()


class CheckResult(BaseModel):
    """개별 서비스 체크 결과."""

    status: str
    message: str


class HealthResponse(BaseModel):
    """헬스체크 응답 모델."""

    status: str
    checks: dict[str, CheckResult]


def _check_sqs(settings: Settings) -> CheckResult:
    """SQS 연결성을 확인한다."""
    try:
        client = boto3.client(
            "sqs",
            region_name=settings.aws_region,
        )
        client.list_queues(MaxResults=1)
        return CheckResult(status="healthy", message="Connected")
    except Exception as e:
        return CheckResult(status="unhealthy", message=str(e))


def _check_bedrock(settings: Settings) -> CheckResult:
    """Bedrock 가용성을 확인한다."""
    try:
        client = boto3.client(
            "bedrock",
            region_name=settings.aws_region,
        )
        client.list_foundation_models(maxResults=1)
        return CheckResult(status="healthy", message="Available")
    except Exception as e:
        return CheckResult(status="unhealthy", message=str(e))


@router.get("/health", response_model=HealthResponse)
def health_check(settings: Settings = Depends(get_settings)) -> HealthResponse:
    """서비스 헬스체크 엔드포인트.

    SQS 연결성과 Bedrock 가용성을 확인하여 전체 상태를 반환한다.
    개별 서비스가 하나라도 unhealthy이면 전체 상태도 unhealthy로 보고한다.
    """
    sqs_result = _check_sqs(settings)
    bedrock_result = _check_bedrock(settings)

    checks = {
        "sqs": sqs_result,
        "bedrock": bedrock_result,
    }

    overall_status = "healthy"
    if any(check.status == "unhealthy" for check in checks.values()):
        overall_status = "unhealthy"

    return HealthResponse(status=overall_status, checks=checks)
