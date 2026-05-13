"""미션 응답 파서 및 폴백 로직.

Bedrock JSON 응답을 구조화된 Mission 객체로 파싱하고,
파싱 실패 또는 서비스 장애 시 폴백 미션을 생성한다.

Requirements: 4.3, 5.1, 5.2, 5.3
"""

from __future__ import annotations

import json

from app.common.logging import get_logger
from app.features.mission.models import IntersectionInfo, Mission, VenueResult

logger = get_logger(__name__)

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


def parse_mission_response(raw_response: str) -> Mission:
    """Bedrock JSON 응답을 Mission 객체로 파싱한다.

    응답에서 JSON을 추출하고, 필수 필드(placeName, activity,
    recommendedTime, description)를 검증하여 Mission 모델로 변환한다.

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

    # 필수 필드 검증
    place_name = data.get("placeName", "")
    activity = data.get("activity", "")
    recommended_time = data.get("recommendedTime", "")
    description = data.get("description", "")

    if not place_name:
        raise ValueError("placeName 필드가 비어있습니다")

    if not activity:
        raise ValueError("activity 필드가 비어있습니다")

    if not recommended_time:
        raise ValueError("recommendedTime 필드가 비어있습니다")

    if not description:
        raise ValueError("description 필드가 비어있습니다")

    return Mission(
        placeName=place_name,
        activity=activity,
        recommendedTime=recommended_time,
        description=description,
    )


def generate_fallback_mission(
    venues: list[VenueResult],
    intersection_info: IntersectionInfo,
) -> Mission:
    """폴백 미션을 생성한다.

    Bedrock 호출 실패 또는 응답 파싱 실패 시 사용한다.
    검색 결과가 있으면 상위 1개 장소 기반으로, 없으면 동선 정보만으로
    기본 미션을 생성한다.

    Args:
        venues: ChromaDB 검색 결과 장소 리스트 (빈 리스트 가능).
        intersection_info: 동선 교집합 정보.

    Returns:
        폴백 Mission 객체.
    """
    # 추천 시간대 결정
    recommended_time = _get_fallback_time(intersection_info)

    # 검색 결과가 있으면 상위 1개 장소 기반 폴백
    if venues:
        return _fallback_from_venue(venues[0], recommended_time)

    # ChromaDB 실패 시 동선 정보만으로 폴백
    return _fallback_from_intersection(intersection_info, recommended_time)


def _fallback_from_venue(
    venue: VenueResult,
    recommended_time: str,
) -> Mission:
    """검색 결과 상위 1개 장소 기반 폴백 미션을 생성한다.

    Args:
        venue: 상위 1개 검색 결과 장소.
        recommended_time: 추천 시간대.

    Returns:
        장소 기반 폴백 Mission 객체.
    """
    metadata = venue.metadata or {}
    place_name = metadata.get("place_name", venue.venueId)

    logger.info("장소 기반 폴백 미션 생성: place=%s", place_name)

    return Mission(
        placeName=place_name,
        activity="함께 시간 보내기",
        recommendedTime=recommended_time,
        description=f"{place_name}에서 가볍게 만나서 이야기를 나눠보세요. 부담 없이 서로를 알아가는 시간이 될 거예요.",
    )


def _fallback_from_intersection(
    intersection_info: IntersectionInfo,
    recommended_time: str,
) -> Mission:
    """동선 정보만으로 폴백 미션을 생성한다.

    ChromaDB 검색 실패 시 요청 메시지에 포함된 동선 교집합 정보만으로
    기본 미션을 생성한다.

    Args:
        intersection_info: 동선 교집합 정보.
        recommended_time: 추천 시간대.

    Returns:
        동선 기반 폴백 Mission 객체.
    """
    # 건물 정보가 있으면 활용
    if intersection_info.buildingIds:
        building = intersection_info.buildingIds[0]
        place_name = f"{building} 근처"
    else:
        place_name = "캠퍼스 내"

    day_name = _DAY_NAMES.get(intersection_info.dayOfWeek, "")
    day_context = f"{day_name} " if day_name else ""

    logger.info("동선 기반 폴백 미션 생성: place=%s", place_name)

    return Mission(
        placeName=place_name,
        activity="캠퍼스 산책하며 대화 나누기",
        recommendedTime=recommended_time,
        description=f"{day_context}{recommended_time}에 {place_name}에서 만나보세요. 가볍게 산책하면서 서로의 이야기를 나눠보는 건 어떨까요?",
    )


def _get_fallback_time(intersection_info: IntersectionInfo) -> str:
    """동선 정보에서 추천 시간대를 추출한다.

    공통 시간대가 있으면 첫 번째 시간대를 사용하고,
    없으면 기본값을 반환한다.

    Args:
        intersection_info: 동선 교집합 정보.

    Returns:
        추천 시간대 문자열.
    """
    if intersection_info.timeSlots:
        return intersection_info.timeSlots[0]

    return "14:00-15:00"


def _extract_json(raw_response: str) -> str:
    """원시 응답에서 JSON 문자열을 추출한다.

    Bedrock 응답에 JSON 외 텍스트가 포함될 수 있으므로,
    중괄호로 시작하는 JSON 블록을 찾아 추출한다.

    Args:
        raw_response: 원시 텍스트 응답.

    Returns:
        추출된 JSON 문자열.

    Raises:
        ValueError: JSON을 찾을 수 없는 경우.
    """
    text = raw_response.strip()

    # 코드 블록 내 JSON 처리 (```json ... ```)
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
