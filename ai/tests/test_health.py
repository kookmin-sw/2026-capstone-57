"""헬스체크 엔드포인트 테스트.

httpx AsyncClient와 moto를 사용하여 /health 엔드포인트를 테스트한다.
"""

import pytest
from httpx import ASGITransport, AsyncClient
from moto import mock_aws
from unittest.mock import patch

from app.config import Settings, get_settings
from app.main import app


def get_test_settings() -> Settings:
    """테스트용 Settings 인스턴스."""
    return Settings(
        aws_region="us-east-1",
    )


@pytest.fixture(autouse=True)
def override_settings():
    """모든 테스트에서 테스트용 설정을 사용하도록 오버라이드."""
    app.dependency_overrides[get_settings] = get_test_settings
    yield
    app.dependency_overrides.clear()


@pytest.fixture
def async_client():
    """httpx AsyncClient 픽스처."""
    transport = ASGITransport(app=app)
    return AsyncClient(transport=transport, base_url="http://test")


@pytest.mark.asyncio
async def test_health_all_healthy(async_client: AsyncClient):
    """SQS와 Bedrock 모두 정상일 때 healthy 응답을 반환한다."""
    from app.health import CheckResult

    with patch("app.health._check_sqs") as mock_sqs, patch(
        "app.health._check_bedrock"
    ) as mock_bedrock:
        mock_sqs.return_value = CheckResult(status="healthy", message="Connected")
        mock_bedrock.return_value = CheckResult(status="healthy", message="Available")

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["checks"]["sqs"]["status"] == "healthy"
    assert data["checks"]["sqs"]["message"] == "Connected"
    assert data["checks"]["bedrock"]["status"] == "healthy"
    assert data["checks"]["bedrock"]["message"] == "Available"


@pytest.mark.asyncio
async def test_health_sqs_unhealthy(async_client: AsyncClient):
    """SQS 연결 실패 시 unhealthy 상태를 반환한다."""
    from app.health import CheckResult

    with patch("app.health._check_sqs") as mock_sqs, patch(
        "app.health._check_bedrock"
    ) as mock_bedrock:
        mock_sqs.return_value = CheckResult(
            status="unhealthy", message="Connection refused"
        )
        mock_bedrock.return_value = CheckResult(
            status="healthy", message="Available"
        )

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "unhealthy"
    assert data["checks"]["sqs"]["status"] == "unhealthy"
    assert data["checks"]["sqs"]["message"] == "Connection refused"


@pytest.mark.asyncio
async def test_health_bedrock_unhealthy(async_client: AsyncClient):
    """Bedrock 연결 실패 시 unhealthy 상태를 반환한다."""
    from app.health import CheckResult

    with patch("app.health._check_sqs") as mock_sqs, patch(
        "app.health._check_bedrock"
    ) as mock_bedrock:
        mock_sqs.return_value = CheckResult(status="healthy", message="Connected")
        mock_bedrock.return_value = CheckResult(
            status="unhealthy", message="Connection timeout"
        )

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "unhealthy"
    assert data["checks"]["bedrock"]["status"] == "unhealthy"
    assert data["checks"]["bedrock"]["message"] == "Connection timeout"


@pytest.mark.asyncio
async def test_health_both_unhealthy(async_client: AsyncClient):
    """SQS와 Bedrock 모두 실패 시 unhealthy 상태를 반환한다."""
    from app.health import CheckResult

    with patch("app.health._check_sqs") as mock_sqs, patch(
        "app.health._check_bedrock"
    ) as mock_bedrock:
        mock_sqs.return_value = CheckResult(
            status="unhealthy", message="SQS error"
        )
        mock_bedrock.return_value = CheckResult(
            status="unhealthy", message="Bedrock error"
        )

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "unhealthy"
    assert data["checks"]["sqs"]["status"] == "unhealthy"
    assert data["checks"]["bedrock"]["status"] == "unhealthy"


@pytest.mark.asyncio
async def test_health_response_structure(async_client: AsyncClient):
    """응답 구조가 올바른 형식을 따르는지 확인한다."""
    from app.health import CheckResult

    with patch("app.health._check_sqs") as mock_sqs, patch(
        "app.health._check_bedrock"
    ) as mock_bedrock:
        mock_sqs.return_value = CheckResult(status="healthy", message="Connected")
        mock_bedrock.return_value = CheckResult(status="healthy", message="Available")

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()

    # 최상위 필드 확인
    assert "status" in data
    assert "checks" in data
    assert data["status"] in ("healthy", "unhealthy")

    # checks 내부 구조 확인
    assert "sqs" in data["checks"]
    assert "bedrock" in data["checks"]

    for check_name in ("sqs", "bedrock"):
        check = data["checks"][check_name]
        assert "status" in check
        assert "message" in check
        assert check["status"] in ("healthy", "unhealthy")


@pytest.mark.asyncio
async def test_health_sqs_real_connectivity(async_client: AsyncClient):
    """moto를 사용하여 실제 SQS 연결성 체크가 동작하는지 확인한다."""
    from app.health import CheckResult

    with mock_aws(), patch("app.health._check_bedrock") as mock_bedrock:
        mock_bedrock.return_value = CheckResult(status="healthy", message="Available")

        async with async_client as client:
            response = await client.get("/health")

    assert response.status_code == 200
    data = response.json()
    # moto가 SQS를 모킹하므로 SQS 체크는 healthy여야 함
    assert data["checks"]["sqs"]["status"] == "healthy"
