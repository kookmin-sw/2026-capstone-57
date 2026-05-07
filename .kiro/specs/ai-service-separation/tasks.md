# 구현 계획: AI 서비스 분리

## 개요

기존 Spring Boot 백엔드에 포함된 AI 관련 코드를 독립적인 Python/FastAPI 서비스(`ai/`)로 분리한다. 프로젝트 구조 설정부터 시작하여, 핵심 인프라(SQS, Bedrock), 퀴즈 마이그레이션, 멀티턴 대화, RAG 파이프라인, 회고/일기 기능 순으로 점진적으로 구현한다.

## Tasks

- [ ] 1. 프로젝트 구조 및 핵심 설정 구성
  - [ ] 1.1 Poetry 기반 프로젝트 초기화 및 디렉토리 구조 생성
    - `ai/` 디렉토리에 `pyproject.toml` 생성 (Python 3.11+, FastAPI, uvicorn, boto3, aioboto3, pydantic, pydantic-settings, chromadb, pytest, hypothesis, pytest-asyncio, moto, httpx 의존성 포함)
    - 설계 문서의 프로젝트 구조에 따라 `app/`, `tests/` 디렉토리 및 `__init__.py` 파일 생성
    - `.env.example` 파일 생성 (모든 환경 변수 키 나열)
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ] 1.2 pydantic-settings 기반 설정 모듈 구현
    - `app/config.py`에 `Settings` 클래스 구현 (AWS 리전, Bedrock 모델 ID, SQS 큐 이름, 대화 설정, RAG 설정, 서버 포트 등 모든 설정 포함)
    - `.env` 파일 로드 지원 및 합리적인 기본값 제공
    - _Requirements: 1.5, 1.6, 10.1, 10.2, 10.3, 10.5_

  - [ ] 1.3 FastAPI 앱 진입점 및 헬스체크 엔드포인트 구현
    - `app/main.py`에 FastAPI 앱 생성, lifespan 이벤트 핸들러 설정
    - `app/health.py`에 `/health` 엔드포인트 구현 (SQS 연결성, Bedrock 가용성 확인)
    - _Requirements: 1.6, 9.3_

  - [ ] 1.4 공통 예외 및 구조화된 로깅 모듈 구현
    - `app/common/exceptions.py`에 공통 예외 클래스 정의 (BedrockInvocationError, MessageParseError, SessionNotFoundError 등)
    - `app/common/logging.py`에 구조화된 JSON 로깅 설정 (correlation_id, feature, error_type 포함)
    - _Requirements: 9.2, 9.5_

- [ ] 2. SQS 메시징 인프라 구현
  - [ ] 2.1 asyncio 기반 SQS Poller 구현
    - `app/sqs/poller.py`에 `SQSPoller` 클래스 구현
    - aioboto3를 사용한 비동기 SQS 폴링 루프 (큐별 독립 asyncio 태스크)
    - 설정 가능한 폴링 간격, 최대 동시 메시지 수 지원
    - graceful shutdown 지원 (`stop()` 메서드)
    - _Requirements: 3.1, 3.7, 3.8_

  - [ ] 2.2 메시지 라우터 구현
    - `app/sqs/router.py`에 `MessageRouter` 클래스 구현
    - `action` 필드 기반 핸들러 라우팅
    - 역직렬화 실패 시 에러 로깅 및 메시지 폐기 (예외 전파 없음)
    - _Requirements: 3.2, 3.5_

  - [ ] 2.3 SQS Publisher 구현
    - `app/sqs/publisher.py`에 `SQSPublisher` 클래스 구현
    - Pydantic 모델을 JSON 직렬화하여 큐로 전송
    - 설정 가능한 재시도 로직 (기본 3회)
    - 재시도 실패 시 에러 로깅
    - _Requirements: 3.3, 3.6_

  - [ ]* 2.4 메시지 라우팅 속성 테스트 작성
    - **Property 1: 메시지 라우팅 정확성**
    - **Property 2: 잘못된 메시지 복원력**
    - **Validates: Requirements 3.2, 3.5**

