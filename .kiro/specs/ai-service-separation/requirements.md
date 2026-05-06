# 요구사항 문서

## 소개

AI 서비스를 기존 백엔드 모놀리스(`backend/`)에서 분리하여 독립적인 Python 서비스(`ai/`)로 구성한다. AI 서비스는 AWS Bedrock(Claude)과의 모든 상호작용을 담당하며, 백엔드와는 SQS를 통해 비동기 메시징으로 통신한다. 현재 퀴즈 생성 기능을 마이그레이션하고, 회고 작성 및 일기 작성을 위한 멀티턴 대화 기능을 지원하는 구조를 갖춘다.

## 용어 정의

- **AI_Service**: `ai/` 폴더에 위치한 독립 Python 애플리케이션(FastAPI 기반)으로, Bedrock 호출 및 AI 관련 처리를 전담하는 서비스
- **Backend**: `backend/` 폴더에 위치한 기존 Spring Boot 모놀리스로, 비즈니스 로직과 사용자 관리를 담당하는 서비스
- **Bedrock_Client**: AWS Bedrock Runtime API를 호출하여 Claude 모델과 통신하는 컴포넌트
- **SQS_Listener**: SQS 큐에서 메시지를 폴링하여 처리하는 컴포넌트
- **SQS_Publisher**: SQS 큐로 메시지를 발행하는 컴포넌트
- **Conversation_Manager**: 멀티턴 대화의 상태(메시지 히스토리)를 관리하는 컴포넌트
- **Prompt_Builder**: 각 기능별 프롬프트를 구성하는 컴포넌트
- **Response_Parser**: Bedrock 응답을 구조화된 객체로 파싱하는 컴포넌트
- **Request_Queue**: Backend에서 AI_Service로 요청을 전달하는 SQS 큐
- **Response_Queue**: AI_Service에서 Backend로 응답을 전달하는 SQS 큐
- **Multi_Turn_Conversation**: 여러 차례의 질문-응답을 통해 맥락을 유지하며 진행되는 대화 방식
- **Session**: 하나의 멀티턴 대화 흐름을 식별하는 단위 (sessionId로 구분)

## 요구사항

### 요구사항 1: 독립 Python 프로젝트 구성

**사용자 스토리:** 개발자로서, AI 서비스를 `ai/` 폴더에 독립적인 Python(FastAPI) 프로젝트로 구성하여 백엔드와 별도로 배포, 확장, 유지보수할 수 있기를 원한다.

#### 인수 조건

1. AI_Service는 워크스페이스 루트의 `ai/` 디렉토리에 위치한 독립 Python 애플리케이션이어야 한다
2. AI_Service는 FastAPI를 웹 프레임워크로 사용하고, Python 3.11 이상을 요구해야 한다
3. AI_Service는 boto3(AWS SDK), pydantic(데이터 검증), uvicorn(ASGI 서버) 의존성을 포함해야 한다
4. AI_Service는 패키지 관리를 위해 Poetry 또는 pip + requirements.txt를 사용해야 한다
5. AI_Service는 자체 설정 파일(`.env` 또는 `config.py`)에 AWS 리전, Bedrock 모델 설정, SQS 큐 이름을 포함해야 한다
6. AI_Service는 Backend와 다른 설정 가능한 포트에서 시작해야 한다 (기본값: 8081)
7. AI_Service는 Backend 모듈이나 공유 라이브러리에 의존하지 않아야 한다

### 요구사항 2: Bedrock 클라이언트 마이그레이션

**사용자 스토리:** 개발자로서, Bedrock 클라이언트 설정 및 호출 로직이 AI 서비스에만 존재하여 백엔드가 AWS Bedrock SDK에 직접 의존하지 않기를 원한다.

#### 인수 조건

