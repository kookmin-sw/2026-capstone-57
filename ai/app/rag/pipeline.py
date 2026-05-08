"""RAG 기반 컨텍스트 검색 파이프라인.

벡터 DB를 활용하여 과거 회고/일기 데이터를 검색하고,
새로운 문서를 임베딩하여 저장한다.

검색 (Read Path):
    쿼리 텍스트 → 임베딩 생성 → 벡터 유사도 검색 → user_id 필터 → 상위 K개 반환

저장 (Write Path):
    텍스트 → 청킹 (500자, 100자 오버랩) → 임베딩 생성 → ChromaDB 저장

사용 예시:
    from app.rag.pipeline import RAGPipeline
    from app.rag.embeddings import EmbeddingGenerator
    from app.rag.vector_store import VectorStore
    from app.config import get_settings

    settings = get_settings()
    embedding_generator = EmbeddingGenerator(settings)
    vector_store = VectorStore(settings)
    pipeline = RAGPipeline(embedding_generator, vector_store, settings)

    # 검색
    results = await pipeline.search(
        user_id="user-123",
        query="오늘 만남은 어땠나요?",
        collection="retrospectives",
    )

    # 저장
    await pipeline.store(
        user_id="user-123",
        content="오늘 친구와 카페에서 만났다...",
        collection="retrospectives",
        metadata={"matched_user_id": "user-456", "created_at": "2024-01-01"},
    )
"""

from __future__ import annotations

import logging
import uuid
from dataclasses import dataclass

from app.config import Settings
from app.rag.embeddings import EmbeddingGenerator
from app.rag.vector_store import SearchResult, VectorStore

logger = logging.getLogger(__name__)

# 청킹 기본 설정
DEFAULT_CHUNK_SIZE = 500
DEFAULT_CHUNK_OVERLAP = 100


@dataclass
class Document:
    """RAG 검색 결과 문서.

    Attributes:
        content: 문서 텍스트 내용
        metadata: 문서 메타데이터 (user_id, created_at 등)
        score: 유사도 점수 (0에 가까울수록 유사)
    """

    content: str
    metadata: dict
    score: float


