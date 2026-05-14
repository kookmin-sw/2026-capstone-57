"""미션 생성을 위한 장소 벡터 검색.

동선 교집합 정보와 사용자 프로필을 기반으로 ChromaDB에서
적합한 캠퍼스 장소를 벡터 유사도 검색한다.

검색 흐름:
    동선 교집합 + 사용자 프로필 → 자연어 검색 쿼리 구성
    → Titan Embeddings 벡터화 → ChromaDB 코사인 유사도 검색
    → 상위 K개 장소 반환

사용 예시:
    from app.features.mission.search import MissionSearch
    from app.rag.embeddings import EmbeddingGenerator
    from app.rag.vector_store import VectorStore
    from app.config import get_settings

    settings = get_settings()
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    search = MissionSearch(embedding_generator, vector_store, settings)

    results = await search.search_venues(intersection_info, user_profiles)

Requirements: 3.1, 3.2, 3.3, 3.4
"""

from __future__ import annotations

import asyncio
import logging

from app.config import Settings
from app.features.mission.models import IntersectionInfo, UserProfile, VenueResult
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import COLLECTION_VENUES, VectorStore

logger = logging.getLogger(__name__)

# 요일 매핑 (dayOfWeek: 1=월 ~ 7=일)
_DAY_NAMES = {
    1: "월요일",
    2: "화요일",
    3: "수요일",
    4: "목요일",
    5: "금요일",
    6: "토요일",
    7: "일요일",
}


