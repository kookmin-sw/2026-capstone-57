"""캠퍼스 장소 데이터 시딩 스크립트.

Backend DB 스키마 기준:
- campus_building: 건물 (미래관, 북악관, ...)
- campus_venue: 중간 거점 (용두리, 농구장, ...)
- place: 건물 내 장소 (자주스, 과방, 편의점, ...)

ChromaDB venues 컬렉션에 place + campus_venue 데이터를 시딩한다.
building_id는 건물 이름을 snake_case로 변환하여 사용한다.

실행:
    python3 scripts/seed_venues.py
"""

from __future__ import annotations

import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.config import get_settings
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import COLLECTION_VENUES, VectorStore

# 장소 타입 → 설명 매핑
TYPE_DESC = {
    "CAFE": "카페",
    "CONVENIENCE_STORE": "편의점",
    "RESTAURANT": "식당",
    "LECTURE_ROOM": "강의실",
    "STUDY_ROOM": "스터디룸/학습공간",
    "MEETING_ROOM": "회의실/모임공간",
    "ELEVATOR": "엘리베이터",
    "BENCH": "벤치/야외공간",
    "OTHER": "기타",
}

# 장소 타입 → 만남 적합도
TYPE_SUITABILITY = {
    "CAFE": 5,
    "CONVENIENCE_STORE": 4,
    "RESTAURANT": 4,
    "LECTURE_ROOM": 1,
    "STUDY_ROOM": 3,
    "MEETING_ROOM": 3,
    "ELEVATOR": 1,
    "BENCH": 4,
    "OTHER": 3,
}

# 장소 타입 → 추천 활동
TYPE_ACTIVITIES = {
    "CAFE": "커피, 대화, 휴식",
    "CONVENIENCE_STORE": "간식, 음료, 대화",
    "RESTAURANT": "식사, 대화",
    "LECTURE_ROOM": "수업, 스터디",
    "STUDY_ROOM": "스터디, 과제, 대화",
    "MEETING_ROOM": "모임, 대화, 협업",
    "ELEVATOR": "이동",
    "BENCH": "대화, 휴식, 산책",
    "OTHER": "대화, 휴식",
}

# ── place 데이터 (건물 내 장소) ──────────────────────────────────────────────
# 형식: (id, building_name, name, floor, type)
PLACE_DATA = [
    # 미래관
    ("place-001", "미래관", "자주스", 4, "STUDY_ROOM"),
    ("place-002", "미래관", "무한상상실", 3, "STUDY_ROOM"),
    ("place-003", "미래관", "과방", 3, "MEETING_ROOM"),
    # 북악관
    ("place-004", "북악관", "편의점", 1, "CONVENIENCE_STORE"),
    ("place-005", "북악관", "1층 로비", 1, "OTHER"),
    # 복지관
    ("place-006", "복지관", "학식", -1, "RESTAURANT"),
    ("place-007", "복지관", "카페", -1, "CAFE"),
    ("place-008", "복지관", "K-BOB", -1, "RESTAURANT"),
    # 예술관
    ("place-009", "예술관", "1층 카페(편의점)", 1, "CONVENIENCE_STORE"),
    # 공학관
    ("place-010", "공학관", "1층 로비", 1, "OTHER"),
    # 경영관
    ("place-011", "경영관", "경영관", 0, "OTHER"),
    # 성곡도서관
    ("place-012", "성곡도서관", "1층 오픈 공간", 1, "STUDY_ROOM"),
    ("place-013", "성곡도서관", "지하 1층 카페(해동)", -1, "CAFE"),
    # 법학관
    ("place-014", "법학관", "법학관", 0, "OTHER"),
]

# ── campus_venue 데이터 (중간 거점) ──────────────────────────────────────────
# 형식: (id, name, type, description, activities)
VENUE_DATA = [
    ("venue-001", "용두리", "BENCH", "캠퍼스 내 자연 속 휴식 공간으로 산책하며 대화하기 좋은 곳", "산책, 대화, 휴식"),
    ("venue-002", "농구장", "BENCH", "농구장 주변 벤치에서 운동 후 가볍게 대화하기 좋은 공간", "운동, 대화, 휴식"),
    ("venue-003", "운동장", "BENCH", "넓은 운동장 주변 의자에서 앉아 대화하기 좋은 야외 공간", "대화, 휴식, 산책"),
    ("venue-004", "예대 매점", "CONVENIENCE_STORE", "예술대학 근처 매점으로 간식을 사서 가볍게 만나기 좋은 곳", "간식, 대화, 휴식"),
    ("venue-005", "정문 버스정류장", "BENCH", "학교 정문 앞 버스정류장으로 등하교 시 가볍게 만나기 좋은 장소", "만남, 대화"),
]


