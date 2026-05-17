"""미션 생성을 위한 서브 노드 검색.

겹치는 서브 노드 ID로 ChromaDB에서 상세 정보를 검색한다.
"""

from __future__ import annotations

import asyncio
import logging

from app.config import Settings
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import COLLECTION_VENUES, VectorStore

logger = logging.getLogger(__name__)


class NodeSearchResult:
    """노드 검색 결과."""

    def __init__(self, node_id: str, name: str, type_activity: str,
                 description: str, operating_hours: str = ""):
        self.node_id = node_id
        self.name = name
        self.type_activity = type_activity
        self.description = description
        self.operating_hours = operating_hours


class MissionSearch:
    """서브 노드 기반 장소 검색.

    겹치는 서브 노드 ID로 ChromaDB venues 컬렉션에서
    상세 정보를 직접 조회한다.
    """

    def __init__(
        self,
        embedding_generator: EmbeddingGenerator,
        vector_store: VectorStore,
        settings: Settings,
    ) -> None:
        self._embedding_generator = embedding_generator
        self._vector_store = vector_store

    async def search_by_node_ids(
        self,
        node_ids: list[str],
    ) -> list[NodeSearchResult]:
        """노드 ID 목록으로 ChromaDB에서 상세 정보를 조회한다.

        Args:
            node_ids: 검색할 서브 노드 ID 리스트.

        Returns:
            NodeSearchResult 리스트.
        """
        if not node_ids:
            return []

        collection = self._vector_store._get_collection(COLLECTION_VENUES)

        # ChromaDB get으로 ID 기반 직접 조회
        results = await asyncio.to_thread(
            collection.get,
            ids=node_ids,
            include=["documents", "metadatas"],
        )

        search_results: list[NodeSearchResult] = []

        if results and results["ids"]:
            ids = results["ids"]
            documents = results["documents"] if results["documents"] else [""] * len(ids)
            metadatas = results["metadatas"] if results["metadatas"] else [{}] * len(ids)

            for node_id, doc, meta in zip(ids, documents, metadatas):
                search_results.append(
                    NodeSearchResult(
                        node_id=node_id,
                        name=meta.get("name", ""),
                        type_activity=meta.get("type_activity", ""),
                        description=meta.get("description", ""),
                        operating_hours=meta.get("operating_hours", ""),
                    )
                )

        logger.info("노드 검색 완료: requested=%d, found=%d", len(node_ids), len(search_results))
        return search_results

    def find_overlapping_nodes(
        self,
        user_a_node_ids: list[str],
        user_b_node_ids: list[str],
    ) -> list[str]:
        """두 사용자의 동선에서 겹치는 노드 ID를 찾는다.

        Args:
            user_a_node_ids: 사용자 A의 서브 노드 ID 리스트.
            user_b_node_ids: 사용자 B의 서브 노드 ID 리스트.

        Returns:
            겹치는 노드 ID 리스트 (순서 유지: A 기준).
        """
        b_set = set(user_b_node_ids)
        overlapping = [nid for nid in user_a_node_ids if nid in b_set]
        logger.info(
            "겹치는 노드: A=%d, B=%d, overlap=%d",
            len(user_a_node_ids), len(user_b_node_ids), len(overlapping),
        )
        return overlapping

    async def search_by_building_name(
        self,
        building_name: str,
    ) -> list[NodeSearchResult]:
        """건물 이름으로 해당 건물의 BUILDING_PLACE를 검색한다.

        Args:
            building_name: 건물 이름 (예: "북악관").

        Returns:
            해당 건물의 NodeSearchResult 리스트.
        """
        collection = self._vector_store._get_collection(COLLECTION_VENUES)

        results = await asyncio.to_thread(
            collection.get,
            where={"building_name": building_name},
            include=["documents", "metadatas"],
        )

        search_results: list[NodeSearchResult] = []

        if results and results["ids"]:
            ids = results["ids"]
            documents = results["documents"] if results["documents"] else [""] * len(ids)
            metadatas = results["metadatas"] if results["metadatas"] else [{}] * len(ids)

            for node_id, doc, meta in zip(ids, documents, metadatas):
                search_results.append(
                    NodeSearchResult(
                        node_id=node_id,
                        name=meta.get("name", ""),
                        type_activity=meta.get("type_activity", ""),
                        description=meta.get("description", ""),
                        operating_hours=meta.get("operating_hours", ""),
                    )
                )

        logger.info("건물 '%s' place 검색 완료: %d개", building_name, len(search_results))
        return search_results
