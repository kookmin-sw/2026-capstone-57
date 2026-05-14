# 구현 계획: 일기예보 (ilgi-yebo)

## 개요

일기예보 서비스의 백엔드 및 데이터 계층을 Spring Boot (Java)로 구현한다. 데이터베이스 스키마 → 핵심 도메인 서비스 → 매칭 엔진 → 상호작용 흐름 → 경험치/안전/알림 순으로 점진적으로 구축하며, 각 단계에서 속성 기반 테스트(jqwik)로 정확성을 검증한다.

## 태스크

- [x] 1. 프로젝트 초기 설정 및 데이터베이스 스키마 구성
  - [x] 1.1 Spring Boot 프로젝트 구조 생성 및 의존성 설정
    - Spring Boot 프로젝트 초기화 (Java 17+, Gradle)
    - 의존성: Spring Web, Spring Data JPA, Spring Security, Spring WebSocket, Spring AMQP, Spring Data Redis, MySQL Driver (mysql-connector-j), Flyway (DB 마이그레이션), jqwik (PBT), JUnit 5, Mockito, AWS SDK for Java v2 (Bedrock Runtime)
    - 디렉토리 구조 (Spring Boot 컨벤션):
      ```
      src/main/java/com/ilgiyebo/
        config/          # Spring 설정 (Security, Redis, AMQP, Bedrock 등)
        domain/          # JPA 엔티티 (도메인 모델)
        repository/      # Spring Data JPA Repository
        service/         # 비즈니스 로직 서비스
        controller/      # REST API 컨트롤러
        dto/             # 요청/응답 DTO (Java record)
        scheduler/       # Spring Scheduler (배치 매칭, 리마인더)
        util/            # 유틸리티 클래스
      src/main/resources/
        db/migration/    # Flyway 마이그레이션 SQL
        application.yml  # Spring Boot 설정
      src/test/java/com/ilgiyebo/
        service/         # 서비스 단위 테스트
        property/        # jqwik 속성 기반 테스트
        integration/     # 통합 테스트
      ```
    - _요구사항: 전체_

  - [x] 1.2 데이터베이스 마이그레이션 및 스키마 생성
    - Flyway 마이그레이션 SQL (MySQL 문법)로 모든 테이블 생성 (USER, SLOT, MATCH, INTERACTION, DIARY_ENTRY, PLAN_ENTRY, TIMETABLE_ENTRY, CHAT_SESSION, CHAT_MESSAGE, GAME_SESSION, MISSION, REVIEW, REVIEW_SESSION, AI_REVIEW_QUESTION, HINT_QUESTION, EXP_HISTORY, REPORT, BLOCK, NOTIFICATION_SETTING, CAMPUS_BUILDING, CAMPUS_PATH, CAMPUS_VENUE)
    - UUID는 BINARY(16) 타입으로 저장, PostgreSQL 배열 타입 대신 JSON 타입 사용
    - USER 테이블에 name (VARCHAR), major (VARCHAR), birth_date (DATE), gender (ENUM) 컬럼 포함
    - SLOT 테이블에 match_preferences (JSON) 컬럼 포함
    - MATCH 테이블에 cycle_extended (BOOLEAN), extension_status (ENUM), extension_requested_by (BINARY(16)) 컬럼 포함
    - 설계 문서의 인덱스 전략에 따른 인덱스 생성 (MySQL 호환)
    - UNIQUE 제약 조건 설정 (DIARY_ENTRY: user_id+entry_date, PLAN_ENTRY: user_id+entry_date, BLOCK: user_id+blocked_user_id, REVIEW_SESSION: interaction_id+user_id, CAMPUS_PATH: from_building_id+to_building_id)
    - _요구사항: 전체_

  - [x] 1.3 JPA 엔티티 및 공통 타입 정의
    - 설계 문서의 모든 데이터 모델을 `domain/` 하위에 JPA 엔티티(@Entity)로 정의 (AIService, CampusDataService 관련 포함)
    - USER 엔티티에 name (String), major (String), birthDate (LocalDate), gender (Gender enum) 필드 추가
    - SLOT 엔티티에 matchPreferences (MatchPreferences JSON) 필드 추가
    - MATCH 엔티티에 cycleExtended (boolean), extensionStatus (ExtensionStatus enum), extensionRequestedBy (UUID) 필드 추가
    - MatchPreferences record 정의 (minAge, maxAge, genderPreference)
    - UUID를 BINARY(16)으로 매핑하는 커스텀 타입 또는 JPA AttributeConverter 설정
    - JSON 컬럼 매핑을 위한 JPA AttributeConverter 설정 (배열 타입 대체)
    - 공통 DTO (Java record), enum, 유틸리티 타입 정의
    - Spring Data JPA Repository 인터페이스 정의
    - application.yml에 MySQL dialect 설정 (org.hibernate.dialect.MySQLDialect)
    - _요구사항: 전체_