def _build_embedding_text(
    name: str,
    building_name: str,
    floor: int,
    place_type: str,
    description: str = "",
    activities: str = "",
) -> str:
    """embeddingText 구성: '장소명 | 설명 | 유형 | 만남적합도 | 위치 | 추천활동'"""
    type_desc = TYPE_DESC.get(place_type, "기타")
    suitability = TYPE_SUITABILITY.get(place_type, 3)
    acts = activities or TYPE_ACTIVITIES.get(place_type, "대화, 휴식")
    desc = description or f"{building_name} {floor}층에 위치한 {type_desc}"
    floor_str = f"{floor}층" if floor >= 0 else f"지하 {abs(floor)}층"

    return f"{name} | {desc} | {type_desc} | 만남적합도:{suitability} | {building_name} {floor_str} | {acts}"


def _building_to_id(building_name: str) -> str:
    """건물 이름 → building_id 변환."""
    mapping = {
        "미래관": "building-mirae",
        "북악관": "building-bukak",
        "복지관": "building-welfare",
        "예술관": "building-arts",
        "공학관": "building-engineering",
        "경영관": "building-business",
        "성곡도서관": "building-library",
        "법학관": "building-law",
    }
    return mapping.get(building_name, f"building-{building_name}")


async def seed_venues() -> None:
    """ChromaDB venues 컬렉션에 장소 데이터를 시딩한다."""
    settings = get_settings()
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)

    total = len(PLACE_DATA) + len(VENUE_DATA)
    print("🏫 캠퍼스 장소 데이터 시딩 시작")
    print(f"   ChromaDB 경로: {settings.chroma_persist_directory}")
    print(f"   임베딩 모델: {settings.bedrock_embedding_model_id}")
    print(f"   장소(place) 수: {len(PLACE_DATA)}개")
    print(f"   거점(venue) 수: {len(VENUE_DATA)}개")
    print(f"   총: {total}개")
    print()

    existing_count = await vector_store.count(COLLECTION_VENUES)
    print(f"   기존 venues 컬렉션 문서 수: {existing_count}개")

    ids: list[str] = []
    documents: list[str] = []
    metadatas: list[dict] = []
    embeddings: list[list[float]] = []

    print("\n📐 임베딩 생성 중...")
    idx = 1

    # place 데이터 처리
    for place_id, building_name, name, floor, place_type in PLACE_DATA:
        embedding_text = _build_embedding_text(name, building_name, floor, place_type)
        print(f"   [{idx}/{total}] {building_name} - {name}...", end=" ")

        embedding = await embedding_generator.generate(embedding_text)
        embeddings.append(embedding)
        ids.append(place_id)
        documents.append(embedding_text)
        metadatas.append({
            "type": place_type.lower(),
            "meeting_suitability": TYPE_SUITABILITY.get(place_type, 3),
            "building_id": _building_to_id(building_name),
            "building_name": building_name,
            "floor": floor,
            "place_name": name,
            "source": "place",
        })
        print(f"✅ (dim={len(embedding)})")
        idx += 1

    # campus_venue 데이터 처리
    for venue_id, name, venue_type, description, activities in VENUE_DATA:
        embedding_text = _build_embedding_text(
            name, "캠퍼스", 0, venue_type, description, activities
        )
        print(f"   [{idx}/{total}] 거점 - {name}...", end=" ")

        embedding = await embedding_generator.generate(embedding_text)
        embeddings.append(embedding)
        ids.append(venue_id)
        documents.append(embedding_text)
        metadatas.append({
            "type": venue_type.lower(),
            "meeting_suitability": TYPE_SUITABILITY.get(venue_type, 3),
            "building_id": "campus-outdoor",
            "building_name": "캠퍼스",
            "floor": 0,
            "place_name": name,
            "source": "campus_venue",
        })
        print(f"✅ (dim={len(embedding)})")
        idx += 1

    # ChromaDB에 저장
    print(f"\n💾 ChromaDB venues 컬렉션에 저장 중...")
    await vector_store.add_documents(
        collection_name=COLLECTION_VENUES,
        documents=documents,
        embeddings=embeddings,
        metadatas=metadatas,
        ids=ids,
    )

    final_count = await vector_store.count(COLLECTION_VENUES)
    print(f"   ✅ 저장 완료! 총 문서 수: {final_count}개")
    print("\n" + "=" * 60)
    print("🎉 캠퍼스 장소 데이터 시딩 완료!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(seed_venues())
