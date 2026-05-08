[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/Lvs6kcL8)

# 🌤️ 일기예보 (Ilgi-yebo)
**"일기로 예견하는 보석같은 만남"**  

코로나19 이후 새로운 사람을 만나는 것에 부담감을 느끼는 대학생들을 위한 일상/동선 기반 소셜 매칭 서비스입니다.

🔗 **Quick Links:** [🏠 팀 페이지](https://kookmin-sw.github.io/2026-capstone-57/) | [💻 팀 GitHub](https://github.com/kookmin-sw/2026-capstone-57)

> 국민대학교 2026 캡스톤 디자인 프로젝트 · 57팀
<br>

## 1. 프로젝트 소개
**일기예보**는 단순한 외모 기반의 데이팅 앱이 아닙니다. 사용자의 수업 시간표와 교내 이동 동선(캠퍼스 공간 데이터)의 교집합을 분석하여 자연스러운 만남의 기회를 제공합니다.

사용자는 매일의 일기와 플래너를 작성하며 스스로를 발전시키고(‘나’), 매칭된 상대와 **5단계 상호작용(퀴즈 → 채팅 → 협동 게임 → 미션 → 회고)**을 거치며 안전하고 점진적으로 관계를 형성(‘너’와 ‘우리’)해 나갈 수 있습니다.

### ✨ 핵심 포인트
* **동선 기반 AI 매칭:** AWS Bedrock 기반 LLM이 사용자의 시간표와 캠퍼스 데이터를 분석하여 최적의 동선 교집합을 찾아냅니다.
* **안전한 5단계 상호작용:** 블라인드 퀴즈부터 시작해 서로 동의 하에만 다음 단계(채팅, 오프라인 미션 등)로 넘어가는 안전한 시스템을 지향합니다.
* **성장형 소셜 라이프:** 일기 및 플래너 작성, 긍정적인 상호작용을 통해 경험치를 얻고 레벨업하는 게미피케이션(Gamification) 요소를 적용했습니다.

---

## 2. 🎥 소개 영상
> 💡 **업데이트 예정:** 프로젝트 데모 및 소개 영상은 현재 제작 중이며, 추후 이곳에 추가될 예정입니다.

---

## 3. 팀 소개
| 이름 | 역할 및 담당 |
| :--- | :--- |
| **김아리** | **[Backend & Architecture]**<br>- 프로젝트 아키텍처 및 도메인(DDD) 설계<br>- Spring Batch 기반 자정 매칭 스케줄러 구현<br>- AWS Bedrock 기반 AI 미션/퀴즈 생성 프롬프트 엔지니어링 |
| **선현승** | **[Backend & DB]**<br>- 5단계 상호작용 상태 관리(FSM) 및 채팅(WebSocket) 구현<br>- MySQL(Aurora) 및 Redis 캐시 최적화<br>- jqwik 활용 속성 기반 테스트(PBT) 구축 |
| **정영미** | **[Client & UI/UX]**<br>- React Native 기반 모바일 앱 구현<br>- 일기/플래너 작성 뷰 및 감정 통계 차트 개발 |
| **황찬우** | **[Client & UI/UX]**<br>- React Native 기반 모바일 앱 구현<br>- 일기/플래너 작성 뷰 및 감정 통계 차트 개발) |

---

## 4. 시스템 아키텍처
일기예보는 대규모 트래픽과 확장을 고려하여 **마이크로서비스 지향형 아키텍처(MSA)** 사상을 일부 차용하고, AWS 클라우드 네이티브 환경에서 구축되었습니다.

### 🏗️ 서비스 구조
*   **Client:** React Native 기반 모바일 애플리케이션이 사용자 인터페이스를 담당합니다.
*   **API Gateway:** AWS ALB(Application Load Balancer)를 통해 트래픽을 각 서비스로 분산합니다.
*   **Backend Services (Spring Boot):** ECS Fargate 컨테이너 기반으로 독립적인 도메인 서비스를 운영합니다.
    *   **인증/사용자:** JWT 기반 보안 및 이메일 인증 처리
    *   **매칭 엔진:** 배치 처리를 통한 최적 상대 매칭 로직 수행
    *   **상호작용:** 채팅(WebSocket), 게임, 미션 상태 관리
    *   **AI 서비스:** AWS SDK를 통해 Bedrock과 통신하며 동선 추론 및 콘텐츠 생성
*   **Data Layer:** 
    *   **MySQL (Aurora):** 메인 비즈니스 데이터 저장
    *   **Redis:** 실시간 채팅 세션 및 게임 상태 캐싱
    *   **Amazon SQS:** 알림 및 비동기 작업 메시지 큐
*   **External AI:** Amazon Bedrock을 활용한 LLM 기반 맞춤형 서비스 제공

---

## 5. 🚀 사용법 (Getting Started)

### ⚙️ 시스템 요구사항
* **Java 17** 이상
* **MySQL 8.0** 이상 (JSON 타입 지원 필수)
* **Redis**
* **AWS 자격 증명** (Amazon Bedrock, SQS 사용 권한 필요)

### 💻 설치 및 실행 방법

1. **Repository 클론**
```bash
git clone [https://github.com/kookmin-sw/capstone-2026-57.git](https://github.com/kookmin-sw/capstone-2026-57.git)
cd capstone-2026-57/backend
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

## 6. 🛠 기술 스택 (Tech Stack)

### Backend
* **Framework:** Spring Boot 3.x, Spring Web, Spring Security
* **Data Access:** Spring Data JPA, Spring Data Redis
* **Batch/Async:** Spring Batch, Spring Scheduler, Spring AMQP, WebSocket
* **Testing:** JUnit 5, Mockito, jqwik (Property-Based Testing), Testcontainers

### Database & Cloud
* **RDBMS:** MySQL (AWS RDS Aurora 호환)
* **In-Memory/Cache:** Redis (Amazon ElastiCache)
* **Message Queue:** Amazon SQS / MQ
* **Infrastructure:** AWS ECS Fargate, ALB, S3
* **DB Migration:** Flyway

### AI & API
* **LLM Integration:** Amazon Bedrock (AWS SDK for Java v2)
* **API Docs:** Springdoc OpenAPI (Swagger UI)