- [ ] 2. 인증 및 사용자 서비스 구현
  - [x] 2.1 AuthService 구현
    - `sendVerification`: 대학 이메일 도메인 검증 후 인증 코드 발송 (EmailService 인터페이스를 통해 발송)
    - EmailService 인터페이스 정의 및 MailtrapEmailService 구현체 작성 (MVP 단계)
    - SesEmailService 구현체 스텁 작성 (프로덕션 전환 대비)
    - `verifyAndCreateUser`: 인증 코드 확인 및 사용자 생성
    - `login`: 이메일/비밀번호 로그인, Spring Security + JWT 토큰 발급
    - 허용 대학 도메인 목록 관리, 정지 계정 로그인 차단
    - _요구사항: 1.1, 1.2, 1.5_

  - [ ]* 2.2 Property 1 속성 테스트: 대학 이메일 도메인 검증
    - **Property 1: 대학 이메일 도메인 검증**
    - jqwik `@Property(tries = 100)` + `@ForAll` 임의의 이메일 문자열에 대해 허용 도메인만 인증 허용 검증
    - **검증 대상: 요구사항 1.1**

  - [x] 2.3 UserService 구현
    - `setupProfile`: 프로필 설정 (이름, 전공, 취미, 관심사, 성격 유형, 이상형, 생년월일, 성별) + Bean Validation 필수 필드 검증
    - MVP 단계: 사용자가 이름(name)과 전공(major)을 직접 입력
    - EmailParsingService 인터페이스 정의 (대학별 이메일 파싱 규칙 관리, SES 도입 후 활성화 대비)
    - 이메일 파싱 활성화 시: 인증 완료된 이메일에서 이름/전공 자동 추출하여 프로필에 사전 입력
    - `getProfile`, `updateProfile`: 프로필 조회/수정
    - 프로필 설정 완료 시 초기 슬롯 1개 자동 부여 (MatchingService.unlockSlot 연동)
    - _요구사항: 1.2, 1.3, 1.4, 1.6, 1.7, 1.8_

  - [ ]* 2.4 Property 2, 3, 44, 45 속성 테스트: 프로필 필수 필드, 초기 슬롯, 이메일 파싱, 이메일 서비스
    - **Property 2: 프로필 필수 필드 불변식** (이름, 전공, 생년월일, 성별 포함)
    - **Property 3: 신규 사용자 초기 슬롯 부여**
    - **Property 44: 이메일 파싱 라운드트립** (대학 이메일에서 이름/전공 파싱 검증)
    - **Property 45: 이메일 서비스 추상화 불변식** (Mailtrap/SES 구현체 동작 검증)
    - **검증 대상: 요구사항 1.3, 1.4, 1.5, 1.6, 1.8**

