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

    source에 따라 임베딩 텍스트를 다르게 구성한다:
    - VENUE: "{name} | 외부 장소 | {description} | {typeActivity} | {operatingHours}"
    - BUILDING_PLACE: "{name} | {buildingName} {floor}층 | {description} | {typeActivity} | {operatingHours}"
    """
    embedding_generator = request.app.state.embedding_generator
    vector_store = request.app.state.vector_store

    try:
        # source에 따라 임베딩 텍스트 구성
        type_str = ", ".join(body.typeActivity) if body.typeActivity else "OTHER"
        hours = body.operatingHours or ""

        if body.source == "BUILDING_PLACE" and body.buildingName:
            floor_str = f"{body.floor}층" if body.floor and body.floor >= 0 else f"지하 {abs(body.floor)}층" if body.floor else ""
            embedding_text = f"{body.name} | {body.buildingName} {floor_str} | {body.description} | {type_str} | {hours}"
        else:
            embedding_text = f"{body.name} | 외부 장소 | {body.description} | {type_str} | {hours}"

        # 임베딩 생성
        embedding = await embedding_generator.generate(embedding_text)

        # ChromaDB에 저장 (upsert: 기존 삭제 후 추가)
        try:
            await vector_store.delete_documents(
                collection_name=COLLECTION_VENUES,
                ids=[body.nodeId],
            )
        except Exception:
            pass

        await vector_store.add_documents(
            collection_name=COLLECTION_VENUES,
            documents=[embedding_text],
            embeddings=[embedding],
            metadatas=[{
                "name": body.name,
                "source": body.source,
                "type_activity": ", ".join(body.typeActivity),
                "description": body.description,
                "operating_hours": hours,
                "building_name": body.buildingName or "",
                "floor": body.floor if body.floor is not None else 0,
                "node_id": body.nodeId,
            }],
            ids=[body.nodeId],
        )

        logger.info(f"노드 인덱싱 완료: {body.nodeId} ({body.name}, source={body.source})")

        return IndexResponse(nodeId=body.nodeId)

    except Exception as exc:
        logger.exception("노드 인덱싱 실패")
        raise HTTPException(status_code=500, detail=str(exc))
