"""캠퍼스 노드 데이터 시딩 스크립트.

VENUE(외부 경로 포인트) + BUILDING_PLACE(건물 내부 장소) 데이터를
인덱싱 API를 통해 ChromaDB에 저장한다.

실행:
    python3 scripts/seed_venues.py
"""

from __future__ import annotations

import sys
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

BASE_URL = "http://localhost:8000"

# ── VENUE (외부 경로 포인트, 서브 노드) ──────────────────────────────────────
VENUE_NODES = [
    {
        "nodeId": "uuid-1",
        "source": "VENUE",
        "name": "북악관 왼쪽 입구",
        "typeActivity": ["CAFE", "CONVENIENCE_STORE", "RESTAURANT"],
        "description": "북악관(N2동) 서쪽 입구, 편의점과 카페 접근 가능",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-2",
        "source": "VENUE",
        "name": "북악관 오른쪽 입구",
        "typeActivity": ["CAFE", "CONVENIENCE_STORE", "RESTAURANT"],
        "description": "북악관(N2동) 동쪽 입구, 편의점과 카페 접근 가능",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-3",
        "source": "VENUE",
        "name": "북악관 앞 삼거리",
        "typeActivity": ["OTHER"],
        "description": "북악관(N2동) 정면에서 다른 건물로 내려가는 삼거리",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-4",
        "source": "VENUE",
        "name": "용두리",
        "typeActivity": ["OTHER"],
        "description": "용두리, 캠퍼스 내 자연 속 휴식 공간",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-5",
        "source": "VENUE",
        "name": "경상관-국제관 사이 계단",
        "typeActivity": ["OTHER"],
        "description": "북악관에서 운동장으로 내려가는 주요 계단",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-6",
        "source": "VENUE",
        "name": "경영관 후문",
        "typeActivity": ["OTHER"],
        "description": "경영관에서 본부관으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-7",
        "source": "VENUE",
        "name": "경영관 앞문",
        "typeActivity": ["OTHER"],
        "description": "경영관에서 예술관으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-8",
        "source": "VENUE",
        "name": "북서쪽 운동장",
        "typeActivity": ["OTHER"],
        "description": "경상관 앞쪽 운동장, 주차장 서문 엘리베이터 근처",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-9",
        "source": "VENUE",
        "name": "북동쪽 운동장",
        "typeActivity": ["OTHER"],
        "description": "경영관 앞쪽 운동장, 주차장 동문 엘리베이터 근처",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-10",
        "source": "VENUE",
        "name": "남서쪽 운동장",
        "typeActivity": ["OTHER"],
        "description": "정문 쪽 운동장",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-11",
        "source": "VENUE",
        "name": "남동쪽 운동장",
        "typeActivity": ["OTHER"],
        "description": "미래관 쪽 운동장",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-12",
        "source": "VENUE",
        "name": "예대 앞 계단",
        "typeActivity": ["OTHER"],
        "description": "예대에서 경영관 올라가는 계단",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-13",
        "source": "VENUE",
        "name": "미래관 앞문 (예대방향)",
        "typeActivity": ["OTHER"],
        "description": "미래관 4층, 예대쪽으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-14",
        "source": "VENUE",
        "name": "미래관 뒷문 (복지관방향)",
        "typeActivity": ["OTHER"],
        "description": "미래관에서 복지관으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-15",
        "source": "VENUE",
        "name": "복지관 동쪽 입구",
        "typeActivity": ["OTHER"],
        "description": "복지관에서 미래관으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-16",
        "source": "VENUE",
        "name": "복지관 서쪽 입구",
        "typeActivity": ["OTHER"],
        "description": "복지관에서 정문으로 가는 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-17",
        "source": "VENUE",
        "name": "정문",
        "typeActivity": ["OTHER"],
        "description": "국민대학교 정문, 버스정류장 근처",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-18",
        "source": "VENUE",
        "name": "공학관 뒷문",
        "typeActivity": ["OTHER"],
        "description": "정문에서 공학관으로 가는 테니스장 옆 길",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-19",
        "source": "VENUE",
        "name": "공학관 가운데 입구",
        "typeActivity": ["OTHER"],
        "description": "공학관 가운데 입구",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-20",
        "source": "VENUE",
        "name": "공학관 동쪽 입구",
        "typeActivity": ["CONVENIENCE_STORE"],
        "description": "공학관 오른쪽 입구, 편의점 근처",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-21",
        "source": "VENUE",
        "name": "공학관 서쪽 입구",
        "typeActivity": ["OTHER"],
        "description": "공학관 왼쪽 입구, 도서관 앞",
        "operatingHours": "24시간",
    },
    {
        "nodeId": "uuid-22",
        "source": "VENUE",
        "name": "성곡도서관 입구",
        "typeActivity": ["OTHER"],
        "description": "성곡도서관 입구",
        "operatingHours": "24시간",
    },
]