- [ ] 3. 플래너 및 일기 서비스 구현
  - [x] 3.1 PlannerService 구현
    - `createPlanEntry`: 날짜 기반 단일 일정 생성 (source=MANUAL)
    - `updatePlanEntry`: 일정 수정 (MANUAL, SCHEDULE_AUTO 모두 수정 가능)
    - `deletePlanEntry`: 일정 삭제
    - `getPlanEntries`: 특정 날짜 일정 목록 조회 (본인만 조회 가능)
    - 시간표 등록 시 학기 범위 내 PLAN_ENTRY 자동 생성 (ScheduleService.upsertMySchedule 연동)
    - 자동 생성 일정: source=SCHEDULE_AUTO, sourceScheduleId로 원본 시간표 참조
    - 직접 작성 일정: source=MANUAL
    - SCHEDULE 재등록 시 기존 SCHEDULE_AUTO 소스의 미래 PLAN_ENTRY 삭제 후 재생성
    - PLAN_ENTRY bulk insert 최적화 (학기 전체 × 주 5일 × 과목 수 대량 생성 대응)
    - 30분 단위 입력 검증 (startTime, endTime이 30분 단위인지)
    - 일정 시간 충돌 검증 (겹치는 시간대 등록 불가, 단 종료시간=시작시간 맞닿는 경우 허용)
    - 종료 시간이 시작 시간보다 이후인지 검증
    - 사용자는 본인 플래너만 조회/수정/삭제 가능
    - 3일 이상 source=MANUAL 플래너 미작성 시 오전 9시 알림 트리거
    - 플래너 작성 경험치 하루 1회만 지급
    - 현재 매칭은 SCHEDULE 기반으로만 수행 (AI 동선 분석 MVP 제외)
    - _요구사항: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.16_

  - [ ]* 3.2 Property 4, 5, 6, 11 속성 테스트: 플래너 관련
    - **Property 4: 플래너 데이터 라운드트립**
      - 저장 후 조회 시 동일 데이터 보장
    - **Property 5: 플래너 미작성 알림 트리거**
      - 최근 3일간 source=MANUAL 일정이 없으면 알림 발생
    - **Property 6: 시간표 기반 플래너 자동 생성**
      - 시간표 등록 시 PLAN_ENTRY 자동 생성 검증
      - source=SCHEDULE_AUTO 검증
      - sourceScheduleId 참조 정확성 검증
    - **Property 11: 일정 충돌 검증**
      - 겹치는 시간대 등록 실패
      - 종료시간=시작시간 경계 접촉 허용
      - 30분 단위 입력 검증
    - **검증 대상: 요구사항 2.2, 2.4, 2.9, 2.10, 2.11**

  - [x] 3.3 DiaryService 구현
    - `createEntry`: 일기 작성 (upsert, 빈 내용 검증, 감정 태그 선택)
    - DiaryEntry 엔티티에 source (MANUAL/AI_GENERATED) 필드 추가
    - DiaryEntry 엔티티에 aiSessionId (nullable, DiarySession FK) 필드 추가
    - `getEntries`: 일기 목록 조회 (본인만 접근 가능, Spring Data JPA Pageable)
    - `getStreak`: 연속 작성 일수 계산
    - `getEmotionTrend`: 감정 변화 추이 조회
    - 일기 작성 시 경험치 부여 연동, 연속 작성 보너스 경험치 로직
    - AI 생성 일기와 일반 일기 공존 지원 (source 필드로 구분)
    - _요구사항: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 3.4 DiarySessionService 구현 (AI 일기 멀티턴 대화)
    - DiarySession 엔티티 생성 (id, userId, targetDate, status, generatedContent, suggestedEmotion, maxTurns, currentTurn)
    - DiaryConversationTurn 엔티티 생성 (id, sessionId, turnNumber, question, answer, askedAt, answeredAt)
    - DiarySessionRepository, DiaryConversationTurnRepository 정의
    - `startAISession`: AI 일기 세션 시작, 당일 플래너 데이터를 컨텍스트로 AIService에 첫 질문 생성 요청
    - `answerQuestion`: 답변 저장 후 AIService에 다음 질문 생성 요청 (또는 대화 완료 판단)
    - `generateDiary`: 모든 답변 기반 AIService에 일기 내용 생성 요청, 세션 상태 GENERATED로 전환
    - `confirmDiary`: 사용자 확정 (선택적 수정 포함), DiaryService.createEntry 호출하여 DiaryEntry 저장, 세션 상태 COMPLETED로 전환
    - `getActiveSession`: 사용자별 날짜별 진행 중 세션 조회
    - `cancelSession`: 세션 취소 처리
    - 동일 날짜에 이미 완료된 세션이 있으면 새 세션 생성 방지
    - maxTurns 기본값 5, AI 조기 종료 지원
    - _요구사항: 3.1 (AI 일기 확장)_

  - [x] 3.5 AI 서버 일기 HTTP 클라이언트 연동
    - DiarySessionService에서 AIServiceClient의 일기 관련 HTTP 메서드 호출
    - `generateDiaryFirstQuestion`: 당일 플래너 + 전날 일기를 입력값으로 AI 서버에 전달
    - `generateDiaryNextQuestion`: 이전 대화 히스토리를 입력값으로 AI 서버에 전달, 다음 질문 또는 대화 완료 응답 수신
    - `generateDiaryContent`: 전체 대화 내용을 입력값으로 AI 서버에 전달, 생성된 일기 + 감정 태그 추천 응답 수신
    - AI 서버 엔드포인트: POST /api/diary/first-question, /next-question, /generate
    - _요구사항: 3.1 (AI 일기 확장)_

  - [ ]* 3.6 Property 7, 8, 9, 10 속성 테스트: 일기 관련
    - **Property 7: 개인 기록 접근 제어**
    - **Property 8: 감정 추이 데이터 정확성**
    - **Property 9: 활동별 경험치 부여**
    - **Property 10: 연속 일기 작성 보너스**
    - **검증 대상: 요구사항 3.2, 3.3, 3.4, 3.5, 9.4, 10.1**

