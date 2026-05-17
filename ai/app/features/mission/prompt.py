"""미션 프롬프트 빌더.

검색된 서브 노드 정보와 동선 컨텍스트를 기반으로
Bedrock Claude에 전송할 미션 생성 프롬프트를 구성한다.
"""

from __future__ import annotations

from app.features.mission.models import MissionRequestMessage
from app.features.mission.search import NodeSearchResult


def build_mission_prompt(
    nodes: list[NodeSearchResult],
    request: MissionRequestMessage,
) -> str:
    """미션 생성 프롬프트를 구성한다.

    Args:
        nodes: 겹치는 서브 노드의 상세 정보 리스트.
        request: 미션 생성 요청 메시지.

    Returns:
        Bedrock에 전송할 프롬프트 문자열.
    """
    # 노드 정보 섹션
    nodes_section = _build_nodes_section(nodes)

    # 동선 정보 섹션
    route_section = _build_route_section(request)

    return f"""당신은 대학생 매칭 서비스의 오프라인 미션 설계자입니다.
매칭된 두 사용자가 자연스럽게 오프라인에서 만날 수 있도록, 구체적인 장소와 활동을 포함한 미션을 생성해주세요.

[미션 생성 핵심 원칙]
1. 동선 우선: 반드시 두 사용자의 동선이 겹치는 장소 중 하나를 선택하세요.
2. 부담 없는 활동: 대학생이 처음 만나도 어색하지 않을 가벼운 활동을 제안하세요.
3. 구체적 장소: 반드시 제공된 장소 목록 중 하나를 선택하세요.
4. 시간대 고려: 주어진 시간대에 맞는 활동을 제안하세요.
5. 친근한 톤: 미션 설명은 친근하고 자연스러운 해요체로 작성하세요.

[겹치는 장소 목록]
{nodes_section}

[동선 정보]
{route_section}

[시간대]
{request.timeSlot}

[출력 형식]
반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
selectedNodeId는 반드시 위 장소 목록의 nodeId 중 하나여야 합니다.

{{
  "location": "선택한 장소명",
  "activity": "구체적인 활동 (예: 커피 마시면서 가벼운 대화 나누기)",
  "description": "미션 설명 (2~3문장, 해요체)",
  "selectedNodeId": "선택한 장소의 nodeId"
}}
"""


def _build_nodes_section(nodes: list[NodeSearchResult]) -> str:
    """노드 목록을 프롬프트 텍스트로 구성."""
    if not nodes:
        return "(겹치는 장소 없음)"

    lines: list[str] = []
    for i, node in enumerate(nodes, start=1):
        parts = [f"{i}. {node.name} (nodeId: {node.node_id})"]
        if node.type_activity:
            parts.append(f"   유형: {node.type_activity}")
        if node.description:
            parts.append(f"   설명: {node.description}")
        if node.operating_hours:
            parts.append(f"   운영시간: {node.operating_hours}")
        lines.append("\n".join(parts))

    return "\n".join(lines)


def _build_route_section(request: MissionRequestMessage) -> str:
    """동선 정보를 프롬프트 텍스트로 구성."""
    lines = [
        f"- 사용자 A: {request.userARoute.fromBuilding.name} → {request.userARoute.toBuilding.name}",
        f"- 사용자 B: {request.userBRoute.fromBuilding.name} → {request.userBRoute.toBuilding.name}",
    ]
    return "\n".join(lines)