- [ ] 3. Bedrock 클라이언트 구현
  - [ ] 3.1 Bedrock Runtime 클라이언트 구현
    - `app/bedrock/client.py`에 `BedrockClient` 클래스 구현
    - 단일 프롬프트 호출 (`invoke`), 메시지 히스토리 호출 (`invoke_with_messages`), 재시도 포함 호출 (`invoke_with_retry`) 메서드 구현
    - 설정 가능한 모델 ID, 최대 토큰, 타임아웃 지원
    - 실패 시 설명적 예외 발생 (BedrockInvocationError)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ]* 3.2 Bedrock 클라이언트 단위 테스트 작성
    - moto 기반 mock으로 재시도 로직 테스트
    - 타임아웃 및 에러 응답 처리 테스트
    - _Requirements: 2.4_

- [ ] 4. 체크포인트 - 핵심 인프라 검증
  - 모든 테스트 통과 확인, 질문이 있으면 사용자에게 문의.

- [ ] 5. 퀴즈 생성 기능 마이그레이션
  - [ ] 5.1 퀴즈 메시지 모델 정의
    - `app/features/quiz/models.py`에 QuizRequestMessage, QuizResponseMessage, TargetProfile, QuizQuestion 등 Pydantic 모델 정의
    - 기존 Java 구현의 JSON 스키마와 동일한 필드명 및 구조 유지
    - _Requirements: 4.5, 4.7_

  - [ ] 5.2 퀴즈 프롬프트 빌더 구현
    - `app/features/quiz/prompt.py`에 프로필 기반 퀴즈 프롬프트 생성 로직 구현
    - 대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함
    - 설정 가능한 프롬프트 템플릿 지원
    - _Requirements: 4.2_

  - [ ] 5.3 퀴즈 응답 파서 및 폴백 로직 구현
    - `app/features/quiz/parser.py`에 Bedrock JSON 응답 파싱 로직 구현
    - 파싱 실패 시 프로필 기반 폴백 퀴즈 생성 (5문제, 각 4선택지, correctIndex 0-3)
    - _Requirements: 4.3, 4.4_

  - [ ] 5.4 퀴즈 핸들러 구현
    - `app/features/quiz/handler.py`에 퀴즈 생성 전체 흐름 구현
    - 요청 수신 → 프롬프트 생성 → Bedrock 호출 → 응답 파싱 → 결과 발행
    - 첫 번째 실패 시 1회 재시도, 재시도 실패 시 폴백 사용
    - 요청당 타임아웃 적용
    - _Requirements: 4.1, 4.6, 9.1, 9.4_

  - [ ]* 5.5 퀴즈 직렬화 및 프롬프트 속성 테스트 작성
    - **Property 3: 퀴즈 프롬프트 완전성**
    - **Property 4: 퀴즈 응답 파싱 정확성**
    - **Property 5: 잘못된 응답 시 폴백 정확성**
    - **Property 6: 퀴즈 메시지 직렬화 라운드트립**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 4.7**

- [ ] 6. 멀티턴 대화 관리자 구현
  - [ ] 6.1 대화 세션 모델 정의
    - `app/conversation/models.py`에 Session, ConversationMessage, SessionStatus, MessageRole 등 Pydantic 모델 정의
    - _Requirements: 5.1_

  - [ ] 6.2 Conversation Manager 구현
    - `app/conversation/manager.py`에 `ConversationManager` 클래스 구현
    - 세션 생성 (`create_session`), 메시지 추가 (`add_message`), 히스토리 조회 (`get_history`), 세션 완료 (`complete_session`) 메서드 구현
    - 인메모리 세션 저장소 (dict 기반)
    - 설정 가능한 최대 턴 수 적용, 초과 시 강제 완료
    - 설정 가능한 세션 타임아웃, 만료 세션 자동 정리 (`cleanup_expired`)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [ ]* 6.3 대화 관리자 속성 테스트 작성
    - **Property 7: 세션 히스토리 무결성**
    - **Property 8: 최대 턴 수 강제 적용**
    - **Validates: Requirements 5.1, 5.2, 5.5, 5.6**