- [ ] 4. 체크포인트 - 기본 서비스 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [ ] 5. 매칭 서비스 구현
  - [x] 5.1 슬롯 관리 구현
    - `getSlots`: 사용자별 슬롯 목록 조회 (Spring Data JPA)
    - `unlockSlot`: 새 슬롯 해금 (레벨업 보상 연동)
    - `updateSlotAttributes`: 슬롯 속성(취미, 관심사, 이상형) 수정
    - `updateMatchPreferences`: 슬롯별 매칭 범위 설정(선호 나이 범위, 선호 성별) 수정
    - _요구사항: 4.8, 4.9, 4.16_

  - [x] 5.2 매칭 점수 계산 엔진 구현 (MVP: 단순 동선 겹침)
    - `calculateRouteOverlap`: 시간표 기반 동선 시간대 겹침 계산 (AI 미사용). 같은 시간대에 같은 건물/인접 건물에 있는 횟수로 점수 산출
    - 차단 목록 필터링 로직
    - 프로필 속성(취미, 관심사, 이상형)과 매칭 범위(나이, 성별)는 데이터 수집만 하고 매칭 점수에 반영하지 않음 (추후 확장 예정)
    - _요구사항: 4.9, 4.10, 11.3_

  - [ ]* 5.3 Property 26 속성 테스트: 차단 필터
    - **Property 26: 차단된 사용자 매칭 방지**
    - **검증 대상: 요구사항 11.3**

  - [x] 5.4 배치 매칭 실행 로직 구현
    - `executeBatchMatching`: 월요일 자정에만 배치 매칭 실행 (Spring @Scheduled, cron = "0 0 0 * * MON")
    - 빈 슬롯 필터링
    - 두 사용자의 시간표에서 출발 건물과 도착 건물이 같은 동선이 있는지 비교
    - 겹치는 동선이 여러 개면 그 중 하나를 선택하여 매칭 성사
    - 선택된 동선 정보를 기반으로 4단계 미션 데이터를 사전 생성 (MissionEntity에 장소/활동 저장)
    - 매칭 주기(cycle_start_date = 월요일, cycle_end_date = 같은 주 금요일, 5일간) 설정
    - 중간에 매칭이 일찍 끝나도 다음 월요일까지 재매칭하지 않음
    - _요구사항: 4.1, 4.2, 4.3, 4.6_

  - [ ]* 5.5 Property 11, 12 속성 테스트: 배치 매칭 규칙
    - **Property 11: 배치 매칭 - 빈 슬롯 매칭 규칙**
    - **Property 12: 매칭 주기 불변식**
    - **검증 대상: 요구사항 4.2, 4.3, 4.6**

  - [ ] 5.6 빠른 매칭 요청 구현 [MVP 후순위]
    - `requestQuickMatch`: 슬롯에 빠른 매칭 플래그 설정
    - 빠른 매칭 성사 시 4단계 즉시 해금 (InteractionService 연동)
    - 빠른 매칭 시 건너뛴 단계(1~3) 경험치 미부여 로직
    - _요구사항: 13.1, 13.3, 13.5, 13.6, 13.7_

  - [ ]* 5.7 Property 31, 32 속성 테스트: 빠른 매칭 [MVP 후순위]
    - **Property 31: 빠른 매칭 시 4단계 즉시 해금**
    - **Property 32: 빠른 매칭 시 건너뛴 단계 경험치 미부여**
    - **검증 대상: 요구사항 13.3, 13.6**

