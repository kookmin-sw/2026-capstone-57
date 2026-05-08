"""ChromaDB 벡터 스토어 연동.

ChromaDB 클라이언트 설정 및 컬렉션 관리를 담당한다.
`retrospectives`와 `diaries` 컬렉션을 생성/관리하며,
user_id 기반 메타데이터 필터링을 지원한다.

사용 예시:
    from app.rag.vector_store import VectorStore
    from app.config import get_settings

    settings = get_settings()
    store = VectorStore(settings)

    # 문서 저장
    await store.add_documents(
        collection_name="retrospectives",
        documents=["회고 내용..."],
        embeddings=[[0.1, 0.2, ...]],
        metadatas=[{"user_id": "user-123", "created_at": "2024-01-01"}],
        ids=["doc-1"],
    )

    # 유사도 검색
    results = await store.search(
        collection_name="retrospectives",
        query_embedding=[0.1, 0.2, ...],
        user_id="user-123",
        top_k=3,
    )
"""

import asyncio
import logging
from dataclasses import dataclass

import chromadb
from chromadb.config import Settings as ChromaSettings

from app.config import Settings

logger = logging.getLogger(__name__)

COLLECTION_RETROSPECTIVES = "retrospectives"
COLLECTION_DIARIES = "diaries"

VALID_COLLECTIONS = {COLLECTION_RETROSPECTIVES, COLLECTION_DIARIES}


@dataclass
class SearchResult:
    """벡터 유사도 검색 결과.

    Attributes:
        document: 검색된 문서 텍스트
        metadata: 문서에 연결된 메타데이터
        distance: 쿼리 벡터와의 거리 (낮을수록 유사)
    """

    document: str
    metadata: dict
    distance: float