- [ ] 7. RAG 파이프라인 구현
  - [ ] 7.1 임베딩 생성 모듈 구현
    - `app/rag/embeddings.py`에 Amazon Titan Embeddings 호출 로직 구현
    - Bedrock Runtime API를 통한 텍스트 임베딩 벡터 생성
    - _Requirements: 6.3, 7.3_

  - [ ] 7.2 ChromaDB 벡터 스토어 연동 구현
    - `app/rag/vector_store.py`에 ChromaDB 클라이언트 설정 및 컬렉션 관리 구현
    - `retrospectives`, `diaries` 컬렉션 생성/관리
    - user_id 기반 메타데이터 필터링 지원
    - _Requirements: 6.3, 6.8, 7.3, 7.8_

  - [ ] 7.3 RAG 검색 파이프라인 구현
    - `app/rag/pipeline.py`에 `RAGPipeline` 클래스 구현
    - 검색 (`search`): 쿼리 임베딩 → 벡터 유사도 검색 → user_id 필터 → 상위 K개 반환
    - 저장 (`store`): 텍스트 청킹 (500자, 100자 오버랩) → 임베딩 생성 → ChromaDB 저장
    - _Requirements: 6.3, 6.4, 6.8, 7.3, 7.4, 7.8_

  - [ ]* 7.4 RAG 파이프라인 단위 테스트 작성
    - ChromaDB 인메모리 모드로 저장/검색 흐름 테스트
    - user_id 필터링 정확성 테스트
    - _Requirements: 6.3, 7.3_

- [ ] 8. 체크포인트 - 대화 및 RAG 인프라 검증
  - 모든 테스트 통과 확인, 질문이 있으면 사용자에게 문의.

- [ ] 9. 회고(Retrospective) 작성 기능 구현
  - [ ] 9.1 회고 메시지 모델 정의
    - `app/features/retrospective/models.py`에 RetroRequestMessage, RetroResponseMessage, MeetingContext 등 Pydantic 모델 정의
    - action 필드: START_SESSION, ANSWER, COMPLETE (요청) / QUESTION, COMPLETED, FAILED (응답)
    - _Requirements: 6.1_

  - [ ] 9.2 회고 프롬프트 빌더 구현
    - `app/features/retrospective/prompt.py`에 회고 전용 시스템 프롬프트 및 질문 생성 로직 구현
    - 만남 맥락(matchedUserName, meetingDate, meetingLocation) 포함
    - RAG 검색 결과(과거 회고글) 컨텍스트 포함
    - _Requirements: 6.2, 6.4_

  - [ ] 9.3 회고 핸들러 구현
    - `app/features/retrospective/handler.py`에 회고 전체 흐름 구현
    - START_SESSION: RAG 검색 → 세션 생성 → 첫 질문 생성 → 응답 발행
    - ANSWER: 답변 추가 → 후속 질문 생성 → 응답 발행
    - COMPLETE: 전체 답변 컴파일 → 벡터 DB 저장 → 최종 결과 발행
    - 세션 타임아웃 시 부분 컴파일 처리
    - _Requirements: 6.1, 6.5, 6.6, 6.7, 6.8, 6.9_

  - [ ]* 9.4 회고 프롬프트 속성 테스트 작성
    - **Property 9: 회고 프롬프트 컨텍스트 포함**
    - **Validates: Requirements 6.2, 6.4**