- [ ] 6. 체크포인트 - 매칭 엔진 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [x] 7. 단계별 상호작용 서비스 구현 (1~2단계)
  - [x] 7.1 InteractionService 핵심 로직 구현
    - `getInteractionState`: 현재 상호작용 상태 조회
    - `respondToStageAdvance`: 단계 진행 동의/거부 처리
    - `terminateMatch`: 매칭 종료 (거부, 기한 만료, 신고 등)
    - 단계 전환 규칙 구현 (양쪽 완료 조건 충족 시 다음 단계 해금)
    - _요구사항: 5.4, 6.3, 6.4, 7.3, 8.3_

  - [ ]* 7.2 Property 17, 19 속성 테스트: 단계 전환 및 거부
    - **Property 17: 단계 전환 규칙**
    - **Property 19: 거부 시 매칭 종료**
    - **검증 대상: 요구사항 5.4, 6.3, 6.4, 7.3, 8.3**

  - [x] 7.3 퀴즈 단계 (1단계) 구현
    - AI 기반 상대방 프로필 퀴즈 생성 (AIService.generateQuiz 연동, 최소 5문항)
    - 퀴즈 완료 시 정답률 및 상대방 요약 정보 제공
    - 양쪽 퀴즈 완료 시 2단계 해금
    - _요구사항: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 7.4 Property 16 속성 테스트: 퀴즈 불변식
    - **Property 16: 퀴즈 불변식**
    - **검증 대상: 요구사항 5.2, 5.3**

  - [x] 7.5 힌트 질문 기능 구현
    - `sendHintQuestion`: 퀴즈 단계에서만 힌트 질문 전송 가능
    - `answerHintQuestion`: 힌트 질문 답변 처리
    - `getHintQuestions`: 힌트 질문/답변 목록 조회
    - 질문 전송 시 상대방 알림, 답변 시 질문자 알림 (NotificationService 연동)
    - 비동기 처리 (실시간 채팅 아닌 질문-답변 형태)
    - _요구사항: 5.5, 5.6, 5.7, 5.8_

  - [ ]* 7.6 Property 33, 34 속성 테스트: 힌트 질문
    - **Property 33: 힌트 질문 비동기 처리**
    - **Property 34: 힌트 질문은 퀴즈 단계에서만 가능**
    - **검증 대상: 요구사항 5.5, 5.6, 5.7, 5.8**

  - [x] 7.7 채팅 단계 (2단계) 구현
    - `startChatSession`: 30분 제한 채팅 세션 생성
    - `sendMessage`: 메시지 전송 (Spring WebSocket + Redis 기반 실시간 처리)
    - `getIcebreakerQuestion`: 아이스브레이킹 질문 제안
    - `getMessages`: 채팅 이력 조회
    - 제한 시간 종료 시 자동 세션 종료 및 3단계 해금 확인
    - _요구사항: 6.1, 6.2, 6.3, 6.4_

  - [ ]* 7.8 Property 18 속성 테스트: 채팅 시간 제한
    - **Property 18: 채팅 세션 시간 제한**
    - **검증 대상: 요구사항 6.1**

