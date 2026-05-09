"""공통 테스트 픽스처"""

import pytest


@pytest.fixture
def settings():
    """테스트용 Settings 인스턴스"""
    from app.config import Settings
    return Settings()
