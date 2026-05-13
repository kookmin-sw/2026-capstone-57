# 구현 계획: 오프라인 미션 생성

## 개요

ChromaDB 벡터 검색과 Bedrock Claude를 활용하여 매칭된 두 사용자에게 오프라인 미션을 생성하는 기능을 구현한다. 기존 VectorStore, EmbeddingGenerator, SQS 인프라를 재사용하며, 모델 정의 → 검색 → 프롬프트/파서 → 핸들러 → 통합 순으로 구현한다.

## Tasks

- [ ] 1. 미션 모델 및 설정 추가
  - [x] 1.1 미션 Pydantic 모델 정의
    - `app/features/mission/__init__.py` 생성
    - `app/features/mission/models.py`에 MissionRequestMessage, MissionResponseMessage, IntersectionInfo, UserProfile, Mission, VenueResult 모델 정의
    - action: GENERATE_MISSION (요청) / MISSION_GENERATED (응답)
    - status: SUCCESS | FALLBACK | FAILED
    - _Requirements: 1.3, 1.4, 6.1, 6.2_

  - [x] 1.2 설정 및 VectorStore 확장
    - `app/config.py`에 `sqs_mission_request_queue`, `sqs_mission_response_queue`, `mission_search_top_k` 추가
    - `app/rag/vector_store.py`에 `COLLECTION_VENUES = "venues"` 추가 및 VALID_COLLECTIONS 업데이트
    - `.env`에 미션 SQS 큐 URL 추가
    - _Requirements: 1.1, 1.2, 2.2_

- [ ] 2. 장소 검색 모듈 구현
  - [x] 2.1 미션 검색 클래스 구현
    - `app/features/mission/search.py`에 MissionSearch 클래스 구현
    - `build_search_query`: 동선 교집합 정보 + 사용자 프로필 → 자연어 검색 쿼리 변환
    - `search_venues`: 쿼리 임베딩 생성 → ChromaDB 벡터 검색 (상위 5개) → VenueResult 리스트 반환
    - metadata 필터 구성 (building_id 기반)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. 미션 프롬프트 및 파서 구현
  - [x] 3.1 미션 프롬프트 빌더 구현
    - `app/features/mission/prompt.py`에 build_mission_prompt 함수 구현
    - 검색된 상위 5개 장소 정보 + 사용자 컨텍스트(프로필, 동선) 포함
    - 미션 JSON 출력 형식 지정 (placeName, activity, recommendedTime, description)
    - 대학생 맥락에 맞는 자연스러운 미션 생성 지침 포함
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6_

  - [x] 3.2 미션 응답 파서 및 폴백 구현
    - `app/features/mission/parser.py`에 parse_mission_response, generate_fallback_mission 함수 구현
    - Bedrock JSON 응답 파싱 → Mission 객체 변환
    - 폴백 미션: 검색 결과 상위 1개 장소 기반 기본 미션 생성
    - ChromaDB 실패 시 동선 정보만으로 폴백 미션 생성
    - _Requirements: 4.3, 5.1, 5.2, 5.3_

- [x] 4. 미션 핸들러 구현
  - [x] 4.1 미션 생성 핸들러 구현
    - `app/features/mission/handler.py`에 MissionHandler 클래스 구현
    - 전체 흐름: 요청 파싱 → ChromaDB 검색 → 프롬프트 증강 → Bedrock 호출 → 응답 파싱 → SQS 발행
    - Bedrock 실패 시 1회 재시도, 재시도 실패 시 폴백 미션 사용
    - ChromaDB 실패 시 동선 정보 기반 폴백
    - 요청당 타임아웃 적용
    - _Requirements: 4.1, 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 5. FastAPI 앱 통합
  - [x] 5.1 main.py에 미션 기능 통합
    - MissionSearch 초기화 (기존 embedding_generator, vector_store 재사용)
    - MissionHandler 생성 및 MessageRouter 등록
    - SQS Poller에 미션 요청 큐 등록
    - _Requirements: 1.1, 1.2_

- [x] 6. 장소 데이터 시딩 및 E2E 테스트
  - [x] 6.1 캠퍼스 장소 데이터 시딩 스크립트 작성
    - `scripts/seed_venues.py` 작성
    - 테스트용 캠퍼스 장소 데이터 (10~20개) 정의
    - Titan Embeddings로 벡터화 → ChromaDB venues 컬렉션에 저장
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 6.2 미션 생성 E2E 테스트 스크립트 작성
    - `test_mission_e2e.py` 작성
    - SQS 전송 → 핸들러 처리 (ChromaDB 검색 → Bedrock 호출) → 응답 확인
    - _Requirements: 1.1, 1.2, 4.1_

## Notes

- 기존 VectorStore, EmbeddingGenerator, SQS Poller/Publisher, Bedrock Client를 모두 재사용
- ChromaDB `venues` 컬렉션은 앱 시작 시 자동 생성됨 (VectorStore._initialize_collections)
- 장소 데이터 시딩은 최초 1회 실행 필요 (이후 관리자 API로 추가/수정 가능)
- `from __future__ import annotations`를 모든 새 파일에 추가 (Python 3.9 호환)
- OpenSearch Serverless 권한 문제 해결 시 나중에 교체 가능 (검색 인터페이스만 변경)