- [ ] 8. 단계별 상호작용 서비스 구현 (3~5단계)
  - [ ] 8.1 게임 단계 (3단계) 구현
    - `getAvailableGames`: 최소 3가지 게임 목록 제공
    - `createGameSession`: 게임 세션 생성 (Spring Data Redis 기반 상태 관리)
    - `processGameAction`: 게임 액션 처리
    - `completeGame`: 게임 완료 시 양쪽 친밀도 점수 부여
    - 게임 완료 후 4단계 해금 확인
    - _요구사항: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 8.2 Property 20 속성 테스트: 게임 친밀도 부여
    - **Property 20: 게임 완료 시 친밀도 부여**
    - **검증 대상: 요구사항 7.2**

  - [ ] 8.3 미션 단계 (4단계) 구현
    - `generateMission`: RAG 파이프라인으로 미션 생성. CampusVectorStoreService에서 동선 겹침 장소 기반 유사도 검색 후, 검색 결과를 Bedrock Claude에 주입하여 자연스러운 미션 생성
    - `confirmMission`: 양쪽 미션 수행 확인 시 5단계 해금
    - `extendMissionDeadline`: 미션 기한 1회 연장 (이미 연장된 경우 거부)
    - 미션 기한 만료 처리 (연장 미사용 시 연장 옵션, 연장 후 만료 시 매칭 종료)
    - _요구사항: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 8.4 Property 21, 22 속성 테스트: 미션 관련
    - **Property 21: 미션 생성 불변식**
    - **Property 22: 미션 연장 1회 제한**
    - **검증 대상: 요구사항 8.1, 8.2, 8.4, 13.5**

  - [ ] 8.5 회고 단계 (5단계) 구현
    - `selectReviewMode`: AI 기반 / 직접 작성 모드 선택
    - AI 기반 모드: `getAIQuestions` (AIService.generateReviewQuestions 연동) → `answerAIQuestion` → `generateReview` (AIService.generateReviewContent 연동) → `editGeneratedReview`
    - 직접 작성 모드: `submitDirectReview` (만족도 1~5, 느낀 점, 재만남 의사 필수)
    - `getReview`: 회고 조회 (본인만 열람 가능)
    - 회고 완료 시 양쪽 경험치 부여
    - _요구사항: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

  - [ ]* 8.6 Property 23, 35, 36 속성 테스트: 회고 관련
    - **Property 23: 회고 필수 필드 불변식**
    - **Property 35: AI 회고 글 생성 라운드트립**
    - **Property 36: 회고 작성 모드 선택 불변식**
    - **검증 대상: 요구사항 9.1, 9.2, 9.3, 9.4, 9.5, 9.6**

- [ ] 9. 체크포인트 - 상호작용 흐름 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [ ] 10. 경험치 및 성장 시스템 구현
  - [ ] 10.1 ExperienceService 구현
    - `grantExperience`: 활동별 경험치 부여 (퀴즈, 채팅, 게임, 미션, 회고. 일기/플래너는 MVP 후순위)
    - `getExperienceInfo`: 누적 경험치, 현재 레벨, 다음 레벨까지 필요 경험치 조회
    - `getExpHistory`: 경험치 획득 내역 조회 (Spring Data JPA Pageable)
    - `checkAndProcessLevelUp`: 레벨업 조건 확인 및 보상 처리 (슬롯 해금 등)
    - 레벨업 시 알림 전송 (NotificationService 연동)
    - _요구사항: 10.1, 10.2, 10.3, 10.4_

  - [ ]* 10.2 Property 9, 13, 24 속성 테스트: 경험치 시스템
    - **Property 9: 활동별 경험치 부여**
    - **Property 13: 경험치 기반 슬롯 해금**
    - **Property 24: 경험치 조회 라운드트립**
    - **검증 대상: 요구사항 10.1, 10.2, 10.3, 10.4**

- [ ] 11. 안전 및 신고 서비스 구현
  - [ ] 11.1 SafetyService 구현
    - `reportUser`: 신고 접수 (자기 자신 신고 방지, 매칭 즉시 중단)
    - `blockUser`: 사용자 차단 (멱등 처리)
    - `getBlockedUsers`: 차단 목록 조회
    - `checkAndSuspend`: 신고 3회 이상 시 자동 계정 정지
    - _요구사항: 11.1, 11.2, 11.3, 11.4_

  - [ ]* 11.2 Property 25, 27 속성 테스트: 안전 관련
    - **Property 25: 신고 시 매칭 즉시 중단**
    - **Property 27: 신고 3회 이상 시 계정 정지**
    - **검증 대상: 요구사항 11.2, 11.4**

- [ ] 12. 알림 및 리마인더 서비스 구현
  - [ ] 12.1 NotificationService 구현
    - `sendNotification`: Spring AMQP(Amazon MQ/SQS) 기반 비동기 알림 전송 (매칭 성사, 단계 완료, 레벨업, 힌트 질문/답변 등)
    - `getNotificationSettings` / `updateNotificationSettings`: 알림 종류별 수신 설정 관리
    - `scheduleReminder`: 미션 기한 24시간 전 리마인더, 플래너 미작성 리마인더 스케줄링
    - _요구사항: 12.1, 12.2, 12.3, 12.4_

  - [ ]* 12.2 Property 28, 29 속성 테스트: 알림 관련
    - **Property 28: 알림 설정 라운드트립**
    - **Property 29: 미션 기한 임박 리마인더**
    - **검증 대상: 요구사항 12.3, 12.4**

