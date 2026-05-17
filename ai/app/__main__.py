"""AI 서비스 실행 진입점.

uvicorn을 사용하여 설정된 포트에서 FastAPI 앱을 시작한다.

사용 예시:
    python -m app
"""

import uvicorn

from app.config import get_settings


def main() -> None:
    """설정된 포트에서 AI 서비스를 시작한다."""
    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.server_port,
        reload=False,
    )


if __name__ == "__main__":
    main()
