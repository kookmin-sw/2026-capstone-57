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

# 캠퍼스 장소 데이터 (15개)
# embeddingText 형식: "장소명 | 설명 | 유형 | 만남적합도 | 운영시간 | 추천활동"
VENUE_DATA = [
    {
        "id": "venue-001",
        "embedding_text": "학생회관 1층 카페 | 조용하고 대화하기 좋은 분위기의 카페 | 카페 | 만남적합도:5 | 월-금 08:00-21:00 | 커피, 대화, 스터디",
        "metadata": {
            "type": "cafe",
            "meeting_suitability": 5,
            "building_id": "building-student-hall",
            "operating_hours": "월-금 08:00-21:00",
            "floor": 1,
            "place_name": "학생회관 1층 카페",
        },
    },
    {
        "id": "venue-002",
        "embedding_text": "중앙도서관 3층 스터디룸 | 조용한 환경에서 함께 공부할 수 있는 스터디룸 | 도서관 | 만남적합도:3 | 월-금 09:00-22:00, 토 09:00-17:00 | 스터디, 과제, 토론",
        "metadata": {
            "type": "library",
            "meeting_suitability": 3,
            "building_id": "building-library",
            "operating_hours": "월-금 09:00-22:00, 토 09:00-17:00",
            "floor": 3,
            "place_name": "중앙도서관 3층 스터디룸",
        },
    },
    {
        "id": "venue-003",
        "embedding_text": "공학관 앞 벤치 | 날씨 좋은 날 가볍게 앉아서 이야기하기 좋은 야외 공간 | 야외 | 만남적합도:4 | 상시 개방 | 대화, 산책, 간식",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-engineering",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "공학관 앞 벤치",
        },
    },
    {
        "id": "venue-004",
        "embedding_text": "학생식당 2층 | 점심시간에 함께 밥 먹으며 대화하기 좋은 식당 | 식당 | 만남적합도:4 | 월-금 11:00-14:00, 17:00-19:00 | 식사, 대화",
        "metadata": {
            "type": "restaurant",
            "meeting_suitability": 4,
            "building_id": "building-student-hall",
            "operating_hours": "월-금 11:00-14:00, 17:00-19:00",
            "floor": 2,
            "place_name": "학생식당 2층",
        },
    },
    {
        "id": "venue-005",
        "embedding_text": "체육관 로비 | 운동 전후로 가볍게 만나기 좋은 넓은 로비 공간 | 체육시설 | 만남적합도:3 | 월-금 06:00-22:00, 토-일 09:00-18:00 | 운동, 대화, 스트레칭",
        "metadata": {
            "type": "sports",
            "meeting_suitability": 3,
            "building_id": "building-gym",
            "operating_hours": "월-금 06:00-22:00, 토-일 09:00-18:00",
            "floor": 1,
            "place_name": "체육관 로비",
        },
    },
    {
        "id": "venue-006",
        "embedding_text": "인문관 옥상 정원 | 탁 트인 전망과 함께 여유롭게 대화할 수 있는 옥상 정원 | 야외 | 만남적합도:5 | 월-금 09:00-18:00 | 대화, 산책, 사진촬영",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 5,
            "building_id": "building-humanities",
            "operating_hours": "월-금 09:00-18:00",
            "floor": 5,
            "place_name": "인문관 옥상 정원",
        },
    },
    {
        "id": "venue-007",
        "embedding_text": "경영관 1층 편의점 앞 테이블 | 간단한 간식과 음료를 사서 앉아 이야기할 수 있는 공간 | 편의시설 | 만남적합도:3 | 월-금 07:00-23:00 | 간식, 대화, 휴식",
        "metadata": {
            "type": "convenience",
            "meeting_suitability": 3,
            "building_id": "building-business",
            "operating_hours": "월-금 07:00-23:00",
            "floor": 1,
            "place_name": "경영관 1층 편의점 앞 테이블",
        },
    },
    {
        "id": "venue-008",
        "embedding_text": "중앙 잔디광장 | 넓은 잔디밭에서 돗자리 깔고 피크닉하기 좋은 캠퍼스 중심 공간 | 야외 | 만남적합도:5 | 상시 개방 | 피크닉, 대화, 독서, 산책",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 5,
            "building_id": "building-central",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "중앙 잔디광장",
        },
    },
    {
        "id": "venue-009",
        "embedding_text": "예술관 갤러리 카페 | 전시 작품을 감상하며 커피를 마실 수 있는 분위기 있는 카페 | 카페 | 만남적합도:5 | 월-금 10:00-20:00 | 커피, 전시감상, 대화",
        "metadata": {
            "type": "cafe",
            "meeting_suitability": 5,
            "building_id": "building-arts",
            "operating_hours": "월-금 10:00-20:00",
            "floor": 1,
            "place_name": "예술관 갤러리 카페",
        },
    },
    {
        "id": "venue-010",
        "embedding_text": "자연과학관 옥상 천문대 | 저녁에 별을 관측하며 특별한 시간을 보낼 수 있는 천문대 | 특수시설 | 만남적합도:4 | 화, 목 19:00-22:00 | 별관측, 대화, 사진촬영",
        "metadata": {
            "type": "special",
            "meeting_suitability": 4,
            "building_id": "building-science",
            "operating_hours": "화, 목 19:00-22:00",
            "floor": 6,
            "place_name": "자연과학관 옥상 천문대",
        },
    },
    {
        "id": "venue-011",
        "embedding_text": "공학관 3층 메이커스페이스 | 함께 간단한 만들기 활동을 할 수 있는 창작 공간 | 특수시설 | 만남적합도:3 | 월-금 09:00-21:00 | 만들기, 3D프린팅, 협업",
        "metadata": {
            "type": "special",
            "meeting_suitability": 3,
            "building_id": "building-engineering",
            "operating_hours": "월-금 09:00-21:00",
            "floor": 3,
            "place_name": "공학관 3층 메이커스페이스",
        },
    },
    {
        "id": "venue-012",
        "embedding_text": "학생회관 지하 1층 탁구장 | 가볍게 탁구 치면서 자연스럽게 친해질 수 있는 공간 | 체육시설 | 만남적합도:4 | 월-금 10:00-20:00 | 탁구, 운동, 대화",
        "metadata": {
            "type": "sports",
            "meeting_suitability": 4,
            "building_id": "building-student-hall",
            "operating_hours": "월-금 10:00-20:00",
            "floor": -1,
            "place_name": "학생회관 지하 1층 탁구장",
        },
    },
    {
        "id": "venue-013",
        "embedding_text": "캠퍼스 산책로 (후문~정문) | 자연 속에서 걸으며 대화하기 좋은 산책 코스 | 야외 | 만남적합도:4 | 상시 개방 | 산책, 대화, 운동",
        "metadata": {
            "type": "outdoor",
            "meeting_suitability": 4,
            "building_id": "building-central",
            "operating_hours": "상시 개방",
            "floor": 0,
            "place_name": "캠퍼스 산책로 (후문~정문)",
        },
    },
    {
        "id": "venue-014",
        "embedding_text": "사회과학관 2층 라운지 | 소파와 테이블이 있어 편하게 앉아 이야기할 수 있는 라운지 | 라운지 | 만남적합도:4 | 월-금 08:00-22:00 | 대화, 휴식, 스터디",
        "metadata": {
            "type": "lounge",
            "meeting_suitability": 4,
            "building_id": "building-social-science",
            "operating_hours": "월-금 08:00-22:00",
            "floor": 2,
            "place_name": "사회과학관 2층 라운지",
        },
    },
    {
        "id": "venue-015",
        "embedding_text": "정문 앞 푸드트럭 거리 | 다양한 음식을 먹으며 활기찬 분위기에서 만날 수 있는 거리 | 식당 | 만남적합도:4 | 월-금 11:00-20:00 | 식사, 간식, 대화, 구경",
        "metadata": {
            "type": "restaurant",
            "meeting_suitability": 4,
            "building_id": "building-gate",
            "operating_hours": "월-금 11:00-20:00",
            "floor": 0,
            "place_name": "정문 앞 푸드트럭 거리",
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