- [ ] 10. 일기(Diary) 작성 기능 구현
  - [ ] 10.1 일기 메시지 모델 정의
    - `app/features/diary/models.py`에 DiaryRequestMessage, DiaryResponseMessage, PlannerEntry 등 Pydantic 모델 정의
    - action 필드: START_SESSION, ANSWER, COMPLETE (요청) / QUESTION, COMPLETED, FAILED (응답)
    - _Requirements: 7.1_

  - [ ] 10.2 일기 프롬프트 빌더 구현
    - `app/features/diary/prompt.py`에 일기 전용 시스템 프롬프트 및 질문 생성 로직 구현
    - 플래너 엔트리(name, startTime, endTime, type) 포함
    - RAG 검색 결과(과거 일기 패턴) 컨텍스트 포함
    - _Requirements: 7.2, 7.4_

  - [ ] 10.3 일기 핸들러 구현
    - `app/features/diary/handler.py`에 일기 전체 흐름 구현
    - START_SESSION: 플래너 데이터 수신 + RAG 검색 → 세션 생성 → 첫 질문 생성 → 응답 발행
    - ANSWER: 답변 추가 → 후속 질문 생성 → 응답 발행
    - COMPLETE: 전체 답변 컴파일 → 벡터 DB 저장 → 최종 결과 발행
    - 세션 타임아웃 시 부분 컴파일 처리
    - _Requirements: 7.1, 7.5, 7.6, 7.7, 7.8, 7.9_

  - [ ]* 10.4 일기 프롬프트 속성 테스트 작성
    - **Property 10: 일기 프롬프트 컨텍스트 포함**
    - **Validates: Requirements 7.2, 7.4**

- [ ] 11. 체크포인트 - 전체 기능 검증
  - 모든 테스트 통과 확인, 질문이 있으면 사용자에게 문의.

- [ ] 12. FastAPI 앱 통합 및 라이프사이클 연결
  - [ ] 12.1 앱 시작 시 모든 컴포넌트 초기화 및 연결
    - `app/main.py`의 lifespan에서 Settings 로드, BedrockClient/SQSPoller/SQSPublisher/ConversationManager/RAGPipeline 초기화
    - MessageRouter에 퀴즈/회고/일기 핸들러 등록
    - SQS Poller 시작 (기능별 큐 폴링 태스크)
    - 세션 만료 정리 주기적 태스크 등록
    - _Requirements: 1.6, 3.1, 3.4, 3.8_

  - [ ] 12.2 설정 기본값 완전성 검증 테스트 작성
    - **Property 11: 설정 기본값 완전성**
    - **Validates: Requirements 10.3**

  - [ ]* 12.3 통합 테스트 작성
    - SQS 폴링 → 핸들러 → 응답 발행 전체 흐름 테스트 (moto 사용)
    - 멀티턴 대화 전체 흐름 테스트 (Bedrock mock)
    - _Requirements: 3.1, 3.2, 3.3, 5.1, 5.2, 5.3_

- [ ] 13. 백엔드 AI 코드 제거
  - [ ] 13.1 Backend에서 AI 관련 코드 및 의존성 제거
    - `backend/src/main/java/com/ilgiyebo/domain/ai/` 패키지 전체 제거 (config, exception, quiz, service)
    - `build.gradle`에서 AWS Bedrock SDK 의존성 제거
    - `application.yml`에서 Bedrock 관련 설정 제거
    - SQS 클라이언트 설정 및 퀴즈 요청/응답 DTO는 유지 (메시지 발행/수신용)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 14. 최종 체크포인트 - 전체 시스템 검증
  - Backend 빌드 성공 확인 (AI 코드 제거 후)
  - AI 서비스 전체 테스트 통과 확인
  - 질문이 있으면 사용자에게 문의.

## Notes

- `*` 표시된 태스크는 선택적이며 빠른 MVP를 위해 건너뛸 수 있음
- 각 태스크는 추적성을 위해 구체적인 요구사항을 참조함
- 체크포인트에서 점진적 검증을 수행하여 안정성 확보
- 속성 테스트는 설계 문서의 Correctness Properties를 검증함
- 단위 테스트는 구체적인 예시와 에지 케이스를 검증함