1. AI_Service는 boto3를 사용하여 설정 가능한 리전, 타임아웃, 자격증명으로 Bedrock Runtime 클라이언트를 구성해야 한다
2. Bedrock_Client는 주어진 프롬프트로 Claude 모델을 호출하고 원시 텍스트 응답을 반환해야 한다
3. Bedrock_Client는 환경 변수를 통해 설정 가능한 모델 ID와 최대 토큰 수를 지원해야 한다
4. Bedrock API 호출이 실패하면, Bedrock_Client는 에러 메시지를 포함한 설명적 예외를 발생시켜야 한다
5. AI_Service가 배포되면, Backend에는 Bedrock SDK 의존성이나 Bedrock 관련 설정이 없어야 한다

### 요구사항 3: SQS 메시징 통신

**사용자 스토리:** 개발자로서, AI 서비스가 백엔드와 SQS 큐를 통해서만 통신하여 서비스 간 결합도를 낮추고 독립적으로 확장할 수 있기를 원한다.

#### 인수 조건

1. AI_Service는 지정된 Request_Queue에서 수신 처리 요청을 폴링해야 한다
2. Request_Queue에서 메시지를 수신하면, SQS_Listener는 메시지를 역직렬화하고 `action` 필드에 따라 적절한 핸들러로 라우팅해야 한다
3. 처리가 완료되면, SQS_Publisher는 지정된 Response_Queue로 응답 메시지를 전송해야 한다
4. AI_Service는 기능별(퀴즈, 회고, 일기) 별도의 요청/응답 큐 쌍을 지원해야 한다
5. 메시지 역직렬화가 실패하면, SQS_Listener는 에러를 로깅하고 크래시 없이 메시지를 폐기해야 한다
6. 응답 큐 발행이 실패하면, SQS_Publisher는 설정 가능한 횟수만큼 재시도한 후 실패를 로깅해야 한다
7. AI_Service는 설정 가능한 최대 동시 메시지 수로 SQS 폴링을 구성해야 한다
8. AI_Service는 asyncio 기반 비동기 SQS 폴링을 사용하여 높은 처리량을 지원해야 한다

### 요구사항 4: 퀴즈 생성 기능 마이그레이션

**사용자 스토리:** 개발자로서, 기존 퀴즈 생성 로직을 외부 SQS 메시지 계약을 변경하지 않고 AI 서비스로 마이그레이션하여, 백엔드의 퀴즈 요청/응답 흐름을 수정 없이 유지하고 싶다.

#### 인수 조건

1. 퀴즈 요청 메시지를 수신하면, AI_Service는 대상 유저 프로필을 기반으로 퀴즈를 생성해야 한다
2. Prompt_Builder는 대상 프로필 필드(name, nickname, university, major, hobbies, interests, personalityType)와 설정 가능한 템플릿을 사용하여 퀴즈 프롬프트를 구성해야 한다
3. Response_Parser는 Bedrock JSON 응답을 구조화된 퀴즈 문제 객체로 파싱해야 한다
4. Response_Parser가 유효하지 않은 응답을 만나면, AI_Service는 대상 프로필 기반의 폴백 퀴즈를 사용해야 한다
5. AI_Service는 현재 구현과 동일한 QuizRequestMessage 및 QuizResponseMessage JSON 스키마를 유지해야 한다
6. 첫 번째 Bedrock 호출이 실패하면, AI_Service는 폴백 전에 한 번 재시도해야 한다
7. 모든 유효한 QuizRequestMessage 객체에 대해, 직렬화 후 역직렬화하면 동등한 객체가 생성되어야 한다 (라운드트립 속성)

### 요구사항 5: 멀티턴 대화 지원 구조

**사용자 스토리:** 개발자로서, AI 서비스가 멀티턴 대화를 지원하여 회고 작성이나 일기 작성 같은 기능에서 맥락을 유지하며 후속 질문을 할 수 있기를 원한다.

#### 인수 조건