- [ ] 13. 체크포인트 - 보조 서비스 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [x] 14. 캠퍼스 공간 데이터 서비스 구현
  - [x] 14.1 CampusDataService 구현
    - `createBuilding`, `updateBuilding`, `getBuildings`, `getBuildingById`: 건물 CRUD (Spring Data JPA)
    - `createPath`, `updatePath`, `getPathBetween`, `getAllPaths`: 경로 CRUD
    - `createVenue`, `updateVenue`, `getVenues`, `getVenueById`: 거점(만남 장소) CRUD
    - `getCampusContext`: AI 서비스에서 사용할 캠퍼스 전체 컨텍스트 조회
    - 관리자 권한 검증 로직 (Spring Security @PreAuthorize)
    - _요구사항: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [x] 14.2 캠퍼스 시드 데이터 작성
    - Flyway 시드 마이그레이션 또는 ApplicationRunner로 테스트용 캠퍼스 건물 데이터 (최소 10개 건물)
    - 건물 간 경로 데이터 (주요 이동 경로)
    - 주요 거점 데이터 (카페, 매점, 벤치, 광장 등)
    - 운영 시간 데이터
    - _요구사항: 14.1, 14.2, 14.3, 14.4_

  - [ ]* 14.3 Property 39 속성 테스트: 캠퍼스 공간 데이터 CRUD 라운드트립
    - **Property 39: 캠퍼스 공간 데이터 CRUD 라운드트립**
    - jqwik로 임의의 건물/경로/거점 데이터에 대해 등록 후 조회 시 동일 데이터 반환 검증
    - **검증 대상: 요구사항 14.1, 14.2, 14.3, 14.4, 14.5**

- [ ] 15. AI 서비스 클라이언트 구현 (별도 AI 서버 연동)
  - [ ] 15.1 AIServiceClient 기본 구조 구현
    - AIServiceClient 인터페이스 정의 (비동기 SQS + 동기 HTTP 통합)
    - SQS 발행 클라이언트 구현 (퀴즈/미션 요청큐 발행)
    - SQS 응답큐 리스너 구현 (퀴즈/미션 생성 결과 수신 → DB 저장)
    - HTTP 클라이언트 구현 (RestTemplate/WebClient 기반, 일기/회고용)
    - 에러 핸들링: HTTP 타임아웃 시 Spring Retry 최대 3회 재시도
    - 응답 파싱 및 검증 유틸리티 (Jackson ObjectMapper)
    - _요구사항: 5.1, 8.1, 9.2, 9.3_

  - [ ] 15.2 AI 동선 추론 구현 [MVP 후순위]
    - `inferRoute`: 시간표/플래너 + 캠퍼스 공간 데이터 기반 이동 경로 추론
    - MVP에서는 시간표 기반 단순 동선 계산으로 대체. 출시 후 데이터 축적 시 AI 기반으로 확장
    - _요구사항: 2.6_

  - [ ] 15.3 AI 매칭 점수 계산 구현 [MVP 후순위]
    - `calculateRouteMatchScore`: 두 사용자의 추론된 동선이 자연스럽게 겹치는 정도 판단
    - MVP에서는 시간대 겹침 기반 단순 점수로 대체. 출시 후 데이터 축적 시 AI 기반으로 확장
    - _요구사항: 4.10_

  - [ ] 15.4 RAG 기반 미션 생성 연동 구현
    - MissionGenerationInput DTO 정의 (동선 교집합 + 사용자 프로필)
    - 매칭 성사 시 SQS 미션 요청큐에 MissionGenerationInput 발행
    - SQS 응답큐 리스너에서 생성된 미션 결과 수신 → MissionEntity DB 저장
    - AI 서버 측 RAG 파이프라인 (OpenSearch 검색 + Bedrock 생성)은 AI 서버에서 구현
    - _요구사항: 8.1, 14.6_

  - [ ] 15.5 퀴즈 사전 생성 연동 구현
    - 퀴즈 생성 시점을 클라이언트 요청 → 매칭 성사 시점으로 변경
    - 매칭 성사 시 SQS 퀴즈 요청큐에 QuizGenerationInput (상대방 프로필) 발행
    - SQS 응답큐 리스너에서 생성된 퀴즈 결과 수신 → DB 저장
    - 클라이언트 퀴즈 조회 API: DB에서 즉시 반환 (대기 없음)
    - _요구사항: 5.1_

  - [ ] 15.6 AI 일기 대화 HTTP 클라이언트 구현
    - AI 서버 엔드포인트 호출: POST /api/diary/first-question, /next-question, /generate
    - DiarySessionService에서 AIServiceClient.generateDiaryFirstQuestion/NextQuestion/Content 호출
    - 입력값: 플래너 데이터 + 대화 히스토리, 출력값: 질문 또는 생성된 일기
    - _요구사항: 3.1 (AI 일기 확장)_

  - [ ] 15.7 AI 회고 대화 HTTP 클라이언트 구현
    - AI 서버 엔드포인트 호출: POST /api/review/questions, /generate
    - ReviewService에서 AIServiceClient.generateReviewQuestions/Content 호출
    - 입력값: 만남 컨텍스트 + 답변, 출력값: 질문 목록 또는 생성된 회고 글
    - _요구사항: 9.2, 9.3_

  - [ ]* 15.8 Property 37, 38, 40 속성 테스트: AI 서비스 관련
    - **Property 37: AI 동선 추론 - 캠퍼스 공간 데이터 활용**
    - **Property 38: AI 미션 생성 - 운영시간 준수**
    - **Property 40: AI 퀴즈 생성 - 프로필 기반 관련성**
    - AI 서버 HTTP/SQS 응답을 Mockito로 모킹하여 테스트
    - **검증 대상: 요구사항 2.6, 5.1, 8.1, 14.6**