class MissionSearch:
    """미션 생성을 위한 장소 벡터 검색.

    동선 교집합 정보를 자연어 검색 쿼리로 변환하고,
    ChromaDB venues 컬렉션에서 코사인 유사도 기반으로
    적합한 장소를 검색한다.

    Args:
        embedding_generator: Titan Embeddings 임베딩 생성기.
        vector_store: ChromaDB 벡터 스토어.
        settings: 애플리케이션 설정.
    """

    def __init__(
        self,
        embedding_generator: EmbeddingGenerator,
        vector_store: VectorStore,
        settings: Settings,
    ) -> None:
        self._embedding_generator = embedding_generator
        self._vector_store = vector_store
        self._top_k = settings.mission_search_top_k

    async def search_venues(
        self,
        intersection_info: IntersectionInfo,
        user_profiles: list[UserProfile],
        top_k: int | None = None,
    ) -> list[VenueResult]:
        """동선 교집합 기반 장소 벡터 유사도 검색.

        검색 쿼리를 자연어로 구성한 후 임베딩을 생성하고,
        ChromaDB venues 컬렉션에서 metadata 필터(building_id)를
        적용하여 상위 K개 장소를 반환한다.

        Args:
            intersection_info: 동선 교집합 정보 (시간대, 건물 위치, 요일).
            user_profiles: 두 사용자의 프로필 정보.
            top_k: 반환할 최대 결과 수 (None이면 설정값 사용).

        Returns:
            유사도 순으로 정렬된 VenueResult 리스트.

        Raises:
            BedrockInvocationError: 임베딩 생성 실패 시.
            Exception: ChromaDB 검색 실패 시.
        """
        k = top_k if top_k is not None else self._top_k

        # 1. 자연어 검색 쿼리 구성
        query = self.build_search_query(intersection_info, user_profiles)

        logger.info(
            "미션 장소 검색 시작: query=%s, top_k=%d",
            query,
            k,
        )

        # 2. 쿼리 임베딩 생성
        query_embedding = await self._embedding_generator.generate(query)

        # 3. metadata 필터 구성 (building_id 기반)
        where_filter = self._build_metadata_filter(intersection_info)

        # 4. ChromaDB 벡터 유사도 검색
        results = await self._query_venues(query_embedding, where_filter, k)

        # 5. 필터 검색 결과가 부족하면 필터 없이 재검색
        if len(results) < k and where_filter is not None:
            logger.info(
                "필터 검색 결과 부족 (%d/%d), 필터 없이 재검색",
                len(results),
                k,
            )
            unfiltered_results = await self._query_venues(
                query_embedding, None, k
            )
            # 기존 결과에 없는 항목만 추가
            existing_ids = {r.venueId for r in results}
            for result in unfiltered_results:
                if result.venueId not in existing_ids and len(results) < k:
                    results.append(result)

        logger.info(
            "미션 장소 검색 완료: results=%d",
            len(results),
        )

        return results

    def build_search_query(
        self,
        intersection_info: IntersectionInfo,
        user_profiles: list[UserProfile],
    ) -> str:
        """동선 교집합 정보와 사용자 프로필을 자연어 검색 쿼리로 변환한다.

        예시 출력: "14시에 공학관 근처에서 두 사람이 대화할 수 있는 조용한 장소"

        Args:
            intersection_info: 동선 교집합 정보.
            user_profiles: 두 사용자의 프로필 정보.

        Returns:
            자연어 검색 쿼리 문자열.
        """
        parts: list[str] = []

        # 시간대 정보
        if intersection_info.timeSlots:
            time_str = ", ".join(intersection_info.timeSlots)
            parts.append(f"{time_str} 시간대")

        # 요일 정보
        day_name = _DAY_NAMES.get(intersection_info.dayOfWeek, "")
        if day_name:
            parts.append(day_name)

        # 건물 위치 정보
        if intersection_info.buildingIds:
            building_str = ", ".join(intersection_info.buildingIds)
            parts.append(f"{building_str} 근처")

        # 사용자 관심사 통합
        all_interests: list[str] = []
        for profile in user_profiles:
            all_interests.extend(profile.interests)

        if all_interests:
            unique_interests = list(dict.fromkeys(all_interests))[:4]
            interests_str = ", ".join(unique_interests)
            parts.append(f"{interests_str} 관련 활동")

        # 기본 맥락 추가
        parts.append("두 사람이 만나서 대화할 수 있는 장소")

        return " ".join(parts)

    def _build_metadata_filter(
        self, intersection_info: IntersectionInfo
    ) -> dict | None:
        """building_name 기반 metadata 필터를 구성한다.

        ChromaDB where 절에 사용할 필터를 생성한다.
        buildingIds에 건물 이름(미래관, 북악관 등)이 들어온다.

        Args:
            intersection_info: 동선 교집합 정보.

        Returns:
            ChromaDB where 필터 딕셔너리, 필터 불필요 시 None.
        """
        building_ids = intersection_info.buildingIds

        if not building_ids:
            return None

        if len(building_ids) == 1:
            return {"building_name": building_ids[0]}

        return {"building_name": {"$in": building_ids}}

    async def _query_venues(
        self,
        query_embedding: list[float],
        where_filter: dict | None,
        top_k: int,
    ) -> list[VenueResult]:
        """ChromaDB venues 컬렉션에서 벡터 유사도 검색을 수행한다.

        Args:
            query_embedding: 쿼리 임베딩 벡터.
            where_filter: metadata 필터 (None이면 필터 없이 검색).
            top_k: 반환할 최대 결과 수.

        Returns:
            VenueResult 리스트.
        """
        collection = self._vector_store._get_collection(COLLECTION_VENUES)

        query_params: dict = {
            "query_embeddings": [query_embedding],
            "n_results": top_k,
            "include": ["documents", "metadatas", "distances"],
        }

        if where_filter is not None:
            query_params["where"] = where_filter

        results = await asyncio.to_thread(collection.query, **query_params)

        venue_results: list[VenueResult] = []

        if results and results["ids"] and results["ids"][0]:
            ids = results["ids"][0]
            documents = results["documents"][0] if results["documents"] else [""] * len(ids)
            metadatas = results["metadatas"][0] if results["metadatas"] else [{}] * len(ids)
            distances = results["distances"][0] if results["distances"] else [0.0] * len(ids)

            for venue_id, doc, meta, dist in zip(ids, documents, metadatas, distances):
                venue_results.append(
                    VenueResult(
                        venueId=venue_id,
                        document=doc or "",
                        metadata=meta or {},
                        distance=dist,
                    )
                )

        return venue_results
