"""공통 예외 정의.

AI 서비스 전반에서 사용되는 구조화된 예외 클래스를 정의한다.
각 예외는 디버깅에 필요한 컨텍스트 정보를 속성으로 포함한다.
"""


class AIServiceError(Exception):
    """AI 서비스의 모든 예외에 대한 기본 클래스.

    모든 커스텀 예외는 이 클래스를 상속하여
    일관된 에러 처리 및 로깅을 지원한다.
    """

    def __init__(self, message: str = "AI 서비스 에러가 발생했습니다"):
        self.message = message
        super().__init__(self.message)

    def __str__(self) -> str:
        return self.message


class BedrockInvocationError(AIServiceError):
    """Bedrock API 호출 실패 시 발생하는 예외.

    Attributes:
        model_id: 호출에 사용된 Bedrock 모델 ID
        detail: 실패 원인에 대한 상세 설명
    """

    def __init__(self, model_id: str, detail: str):
        self.model_id = model_id
        self.detail = detail
        message = f"Bedrock 모델 '{model_id}' 호출 실패: {detail}"
        super().__init__(message)


class MessageParseError(AIServiceError):
    """SQS 메시지 역직렬화 실패 시 발생하는 예외.

    Attributes:
        raw_snippet: 파싱에 실패한 원본 메시지의 일부 (최대 200자)
    """

    def __init__(self, raw_snippet: str):
        self.raw_snippet = raw_snippet[:200]
        message = f"메시지 파싱 실패 - 원본 일부: '{self.raw_snippet}'"
        super().__init__(message)


class SessionNotFoundError(AIServiceError):
    """요청된 대화 세션을 찾을 수 없을 때 발생하는 예외.

    Attributes:
        session_id: 찾을 수 없는 세션의 ID
    """

    def __init__(self, session_id: str):
        self.session_id = session_id
        message = f"세션을 찾을 수 없습니다: session_id='{session_id}'"
        super().__init__(message)


class SessionExpiredError(AIServiceError):
    """세션이 타임아웃으로 만료되었을 때 발생하는 예외.

    Attributes:
        session_id: 만료된 세션의 ID
    """

    def __init__(self, session_id: str):
        self.session_id = session_id
        message = f"세션이 만료되었습니다: session_id='{session_id}'"
        super().__init__(message)


class PublishError(AIServiceError):
    """SQS 메시지 발행이 재시도 후에도 실패했을 때 발생하는 예외.

    Attributes:
        queue_name: 발행에 실패한 대상 큐 이름
    """

    def __init__(self, queue_name: str):
        self.queue_name = queue_name
        message = f"SQS 메시지 발행 실패 (재시도 소진): queue='{queue_name}'"
        super().__init__(message)
