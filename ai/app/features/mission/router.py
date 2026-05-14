"""캠퍼스 노드 인덱싱 HTTP 라우터.

Backend에서 서브 노드 정보를 등록/수정할 때 호출하여
ChromaDB venues 컬렉션에 벡터화하여 저장한다.

Endpoints:
    POST /api/campus-nodes/index - 서브 노드 인덱싱
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from app.common.logging import get_logger
from app.features.mission.models import NodeIndexRequest
from app.rag.vector_store import COLLECTION_VENUES

logger = get_logger(__name__)

router = APIRouter(prefix="/api/campus-nodes", tags=["campus-nodes"])


class IndexResponse(BaseModel):
    """인덱싱 응답."""

    nodeId: str
    status: str = "indexed"


@router.post("/index", response_model=IndexResponse)
async def index_campus_node(body: NodeIndexRequest, request: Request):
    """서브 노드를 ChromaDB에 인덱싱한다.

    노드 정보를 임베딩하여 venues 컬렉션에 저장한다.
    동일 nodeId가 이미 존재하면 덮어쓴다.
    """
    embedding_generator = request.app.state.embedding_generator
    vector_store = request.app.state.vector_store

    try:
        # embeddingText 구성
        parts = [body.name]
        if body.description:
            parts.append(body.description)
        if body.typeActivity:
            parts.append(f"유형: {body.typeActivity}")
        if body.operatingHours:
            parts.append(f"운영시간: {body.operatingHours}")

        embedding_text = " | ".join(parts)

        # 임베딩 생성
        embedding = await embedding_generator.generate(embedding_text)

        # ChromaDB에 저장 (upsert 방식: 기존 삭제 후 추가)
        try:
            await vector_store.delete_documents(
                collection_name=COLLECTION_VENUES,
                ids=[body.nodeId],
            )
        except Exception:
            pass  # 존재하지 않으면 무시

        await vector_store.add_documents(
            collection_name=COLLECTION_VENUES,
            documents=[embedding_text],
            embeddings=[embedding],
            metadatas=[{
                "name": body.name,
                "type_activity": body.typeActivity,
                "description": body.description,
                "operating_hours": body.operatingHours or "",
                "node_id": body.nodeId,
            }],
            ids=[body.nodeId],
        )

        logger.info(f"노드 인덱싱 완료: {body.nodeId} ({body.name})")

        return IndexResponse(nodeId=body.nodeId)

    except Exception as exc:
        logger.exception("노드 인덱싱 실패")
        raise HTTPException(status_code=500, detail=str(exc))