1. Conversation_Manager는 고유한 sessionId로 식별되는 세션별 메시지 히스토리를 유지해야 한다
2. 기존 sessionId가 포함된 대화 메시지를 수신하면, Conversation_Manager는 새 사용자 메시지를 기존 히스토리에 추가해야 한다
3. Bedrock_Client는 멀티턴 세션에서 모델을 호출할 때 전체 대화 히스토리(시스템 프롬프트 + 모든 이전 메시지)를 전송해야 한다
4. 대화가 완료로 표시되면, Conversation_Manager는 세션 히스토리를 정리하고 최종 컴파일된 결과를 발행해야 한다
5. Conversation_Manager는 세션당 설정 가능한 최대 턴 수를 적용해야 한다
6. 최대 턴 수를 초과하면, Conversation_Manager는 대화를 강제 완료하고 가용한 답변을 컴파일해야 한다
7. AI_Service는 설정 가능한 세션 타임아웃을 지원하여, 비활성 세션을 자동으로 정리해야 한다

### 요구사항 6: 회고(Retrospective) 작성 기능

**사용자 스토리:** 사용자로서, AI 플래너가 다른 유저와의 만남에 대해 질문하고 내 답변을 모아 회고글을 작성해주어, 사회적 상호작용을 쉽게 되돌아볼 수 있기를 원한다.

#### 인수 조건

1. 회고 세션 요청을 수신하면, AI_Service는 회고 작성에 맞춤화된 시스템 프롬프트로 멀티턴 대화를 시작해야 한다
2. Prompt_Builder는 만남 맥락(매칭된 유저 정보, 만남 날짜, 만남 장소 등)을 기반으로 초기 질문을 생성해야 한다
3. 회고 세션 시작 시, AI_Service는 해당 유저의 과거 회고글을 벡터 검색(RAG)하여 프롬프트 컨텍스트에 포함해야 한다
4. Prompt_Builder는 만남 맥락과 RAG로 검색된 과거 회고 데이터를 함께 활용하여 더 개인화되고 깊이 있는 질문을 생성해야 한다
5. 사용자가 답변을 제공하면, AI_Service는 이전 답변과 만남 맥락, 과거 회고 데이터를 종합하여 다음 꼬리질문을 생성하고 Response_Queue를 통해 전송해야 한다
6. 대화가 완료로 표시되면(사용자가 완료 신호를 보내거나 최대 턴 수에 도달), AI_Service는 모든 답변을 구조화된 회고글로 컴파일해야 한다
7. AI_Service는 컴파일된 회고 콘텐츠를 Backend로 전송하는 최종 응답 메시지에 포함해야 한다
8. 완성된 회고글은 벡터 데이터베이스에 임베딩 저장하여 향후 RAG 검색에 활용해야 한다
9. 사용자가 세션 타임아웃 내에 응답하지 않으면, AI_Service는 가용한 답변으로 부분 회고글을 컴파일하고 Backend에 알려야 한다

### 요구사항 7: 일기(Diary) 작성 기능

**사용자 스토리:** 사용자로서, AI 어시스턴트가 대화형 흐름을 통해 일기 작성을 안내해주어, 일상의 생각과 경험을 쉽게 기록할 수 있기를 원한다.

#### 인수 조건