# ── BUILDING_PLACE (건물 내부 장소) ──────────────────────────────────────────
BUILDING_PLACE_NODES = [
    {
        "nodeId": "uuid-101",
        "source": "BUILDING_PLACE",
        "name": "북악관 편의점",
        "typeActivity": ["CONVENIENCE_STORE"],
        "description": "북악관(N2동) 1층 편의점, 간식과 음료 구매 가능",
        "operatingHours": "월-금 08:00-22:00",
        "buildingName": "북악관",
        "floor": 1,
    },
    {
        "nodeId": "uuid-102",
        "source": "BUILDING_PLACE",
        "name": "북악관 카페",
        "typeActivity": ["CAFE"],
        "description": "북악관(N2동) 1층 카페",
        "operatingHours": "월-금 11:00-17:00",
        "buildingName": "북악관",
        "floor": 1,
    },
    {
        "nodeId": "uuid-103",
        "source": "BUILDING_PLACE",
        "name": "복지관 학식",
        "typeActivity": ["RESTAURANT"],
        "description": "종합복지관(S1동) 지하1층 학생식당",
        "operatingHours": "월-금 11:00-14:00, 17:00-19:00",
        "buildingName": "복지관",
        "floor": -1,
    },
    {
        "nodeId": "uuid-104",
        "source": "BUILDING_PLACE",
        "name": "복지관 카페",
        "typeActivity": ["CAFE"],
        "description": "종합복지관(S1동) 지하1층 카페",
        "operatingHours": "월-금 08:00-17:00",
        "buildingName": "복지관",
        "floor": -1,
    },
    {
        "nodeId": "uuid-105",
        "source": "BUILDING_PLACE",
        "name": "복지관 K-BOB",
        "typeActivity": ["RESTAURANT"],
        "description": "종합복지관(S1동) 지하1층 분식/간편식 매장",
        "operatingHours": "월-금 10:00-19:00",
        "buildingName": "복지관",
        "floor": -1,
    },
    {
        "nodeId": "uuid-106",
        "source": "BUILDING_PLACE",
        "name": "할리스 카페",
        "typeActivity": ["CAFE"],
        "description": "성곡도서관 지하 1층 할리스 카페 (해동도서관), 대화하기 좋은 공간",
        "operatingHours": "월-금 08:00-21:00, 토 10:00-18:00",
        "buildingName": "성곡도서관",
        "floor": -1,
    },
    {
        "nodeId": "uuid-107",
        "source": "BUILDING_PLACE",
        "name": "이마트24 (공학관)",
        "typeActivity": ["CONVENIENCE_STORE"],
        "description": "공학관(W1동) 편의점",
        "operatingHours": "24시간",
        "buildingName": "공학관",
        "floor": 1,
    },
    {
        "nodeId": "uuid-108",
        "source": "BUILDING_PLACE",
        "name": "예대 매점",
        "typeActivity": ["CONVENIENCE_STORE", "CAFE"],
        "description": "예술대학 근처 매점, 간식과 음료 구매 가능",
        "operatingHours": "월-금 09:00-18:00",
        "buildingName": "예술대학",
        "floor": 1,
    },
    {
        "nodeId": "uuid-109",
        "source": "BUILDING_PLACE",
        "name": "미래관 자주스",
        "typeActivity": ["STUDY_ROOM"],
        "description": "미래관(S2동) 4층 자율주행스튜디오, 학습과 휴식 공간",
        "operatingHours": "월-금 09:00-21:00",
        "buildingName": "미래관",
        "floor": 4,
    },
    {
        "nodeId": "uuid-110",
        "source": "BUILDING_PLACE",
        "name": "미래관 무한상상실",
        "typeActivity": ["STUDY_ROOM"],
        "description": "미래관(S2동) 4층 학습 공간",
        "operatingHours": "월-금 09:00-21:00",
        "buildingName": "미래관",
        "floor": 4,
    },
]


def seed_nodes():
    """인덱싱 API를 통해 모든 노드를 ChromaDB에 시딩한다."""
    all_nodes = VENUE_NODES + BUILDING_PLACE_NODES
    total = len(all_nodes)

    print("🏫 캠퍼스 노드 데이터 시딩 시작")
    print(f"   서버: {BASE_URL}")
    print(f"   VENUE 노드: {len(VENUE_NODES)}개")
    print(f"   BUILDING_PLACE 노드: {len(BUILDING_PLACE_NODES)}개")
    print(f"   총: {total}개")
    print()

    success = 0
    failed = 0

    for i, node in enumerate(all_nodes, 1):
        name = node["name"]
        source = node["source"]
        print(f"   [{i}/{total}] [{source}] {name}...", end=" ")

        try:
            response = requests.post(
                f"{BASE_URL}/api/campus-nodes/index",
                json=node,
                timeout=30,
            )
            if response.status_code == 200:
                print("✅")
                success += 1
            else:
                print(f"❌ ({response.status_code}: {response.text[:100]})")
                failed += 1
        except Exception as e:
            print(f"❌ (에러: {e})")
            failed += 1

    print(f"\n{'=' * 60}")
    print(f"🎉 시딩 완료! 성공: {success}개, 실패: {failed}개")
    print(f"{'=' * 60}")


if __name__ == "__main__":
    seed_nodes()
