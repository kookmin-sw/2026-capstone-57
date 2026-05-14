"""캠퍼스 장소 데이터 시딩 스크립트.

테스트용 캠퍼스 장소 데이터를 Titan Embeddings로 벡터화하여
ChromaDB venues 컬렉션에 저장한다.

실행 방법 (프로젝트 루트에서):
    python -m scripts.seed_venues

또는:
    python scripts/seed_venues.py

Requirements: 2.1, 2.2, 2.3, 2.4
"""

from __future__ import annotations

import asyncio
import sys
from pathlib import Path

# 프로젝트 루트를 sys.path에 추가
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.config import get_settings
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import COLLECTION_VENUES, VectorStore

# 국민대학교 캠퍼스 장소 데이터 (20개)
# embeddingText 형식: "장소명 | 설명 | 유형 | 만남적합도 | 운영시간 | 추천활동"
VENUE_DATA = [
    {
        "id": "venue-001",
        "embedding_text": "복지관 학식 | 지하 1층에 위치한 학생식당으로 함께 밥 먹으며 대화하기 좋은 공간 | 식당 | 만남적합도:4 | 월-금 11:00-14:00, 17:00-19:00 | 식사, 대화",
        "metadata": {
            "type": "restaurant",
            "meeting_suitability": 4,
            "building_id": "building-welfare",
            "operating_hours": "월-금 11:00-14:00, 17:00-19:00",
            "floor": -1,
            "place_name": "복지관 학식",
        },
    },
    {
        "id": "venue-002",
        "embedding_text": "복지관 카페 | 지하 1층에 위치한 카페로 커피 마시며 가볍게 대화하기 좋은 공간 | 카페 | 만남적합도:5 | 월-금 08:00-20:00 | 커피, 대화, 휴식",
        "metadata": {
            "type": "cafe",
            "meeting_suitability": 5,
            "building_id": "building-welfare",
            "operating_hours": "월-금 08:00-20:00",
            "floor": -1,
            "place_name": "복지관 카페",
        },
    },
    {
        "id": "venue-003",
        "embedding_text": "복지관 K-BOB | 지하 1층에 위치한 간편식 매장으로 간단히 먹으며 만나기 좋은 곳 | 식당 | 만남적합도:4 | 월-금 10:00-19:00 | 식사, 간식, 대화",
        "metadata": {
            "type": "restaurant",
            "meeting_suitability": 4,
            "building_id": "building-welfare",
            "operating_hours": "월-금 10:00-19:00",
            "floor": -1,
            "place_name": "복지관 K-BOB",
        },
    },
    {
        "id": "venue-004",
        "embedding_text": "미래관 자율주행스튜디오(자주스) | 학습과 휴식을 함께 할 수 있는 열린 공간 | 학습공간 | 만남적합도:3 | 월-금 09:00-21:00 | 스터디, 휴식, 대화",
        "metadata": {
            "type": "study",
            "meeting_suitability": 3,
            "building_id": "building-mirae",
            "operating_hours": "월-금 09:00-21:00",
            "floor": 1,
            "place_name": "미래관 자율주행스튜디오(자주스)",
        },
    },
    {
        "id": "venue-005",
        "embedding_text": "미래관 무한상상실 | 학습에 집중할 수 있는 조용한 공간 | 학습공간 | 만남적합도:2 | 월-금 09:00-21:00 | 스터디, 과제, 토론",
        "metadata": {
            "type": "study",
            "meeting_suitability": 2,
            "building_id": "building-mirae",
            "operating_hours": "월-금 09:00-21:00",
            "floor": 1,
            "place_name": "미래관 무한상상실",
        },
    },
    {
        "id": "venue-006",
        "embedding_text": "예술관 1층 카페(편의점) | 음료와 간식을 사서 가볍게 앉아 이야기할 수 있는 공간 | 편의시설 | 만남적합도:4 | 월-금 08:00-21:00 | 간식, 커피, 대화",
        "metadata": {
            "type": "convenience",
            "meeting_suitability": 4,
            "building_id": "building-arts",
            "operating_hours": "월-금 08:00-21:00",
            "floor": 1,
            "place_name": "예술관 1층 카페(편의점)",
        },
    },
    {
        "id": "venue-007",
        "embedding_text": "공학관 1층 로비 | 넓은 공간에 테이블이 있어 밥 먹거나 대화하기 좋은 로비 | 로비 | 만남적합도:4 | 상시 개방 | 식사, 대화, 휴식",
        "metadata": {
            "type": "lounge",
            "meeting_suitability": 4,
            "building_id": "building-engineering",
            "operating_hours": "상시 개방",
            "floor": 1,
            "place_name": "공학관 1층 로비",
        },
    },
    {
        "id": "venue-008",
        "embedding_text": "북악관 1층 로비 | 테이블이 있어 밥 먹거나 앉아서 대화하기 좋은 로비 공간 | 로비 | 만남적합도:4 | 상시 개방 | 식사, 대화, 휴식",
        "metadata": {
            "type": "lounge",
            "meeting_suitability": 4,
            "building_id": "building-bukak",
            "operating_hours": "상시 개방",
            "floor": 1,
            "place_name": "북악관 1층 로비",
        },
    },
    {
        "id": "venue-009",
        "embedding_text": "경영관 | 경영대학 건물로 수업 전후 가볍게 만나기 좋은 장소 | 건물 | 만남적합도:3 | 상시 개방 | 대화, 만남",
        "metadata": {
            "type": "building",
            "meeting_suitability": 3,
            "building_id": "building-business",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "경영관",
        },
    },
    {
        "id": "venue-010",
        "embedding_text": "성곡도서관 1층 오픈 공간 | 넓고 쾌적한 열린 공간으로 가볍게 앉아 대화하거나 공부할 수 있는 곳 | 도서관 | 만남적합도:4 | 월-금 09:00-22:00, 토 09:00-17:00 | 스터디, 대화, 휴식",
        "metadata": {
            "type": "library",
            "meeting_suitability": 4,
            "building_id": "building-library",
            "operating_hours": "월-금 09:00-22:00, 토 09:00-17:00",
            "floor": 1,
            "place_name": "성곡도서관 1층 오픈 공간",
        },
    },
    {
        "id": "venue-011",
        "embedding_text": "성곡도서관 지하 1층 카페 해동 | 도서관 지하에 위치한 카페로 공부 전후 커피 마시며 대화하기 좋은 곳 | 카페 | 만남적합도:5 | 월-금 08:00-21:00 | 커피, 대화, 스터디",
        "metadata": {
            "type": "cafe",
            "meeting_suitability": 5,
            "building_id": "building-library",
            "operating_hours": "월-금 08:00-21:00",
            "floor": -1,
            "place_name": "성곡도서관 지하 1층 카페(해동)",
        },
    },
    {
        "id": "venue-012",
        "embedding_text": "법학관 | 법과대학 건물로 수업 전후 가볍게 만나기 좋은 장소 | 건물 | 만남적합도:3 | 상시 개방 | 대화, 만남",
        "metadata": {
            "type": "building",
            "meeting_suitability": 3,
            "building_id": "building-law",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "법학관",
        },
    },
    {
        "id": "venue-013",
        "embedding_text": "운동장 의자 (왼쪽 위) | 운동장 왼쪽 위편에 위치한 벤치로 야외에서 앉아 대화하기 좋은 곳 | 야외 | 만남적합도:4 | 상시 개방 | 대화, 휴식, 산책",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-stadium",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "운동장 의자 (왼쪽 위)",
        },
    },
    {
        "id": "venue-014",
        "embedding_text": "운동장 의자 (왼쪽 아래) | 운동장 왼쪽 아래편에 위치한 벤치로 야외에서 앉아 대화하기 좋은 곳 | 야외 | 만남적합도:4 | 상시 개방 | 대화, 휴식, 산책",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-stadium",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "운동장 의자 (왼쪽 아래)",
        },
    },
    {
        "id": "venue-015",
        "embedding_text": "운동장 의자 (오른쪽 위) | 운동장 오른쪽 위편에 위치한 벤치로 야외에서 앉아 대화하기 좋은 곳 | 야외 | 만남적합도:4 | 상시 개방 | 대화, 휴식, 산책",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-stadium",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "운동장 의자 (오른쪽 위)",
        },
    },
    {
        "id": "venue-016",
        "embedding_text": "운동장 의자 (오른쪽 아래) | 운동장 오른쪽 아래편에 위치한 벤치로 야외에서 앉아 대화하기 좋은 곳 | 야외 | 만남적합도:4 | 상시 개방 | 대화, 휴식, 산책",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-stadium",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "운동장 의자 (오른쪽 아래)",
        },
    },
    {
        "id": "venue-017",
        "embedding_text": "정문 버스정류장 | 학교 정문 앞 버스정류장으로 등하교 시 가볍게 만나기 좋은 장소 | 야외 | 만남적합도:3 | 상시 개방 | 만남, 대화",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 3,
            "building_id": "building-gate",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "정문 버스정류장",
        },
    },
    {
        "id": "venue-018",
        "embedding_text": "용두리 | 캠퍼스 내 자연 속 휴식 공간으로 산책하며 대화하기 좋은 곳 | 야외 | 만남적합도:4 | 상시 개방 | 산책, 대화, 휴식",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-yongduri",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "용두리",
        },
    },
]


