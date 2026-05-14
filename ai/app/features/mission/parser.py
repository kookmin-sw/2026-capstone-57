"""미션 응답 파서.

Bedrock JSON 응답을 Mission 객체로 파싱한다.
"""

from __future__ import annotations

import json
import logging

from app.features.mission.models import Mission

logger = logging.getLogger(__name__)


def parse_mission_response(raw_response: str) -> Mission:
    """Bedrock JSON 응답을 Mission 객체로 파싱한다.

    Args:
        raw_response: Bedrock에서 반환된 원시 텍스트 응답.

    Returns:
        파싱된 Mission 객체.

    Raises:
        ValueError: JSON 파싱 실패 또는 필수 필드 누락 시.
    """
    json_str = _extract_json(raw_response)
    data = json.loads(json_str)

    if not isinstance(data, dict):
        raise ValueError("미션 응답이 JSON 객체가 아닙니다")

    location = data.get("location", "")
    activity = data.get("activity", "")
    description = data.get("description", "")
    selected_node_id = data.get("selectedNodeId", "")

    if not location:
        raise ValueError("location 필드가 비어있습니다")
    if not activity:
        raise ValueError("activity 필드가 비어있습니다")
    if not description:
        raise ValueError("description 필드가 비어있습니다")
    if not selected_node_id:
        raise ValueError("selectedNodeId 필드가 비어있습니다")

    return Mission(
        location=location,
        activity=activity,
        description=description,
        selectedNodeId=selected_node_id,
    )


def _extract_json(raw_response: str) -> str:
    """원시 응답에서 JSON 문자열을 추출한다."""
    text = raw_response.strip()

    # 코드 블록 내 JSON 처리
    if "```json" in text:
        start = text.index("```json") + len("```json")
        end = text.index("```", start)
        text = text[start:end].strip()
    elif "```" in text:
        start = text.index("```") + len("```")
        end = text.index("```", start)
        text = text[start:end].strip()

    # 첫 번째 { 찾기
    json_start = -1
    for i, char in enumerate(text):
        if char == "{":
            json_start = i
            break

    if json_start == -1:
        raise ValueError("응답에서 JSON을 찾을 수 없습니다")

    # 매칭되는 닫는 괄호 찾기
    depth = 0
    json_end = -1
    for i in range(json_start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                json_end = i + 1
                break

    if json_end == -1:
        raise ValueError("JSON 괄호가 올바르게 닫히지 않았습니다")

    return text[json_start:json_end]