class RAGPipeline:
    """RAG 기반 컨텍스트 검색 파이프라인.

    임베딩 생성기와 벡터 스토어를 조합하여
    문서 저장 및 유사도 검색 기능을 제공한다.

    Attributes:
        top_k: 검색 시 반환할 최대 문서 수
        chunk_size: 텍스트 청킹 시 청크 크기 (문자 수)
        chunk_overlap: 청크 간 오버랩 크기 (문자 수)
    """

    def __init__(
        self,
        embedding_generator: EmbeddingGenerator,
        vector_store: VectorStore,
        settings: Settings,
    ) -> None:
        self._embedding_generator = embedding_generator
        self._vector_store = vector_store
        self.top_k = settings.rag_top_k
        self.chunk_size = DEFAULT_CHUNK_SIZE
        self.chunk_overlap = DEFAULT_CHUNK_OVERLAP

    async def search(
        self,
        user_id: str,
        query: str,
        collection: str,
        top_k: int | None = None,
    ) -> list[Document]:
        """유저별 과거 문서를 벡터 유사도로 검색한다.

        쿼리 텍스트를 임베딩으로 변환한 후, 벡터 스토어에서
        user_id 필터를 적용하여 유사한 문서를 검색한다.

        Args:
            user_id: 검색 대상 사용자 ID
            query: 검색 쿼리 텍스트
            collection: 검색 대상 컬렉션 ("retrospectives" 또는 "diaries")
            top_k: 반환할 최대 결과 수 (None이면 설정값 사용)

        Returns:
            유사도 순으로 정렬된 Document 리스트

        Raises:
            BedrockInvocationError: 임베딩 생성 실패 시
            ValueError: 유효하지 않은 컬렉션 이름인 경우
        """
        k = top_k if top_k is not None else self.top_k

        logger.info(
            "RAG 검색 시작: user_id=%s, collection=%s, top_k=%d",
            user_id,
            collection,
            k,
        )

        # 쿼리 임베딩 생성
        query_embedding = await self._embedding_generator.generate(query)

        # 벡터 유사도 검색 (user_id 필터 적용)
        search_results: list[SearchResult] = await self._vector_store.search(
            collection_name=collection,
            query_embedding=query_embedding,
            user_id=user_id,
            top_k=k,
        )

        # SearchResult → Document 변환
        documents = [
            Document(
                content=result.document,
                metadata=result.metadata,
                score=result.distance,
            )
            for result in search_results
        ]

        logger.info(
            "RAG 검색 완료: user_id=%s, collection=%s, results=%d",
            user_id,
            collection,
            len(documents),
        )

        return documents

    async def store(
        self,
        user_id: str,
        content: str,
        collection: str,
        metadata: dict | None = None,
    ) -> None:
        """완성된 문서를 벡터 DB에 임베딩하여 저장한다.

        텍스트를 청크로 분할한 후, 각 청크의 임베딩을 생성하고
        ChromaDB에 저장한다. 모든 청크에 user_id 메타데이터가 포함된다.

        Args:
            user_id: 문서 소유자 사용자 ID
            content: 저장할 문서 전체 텍스트
            collection: 저장 대상 컬렉션 ("retrospectives" 또는 "diaries")
            metadata: 추가 메타데이터 (created_at, matched_user_id 등)

        Raises:
            BedrockInvocationError: 임베딩 생성 실패 시
            ValueError: 유효하지 않은 컬렉션 이름이거나 빈 콘텐츠인 경우
        """
        if not content or not content.strip():
            raise ValueError("저장할 콘텐츠가 비어있습니다.")

        logger.info(
            "RAG 저장 시작: user_id=%s, collection=%s, content_length=%d",
            user_id,
            collection,
            len(content),
        )

        # 텍스트 청킹
        chunks = self._chunk_text(content)

        logger.info(
            "텍스트 청킹 완료: chunks=%d, chunk_size=%d, overlap=%d",
            len(chunks),
            self.chunk_size,
            self.chunk_overlap,
        )

        # 각 청크의 임베딩 생성
        embeddings = await self._embedding_generator.generate_batch(chunks)

        # 메타데이터 구성 (모든 청크에 user_id 포함)
        base_metadata = {"user_id": user_id}
        if metadata:
            base_metadata.update(metadata)

        # 청크별 고유 ID 및 메타데이터 생성
        doc_id = str(uuid.uuid4())
        ids = [f"{doc_id}_chunk_{i}" for i in range(len(chunks))]
        metadatas = [
            {**base_metadata, "chunk_index": i, "total_chunks": len(chunks)}
            for i in range(len(chunks))
        ]

        # ChromaDB에 저장
        await self._vector_store.add_documents(
            collection_name=collection,
            documents=chunks,
            embeddings=embeddings,
            metadatas=metadatas,
            ids=ids,
        )

        logger.info(
            "RAG 저장 완료: user_id=%s, collection=%s, chunks=%d, doc_id=%s",
            user_id,
            collection,
            len(chunks),
            doc_id,
        )

    def _chunk_text(self, text: str) -> list[str]:
        """텍스트를 고정 크기 청크로 분할한다.

        500자 단위로 분할하며, 인접 청크 간 100자 오버랩을 적용하여
        문맥 연속성을 유지한다.

        Args:
            text: 분할할 원본 텍스트

        Returns:
            청크 텍스트 리스트 (최소 1개)
        """
        text = text.strip()

        if len(text) <= self.chunk_size:
            return [text]

        chunks: list[str] = []
        start = 0

        while start < len(text):
            end = start + self.chunk_size
            chunk = text[start:end]

            if chunk.strip():
                chunks.append(chunk)

            # 다음 청크 시작 위치 = 현재 시작 + (청크 크기 - 오버랩)
            start += self.chunk_size - self.chunk_overlap

        return chunks if chunks else [text]