async def seed_venues() -> None:
    """캠퍼스 장소 데이터를 ChromaDB venues 컬렉션에 시딩한다.

    1. 장소 데이터의 embeddingText를 Titan Embeddings로 벡터화
    2. ChromaDB venues 컬렉션에 저장
    """
    settings = get_settings()
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)

    print("🏫 캠퍼스 장소 데이터 시딩 시작")
    print(f"   ChromaDB 경로: {settings.chroma_persist_directory}")
    print(f"   임베딩 모델: {settings.bedrock_embedding_model_id}")
    print(f"   장소 수: {len(VENUE_DATA)}개")
    print()

    # 기존 데이터 수 확인
    existing_count = await vector_store.count(COLLECTION_VENUES)
    print(f"   기존 venues 컬렉션 문서 수: {existing_count}개")

    # 임베딩 생성
    print("\n📐 임베딩 생성 중...")
    ids: list[str] = []
    documents: list[str] = []
    metadatas: list[dict] = []
    embeddings: list[list[float]] = []

    for i, venue in enumerate(VENUE_DATA, 1):
        print(f"   [{i}/{len(VENUE_DATA)}] {venue['metadata']['place_name']}...", end=" ")

        embedding = await embedding_generator.generate(venue["embedding_text"])
        embeddings.append(embedding)

        ids.append(venue["id"])
        documents.append(venue["embedding_text"])
        metadatas.append(venue["metadata"])

        print(f"✅ (dim={len(embedding)})")

    # ChromaDB에 저장
    print(f"\n💾 ChromaDB venues 컬렉션에 저장 중...")
    await vector_store.add_documents(
        collection_name=COLLECTION_VENUES,
        documents=documents,
        embeddings=embeddings,
        metadatas=metadatas,
        ids=ids,
    )

    # 저장 확인
    final_count = await vector_store.count(COLLECTION_VENUES)
    print(f"   ✅ 저장 완료! 총 문서 수: {final_count}개")

    print("\n" + "=" * 60)
    print("🎉 캠퍼스 장소 데이터 시딩 완료!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(seed_venues())