- [ ] 16. 체크포인트 - AI/캠퍼스 서비스 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [ ] 17. 서비스 간 통합 및 배치 스케줄러 연결
  - [ ] 17.1 배치 매칭 스케줄러 구현
    - Spring @Scheduled(cron = "0 0 0 * * MON") 월요일 자정 실행 크론 작업 설정
    - MatchingService.executeBatchMatching 호출
    - 배치 실행 결과 로깅 (SLF4J)
    - _요구사항: 4.1_

  - [ ] 17.2 리마인더 스케줄러 구현
    - 미션 기한 24시간 전 리마인더 자동 스케줄링
    - _요구사항: 12.4_

  - [ ] 17.3 서비스 간 이벤트 연동 통합
    - Spring ApplicationEvent 또는 Spring AMQP를 활용한 이벤트 기반 연동
    - 매칭 성사 → 상호작용 생성 → AI 퀴즈 생성 흐름 연결
    - 신고 접수 → 매칭 종료 → 차단 처리 흐름 연결
    - 회고 완료 → 경험치 부여 → 매칭 완료 처리 흐름 연결
    - 시간표 기반 동선 겹침 계산 → 배치 매칭 흐름 연결
    - _요구사항: 전체_

  - [ ]* 17.4 통합 테스트 작성
    - Spring Boot Test + Testcontainers (MySQL, Redis) 기반 통합 테스트
    - 배치 매칭 전체 흐름 (시간표 기반 동선 겹침 → 매칭 → 알림 전송)
    - 매칭 주기 연장 흐름 (연장 요청 → 상대방 알림 → 동의/거부 → 주기 연장 또는 유지)
    - 단계별 상호작용 전체 흐름 (1단계 → 5단계)
    - 신고 → 매칭 중단 → 차단 → 재매칭 방지 흐름
    - 경험치 부여 → 레벨업 → 슬롯 해금 흐름
    - 캠퍼스 공간 데이터 CRUD → 미션 장소 조회 흐름
    - _요구사항: 전체_

- [ ] 18. 최종 체크포인트 - 전체 시스템 검증
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

## 참고 사항

- `*` 표시된 태스크는 선택 사항이며, 빠른 MVP를 위해 건너뛸 수 있습니다
- 각 태스크는 특정 요구사항을 참조하여 추적 가능합니다
- 체크포인트에서 점진적 검증을 수행합니다
- 속성 기반 테스트(jqwik)는 보편적 정확성 속성을 검증합니다 (`@Property(tries = 100)`)
- 단위 테스트(JUnit 5 + Mockito)는 구체적 예시와 에지 케이스를 검증합니다
- 통합 테스트(Spring Boot Test + Testcontainers)는 서비스 간 흐름을 검증합니다
- AI 서비스는 AWS SDK for Java v2의 BedrockRuntimeClient를 사용하며, 캠퍼스 데이터는 MySQL에서 조회하여 프롬프트 컨텍스트로 전달합니다