class VectorStore:
    """ChromaDB 기반 벡터 스토어.

    ChromaDB 클라이언트를 관리하고, `retrospectives`와 `diaries`
    컬렉션에 대한 문서 저장 및 검색 기능을 제공한다.

    Attributes:
        persist_directory: ChromaDB 데이터 영속화 디렉토리 경로
    """

    def __init__(self, settings: Settings) -> None:
        self.persist_directory = settings.chroma_persist_directory
        self._client = chromadb.PersistentClient(
            path=self.persist_directory,
            settings=ChromaSettings(
                anonymized_telemetry=False,
            ),
        )
        self._collections: dict[str, chromadb.Collection] = {}
        self._initialize_collections()

    def _initialize_collections(self) -> None:
        """retrospectives와 diaries 컬렉션을 생성하거나 가져온다."""
        for name in VALID_COLLECTIONS:
            self._collections[name] = self._client.get_or_create_collection(
                name=name,
                metadata={"hnsw:space": "cosine"},
            )
            logger.info("ChromaDB 컬렉션 초기화 완료: %s", name)

    def _get_collection(self, collection_name: str) -> chromadb.Collection:
        """컬렉션 이름으로 ChromaDB 컬렉션 객체를 반환한다.

        Args:
            collection_name: 컬렉션 이름 ("retrospectives" 또는 "diaries")

        Returns:
            ChromaDB Collection 객체

        Raises:
            ValueError: 유효하지 않은 컬렉션 이름인 경우
        """
        if collection_name not in VALID_COLLECTIONS:
            raise ValueError(
                f"유효하지 않은 컬렉션: '{collection_name}'. "
                f"허용된 컬렉션: {VALID_COLLECTIONS}"
            )
        return self._collections[collection_name]

    async def add_documents(
        self,
        collection_name: str,
        documents: list[str],
        embeddings: list[list[float]],
        metadatas: list[dict],
        ids: list[str],
    ) -> None:
        """문서를 벡터 스토어에 저장한다.

        Args:
            collection_name: 대상 컬렉션 이름 ("retrospectives" 또는 "diaries")
            documents: 저장할 문서 텍스트 리스트
            embeddings: 각 문서의 임베딩 벡터 리스트
            metadatas: 각 문서의 메타데이터 딕셔너리 리스트
                (반드시 "user_id" 키를 포함해야 함)
            ids: 각 문서의 고유 ID 리스트

        Raises:
            ValueError: 유효하지 않은 컬렉션 이름이거나 입력 길이가 불일치하는 경우
        """
        if not (len(documents) == len(embeddings) == len(metadatas) == len(ids)):
            raise ValueError(
                "documents, embeddings, metadatas, ids의 길이가 동일해야 합니다. "
                f"documents={len(documents)}, embeddings={len(embeddings)}, "
                f"metadatas={len(metadatas)}, ids={len(ids)}"
            )

        collection = self._get_collection(collection_name)

        await asyncio.to_thread(
            collection.add,
            documents=documents,
            embeddings=embeddings,
            metadatas=metadatas,
            ids=ids,
        )

        logger.info(
            "문서 저장 완료: collection=%s, count=%d",
            collection_name,
            len(documents),
        )

    async def search(
        self,
        collection_name: str,
        query_embedding: list[float],
        user_id: str,
        top_k: int = 3,
    ) -> list[SearchResult]:
        """user_id 필터를 적용하여 벡터 유사도 검색을 수행한다.

        Args:
            collection_name: 검색 대상 컬렉션 이름
            query_embedding: 쿼리 임베딩 벡터
            user_id: 필터링할 사용자 ID
            top_k: 반환할 최대 결과 수

        Returns:
            유사도 순으로 정렬된 SearchResult 리스트

        Raises:
            ValueError: 유효하지 않은 컬렉션 이름인 경우
        """
        collection = self._get_collection(collection_name)

        results = await asyncio.to_thread(
            collection.query,
            query_embeddings=[query_embedding],
            where={"user_id": user_id},
            n_results=top_k,
            include=["documents", "metadatas", "distances"],
        )

        search_results: list[SearchResult] = []

        if results and results["documents"] and results["documents"][0]:
            documents = results["documents"][0]
            metadatas = results["metadatas"][0] if results["metadatas"] else [{}] * len(documents)
            distances = results["distances"][0] if results["distances"] else [0.0] * len(documents)

            for doc, meta, dist in zip(documents, metadatas, distances):
                search_results.append(
                    SearchResult(
                        document=doc,
                        metadata=meta or {},
                        distance=dist,
                    )
                )

        logger.info(
            "벡터 검색 완료: collection=%s, user_id=%s, results=%d",
            collection_name,
            user_id,
            len(search_results),
        )

        return search_results

    async def delete_documents(
        self,
        collection_name: str,
        ids: list[str],
    ) -> None:
        """문서를 벡터 스토어에서 삭제한다.

        Args:
            collection_name: 대상 컬렉션 이름
            ids: 삭제할 문서 ID 리스트

        Raises:
            ValueError: 유효하지 않은 컬렉션 이름인 경우
        """
        collection = self._get_collection(collection_name)

        await asyncio.to_thread(
            collection.delete,
            ids=ids,
        )

        logger.info(
            "문서 삭제 완료: collection=%s, count=%d",
            collection_name,
            len(ids),
        )

    async def count(self, collection_name: str) -> int:
        """컬렉션의 총 문서 수를 반환한다.

        Args:
            collection_name: 대상 컬렉션 이름

        Returns:
            컬렉션에 저장된 문서 수

        Raises:
            ValueError: 유효하지 않은 컬렉션 이름인 경우
        """
        collection = self._get_collection(collection_name)
        result = await asyncio.to_thread(collection.count)
        return result

    def reset(self) -> None:
        """모든 컬렉션을 삭제하고 재생성한다.

        주의: 테스트 용도로만 사용할 것. 모든 데이터가 삭제된다.
        """
        for name in VALID_COLLECTIONS:
            self._client.delete_collection(name)
        self._collections.clear()
        self._initialize_collections()
        logger.warning("모든 벡터 스토어 컬렉션이 초기화되었습니다.")
