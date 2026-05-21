[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/Lvs6kcL8)

# 🌤️ 일기예보 (Ilgi-yebo)
**"일기로 예견하는 보석같은 만남"**  
일기예보는 사람을 찾아주는 서비스가 아니라, 오늘 하루에 기록될 새로운 만남을 예보하는 서비스입니다.<br>
어제의 기록과 오늘의 동선을 바탕으로, 사용자의 일상에 짧고 재밌는 사건을 만들어냅니다. <br>
그래서 만남은 부담스러운 약속이 아니라, 오늘의 일기에 남길 수 있는 가벼운 장면이 됩니다.

🔗 **Quick Links:** [🏠 팀 페이지](https://kookmin-sw.github.io/2026-capstone-57/) | [🌐 서비스 바로가기](https://ilgiyebo.vercel.app) | [💻 팀 GitHub](https://github.com/kookmin-sw/2026-capstone-57)

> 국민대학교 2026 캡스톤 디자인 프로젝트 · 57팀

<br>

## 1. 프로젝트 소개
**일기예보**는 단순한 외모 기반의 데이팅 앱이 아닙니다.  
사용자의 수업 시간표와 교내 이동 동선(캠퍼스 공간 데이터)의 교집합을 분석하여 자연스러운 만남의 기회를 제공합니다.

사용자는 매일의 일기와 플래너를 작성하며 스스로를 발전시키고('나') 매칭된 상대와 <b>5단계 상호작용(퀴즈 → 채팅 → 협동 게임 → 미션 → 회고)</b>을 거치며 안전하고 점진적으로 관계를 형성('너'와 '우리')해 나갈 수 있습니다.

### ✨ 핵심 포인트
* **동선 기반 배치 매칭:** 매주 월요일 자정, 시간표 기반 동선 교집합 분석으로 최적의 상대를 자동 배정합니다.
* **AI 콘텐츠 생성:** AWS Bedrock 기반 AI가 사용자 프로필 기반 퀴즈, RAG 기반 오프라인 미션 장소 추천, 만남 후 회고 글을 자동 생성합니다.
* **안전한 5단계 상호작용:** 블라인드 퀴즈(150자 채팅 → 협동 게임 → 오프라인 미션 → 회고)로 단계적으로 관계를 형성하며, 각 단계는 상호 동의 시에만 진행됩니다.
* **성장형 소셜 라이프:** 일기 및 플래너 작성, 긍정적인 상호작용을 통해 경험치를 얻고 레벨업 및 매칭 슬롯을 해금하는 게이미피케이션(Gamification) 요소를 적용했습니다.

---

## 2. 소개 영상

[![일기예보 시연영상](https://img.youtube.com/vi/zxztzmkYxnc/maxresdefault.jpg)](https://www.youtube.com/watch?v=zxztzmkYxnc)

---

## 3. 팀 소개
| 이름 | 역할 및 담당 |
| :--- | :--- |
| **김아리** | **[매칭 & 사용자 인증]**<br>- 대학교 이메일 파싱 및 JWT 기반 로그인/보안 시스템 구현<br>- Spring Batch를 활용한 자정 배치 매칭 스케줄러 개발<br>- 빈 슬롯 관리 및 시간표 기반 동선 겹침 매칭 알고리즘 설계 |
| **선현승** | **[실시간 상호작용 (게임)]**<br>- Spring WebSocket 및 Redis를 활용한 3단계 협동 게임 세션 구현<br>- 실시간 게임 상태 동기화 및 완료 시 친밀도 점수 부여 로직<br>- 매칭된 사용자 간의 상호작용 상태(FSM) 전환 파이프라인 관리 |
| **정영미** | **[일상 기록 & 퀴즈 도메인]**<br>- 플래너(일정/동선) 및 일기(감정 태그) 도메인 핵심 비즈니스 로직 구현<br>- 1단계 상호작용(퀴즈) 비동기 힌트 질문(AWS SQS 연동) 처리<br>- 활동별 경험치 획득, 레벨업 및 슬롯 해금 성장 시스템 구축 |
| **황찬우** | **[AI & 공간 데이터]**<br>- AWS Bedrock(LLM) 연동 및 맞춤형 퀴즈/미션/회고 생성 프롬프트 엔지니어링<br>- ChromaDB 벡터 검색과 동선 교집합을 결합한 RAG 파이프라인으로 최적 미션 장소 추천 로직 설계 |

---

## 4. 시스템 아키텍처
일기예보는 **도메인 중심 모놀리식 아키텍처**로 구축되었으며, 추후 트래픽 증가에 따라 서비스 분리를 고려한 도메인 중심 설계(DDD)를 채택하였습니다.

> **아키텍처 변경 배경:** 초기에는 마이크로서비스 아키텍처(MSA)를 목표로 했으나, 복잡한 5단계 상호작용 로직의 데이터 정합성 확보 및 개발 생산성을 위해 단일 Spring Boot 애플리케이션 구조를 채택하였습니다. 도메인 간 의존성은 단방향으로 강제하여 향후 분리가 가능한 구조를 유지합니다.

### 🏗️ 서비스 구조
* **Client:** React Native 기반 모바일 애플리케이션이 사용자 인터페이스를 담당합니다.
* **Backend Services (Spring Boot):** AWS 환경에서 단일 컨테이너로 배포되며, 도메인별 패키지로 격리된 모놀리식 구조로 운영합니다.
    * **인증/사용자:** JWT 기반 보안 및 대학 이메일 도메인 검증 처리
    * **매칭 엔진:** 매주 월요일 자정 Spring Batch 기반 시간표 동선 겹침 매칭
    * **상호작용:** 채팅(STOMP over SockJS), Phaser 3 기반 협동 게임, 미션/회고 FSM 상태 관리
    * **AI 서비스:** AWS SDK를 통해 Bedrock과 통신하며, 퀴즈 사전 생성·RAG 기반 미션 추천·회고 글 자동 생성 수행
* **Data Layer:**
    * **MySQL (Aurora):** 메인 비즈니스 데이터 저장 (22개 테이블, DDD 기반 5개 Bounded Context)
    * **Redis:** 실시간 채팅 세션 및 게임 상태 캐싱
    * **ChromaDB:** 캠퍼스 공간 데이터 벡터 인덱스 (RAG 미션 추천용)
    * **Amazon SQS:** 퀴즈 힌트 발송 및 AI 요청 비동기 처리
* **External AI:** Amazon Bedrock(LLM) + Amazon Titan Embeddings 활용

---

## 5. 주요 기능

| 기능 | 상태 | 설명 |
| :--- | :---: | :--- |
| 사용자 등록 및 인증 | ✅ 완료 | 대학 이메일 도메인 검증 및 파싱, JWT 기반 인증 |
| 시간표 및 동선 관리 | ✅ 완료 | 에브리타임 시간표 공유 URL 파싱 기반 시간표 등록 및 캠퍼스 이동 경로 데이터화 |
| 배치 매칭 엔진 | ✅ 완료 | 매주 월요일 자정, 동선 겹침 분석 기반 1:1 자동 매칭 |
| 5단계 점진적 상호작용 | ✅ 완료 | 퀴즈 → 채팅(150자 한도) → 협동 게임 → 오프라인 미션 → 회고 FSM 구현 |
| AI 퀴즈 생성 | ✅ 완료 | 회원가입 시 프로필 기반 4지선다 퀴즈 5문항을 Bedrock이 사전 생성, SQS로 비동기 영속화 |
| 실시간 채팅 | ✅ 완료 | STOMP over SockJS 기반, 150자 토큰 한도 도달 시 자동 종료 |
| 실시간 협동 게임 | ✅ 완료 | Phaser 3 기반 2인 협동 퍼즐, 20Hz 위치 동기화 및 낙관적 업데이트 |
| RAG 기반 미션 생성 | ✅ 완료 | 동선 교집합 + ChromaDB 벡터 검색으로 최적 오프라인 미션 장소 추천 |
| AI 회고 작성 | ✅ 완료 | 미션 컨텍스트 기반 AI 회고 질문 생성, AI 대화 모드 또는 직접 작성 모드 지원 |
| AI 기반 일기 작성 | ✅ 완료 | 당일 일정 분석 기반 AI 대화로 일기 본문 자동 생성 (최대 5턴) |
| 경험치 및 안전 시스템 | ✅ 완료 | 활동별 EXP 부여, 레벨업·슬롯 해금, 신고 3회 누적 시 자동 계정 정지 |
| 빠른 매칭 (1~3단계 스킵) | 🔜 차기 버전 | 사용자 피드백 수집 후 구현 예정 |

---

## 6. 사용법 (Getting Started)

### ⚙️ 시스템 요구사항
* **Java 17** 이상
* **MySQL 8.0** 이상 (JSON 타입 지원 필수)
* **Redis**
* **AWS 자격 증명** (Amazon Bedrock, SQS 사용 권한 필요)

### 💻 설치 및 실행 방법

1. **Repository 클론**
```bash
git clone https://github.com/kookmin-sw/2026-capstone-57.git
cd 2026-capstone-57/backend
```

2. **환경 변수 설정 (`.env` 또는 `application.yml`)**
프로젝트 실행을 위해 아래 환경 변수 설정이 필요합니다.
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ilgiyebo
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
JWT_SECRET=your_super_secret_jwt_key
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

3. **서버 빌드 및 실행**
본 프로젝트는 **Flyway**를 사용하여 서버 실행 시 데이터베이스 스키마가 자동으로 생성 및 마이그레이션 됩니다.
```bash
./gradlew build -x test
./gradlew bootRun
```

4. **API 문서 확인**
서버 구동 후 브라우저에서 아래 주소로 접속하여 **Swagger UI**를 통해 API 명세를 확인하고 테스트할 수 있습니다.
> [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 7. 기술 스택 (Tech Stack)

### Backend
* **Framework:** Spring Boot 3.x, Spring Web, Spring Security
* **Data Access:** Spring Data JPA, Spring Data Redis
* **Batch/Async:** Spring Batch, Spring Scheduler, AWS SDK, WebSocket (STOMP over SockJS)
* **Testing:** JUnit 5, Mockito, jqwik (Property-Based Testing, 45종), Testcontainers

### Database & Cloud
* **RDBMS:** MySQL (AWS RDS Aurora 호환)
* **In-Memory/Cache:** Redis
* **Vector DB:** ChromaDB (RAG 미션 추천)
* **Message Queue:** Amazon SQS
* **Infrastructure:** AWS (S3, ECS)
* **DB Migration:** Flyway

### AI & API
* **LLM Integration:** Amazon Bedrock
* **Embeddings:** Amazon Titan Embeddings
* **AWS SDK:** AWS SDK for Java v2 (Bedrock Runtime, SQS Client)
* **API Docs:** Springdoc OpenAPI (Swagger UI)

### Frontend (Mobile)
* **Framework:** React Native
* **Game Engine:** Phaser 3 (협동 게임)