1. 일기 세션 요청을 수신하면, AI_Service는 일기 작성에 맞춤화된 시스템 프롬프트로 멀티턴 대화를 시작해야 한다
2. 일기 세션 시작 시, AI_Service는 해당 날의 플래너 엔트리 데이터를 요청 메시지에서 수신하여 질문 생성에 활용해야 한다
3. 일기 세션 시작 시, AI_Service는 해당 유저의 과거 일기를 벡터 검색(RAG)하여 일기 작성 패턴(주제, 감정 표현 방식, 관심사 변화 등)을 파악해야 한다
4. Prompt_Builder는 당일 플래너 데이터와 RAG로 검색된 과거 일기 패턴을 함께 활용하여 개인화된 안내 질문을 생성해야 한다
5. 사용자가 답변을 제공하면, AI_Service는 플래너 맥락과 과거 일기 패턴을 종합하여 다음 안내 질문을 생성하고 Response_Queue를 통해 전송해야 한다
6. 대화가 완료로 표시되면, AI_Service는 모든 답변을 잘 구조화된 일기 항목으로 컴파일해야 한다
7. AI_Service는 컴파일된 일기 콘텐츠를 Backend로 전송하는 최종 응답 메시지에 포함해야 한다
8. 완성된 일기는 벡터 데이터베이스에 임베딩 저장하여 향후 RAG 검색에 활용해야 한다
9. 사용자가 세션 타임아웃 내에 응답하지 않으면, AI_Service는 가용한 답변으로 부분 일기를 컴파일하고 Backend에 알려야 한다

### 요구사항 8: 백엔드 AI 코드 제거

**사용자 스토리:** 개발자로서, 마이그레이션 후 백엔드에서 모든 AI 관련 코드를 제거하여, 백엔드가 비즈니스 로직에만 집중하고 불필요한 Bedrock 의존성을 갖지 않기를 원한다.

#### 인수 조건

1. AI_Service가 운영 가능해지면, Backend에서 전체 `domain/ai/` 패키지를 제거해야 한다
2. Backend는 build.gradle에서 AWS Bedrock SDK 의존성을 제거해야 한다
3. Backend는 요청 발행 및 응답 수신을 위한 SQS 클라이언트 설정만 유지해야 한다 (Bedrock 관련 SQS 리스너 없음)
4. Backend는 SQS 메시지 직렬화를 위한 퀴즈 요청/응답 DTO 정의를 유지해야 한다
5. Backend는 application.yml에 AWS Bedrock 자격증명이나 설정이 필요하지 않아야 한다

### 요구사항 9: 에러 처리 및 복원력

**사용자 스토리:** 개발자로서, AI 서비스가 에러를 우아하게 처리하고 의미 있는 피드백을 제공하여, AI 처리 실패가 백엔드로 전파되거나 요청이 유실되지 않기를 원한다.

#### 인수 조건

1. 재시도 후에도 Bedrock API 호출이 실패하면, AI_Service는 상태 "FAILED"와 에러 설명이 포함된 에러 응답 메시지를 Response_Queue로 전송해야 한다
2. 메시지 처리 중 예상치 못한 예외가 발생하면, AI_Service는 전체 스택 트레이스를 로깅하고 일반 에러 응답을 전송해야 한다
3. AI_Service는 SQS 연결성과 Bedrock 클라이언트 가용성을 확인하는 헬스체크 엔드포인트(`/health`)를 구현해야 한다
4. AI_Service가 요청을 처리하는 동안, 무한 대기를 방지하기 위해 요청당 설정 가능한 전체 타임아웃을 적용해야 한다
5. AI_Service는 추적성을 위해 상관 ID(matchId, sessionId)를 포함한 구조화된 로깅을 사용해야 한다

### 요구사항 10: 설정 및 환경 관리

**사용자 스토리:** 개발자로서, AI 서비스가 환경 변수와 설정 파일로 설정 가능하여, 코드 변경 없이 다양한 환경에 배포할 수 있기를 원한다.

#### 인수 조건

1. AI_Service는 모든 AWS 자격증명, 리전, 큐 이름, 모델 ID, 타임아웃 값을 환경 변수로 외부화해야 한다
2. AI_Service는 pydantic-settings를 사용하여 환경 변수 기반 설정 관리를 구현해야 한다
3. AI_Service는 로컬 개발을 위해 모든 설정에 합리적인 기본값을 제공해야 한다
4. AI_Service는 환경별(local, dev, prod) 설정 파일을 지원해야 한다
5. AI_Service는 로컬 개발을 위해 `.env` 파일에서 환경 변수를 로드해야 한다
