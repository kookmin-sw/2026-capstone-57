"""미션 프롬프트 빌더.

검색된 장소 정보와 사용자 컨텍스트(프로필, 동선)를 기반으로
Bedrock Claude에 전송할 미션 생성 프롬프트를 구성한다.

Requirements: 4.1, 4.2, 4.4, 4.5, 4.6
"""

from __future__ import annotations

from app.features.mission.models import IntersectionInfo, UserProfile, VenueResult

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

DEFAULT_MISSION_PROMPT_TEMPLATE = """당신은 대학생 매칭 서비스의 오프라인 미션 설계자입니다.
매칭된 두 사용자가 자연스럽게 오프라인에서 만날 수 있도록, 구체적인 장소와 활동을 포함한 미션을 생성해주세요.

[미션 생성 핵심 원칙]
1. 부담 없는 활동: 대학생이 처음 만나도 어색하지 않을 가벼운 활동을 제안하세요.
2. 구체적 장소: 반드시 제공된 장소 목록 중 하나를 선택하세요.
3. 시간대 고려: 두 사용자의 공통 시간대에 맞는 활동을 제안하세요.
4. 관심사 반영: 두 사용자의 관심사와 성격을 고려하여 자연스러운 활동을 제안하세요.
5. 친근한 톤: 미션 설명은 친근하고 자연스러운 해요체로 작성하세요.

[검색된 캠퍼스 장소 (상위 {venue_count}개)]
{venues_section}

[사용자 동선 정보]
{intersection_section}

[사용자 프로필]
{profiles_section}

[출력 형식]
반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
{{
  "placeName": "선택한 장소명",
  "activity": "구체적인 활동 (예: 커피 마시면서 가벼운 대화 나누기)",
  "recommendedTime": "추천 시간대 (예: 14:00-15:00)",
  "description": "미션 설명 (2~3문장, 해요체, 두 사용자의 맥락을 반영)"
}}
"""


def build_venues_section(venues: list[VenueResult]) -> str:
    """검색된 장소 목록을 프롬프트 텍스트로 구성한다.

    Args:
        venues: ChromaDB 검색 결과 장소 리스트.

    Returns:
        장소 정보가 포함된 텍스트 문자열.
    """
    if not venues:
        return "(검색된 장소 없음)"

    lines: list[str] = []

    for i, venue in enumerate(venues, start=1):
        metadata = venue.metadata or {}
        place_name = metadata.get("place_name", venue.venueId)
        venue_type = metadata.get("type", "")
        operating_hours = metadata.get("operating_hours", "")
        meeting_suitability = metadata.get("meeting_suitability", "")

        line = f"{i}. {place_name}"
        details: list[str] = []

        if venue_type:
            details.append(f"유형: {venue_type}")
        if operating_hours:
            details.append(f"운영시간: {operating_hours}")
        if meeting_suitability:
            details.append(f"만남적합도: {meeting_suitability}/5")

        # document에서 추가 정보 추출 (embeddingText 형식)
        if venue.document:
            parts = venue.document.split(" | ")
            if len(parts) >= 2:
                details.append(f"설명: {parts[1]}")
            if len(parts) >= 6:
                details.append(f"추천활동: {parts[5]}")

        if details:
            line += f" ({', '.join(details)})"

        lines.append(line)

    return "\n".join(lines)


def build_intersection_section(intersection_info: IntersectionInfo) -> str:
    """동선 교집합 정보를 프롬프트 텍스트로 구성한다.

    Args:
        intersection_info: 동선 교집합 정보.

    Returns:
        동선 정보가 포함된 텍스트 문자열.
    """
    lines: list[str] = []

    day_name = _DAY_NAMES.get(intersection_info.dayOfWeek, "")
    if day_name:
        lines.append(f"- 요일: {day_name}")

    if intersection_info.timeSlots:
        time_str = ", ".join(intersection_info.timeSlots)
        lines.append(f"- 공통 시간대: {time_str}")

    if intersection_info.buildingIds:
        building_str = ", ".join(intersection_info.buildingIds)
        lines.append(f"- 근처 건물: {building_str}")

    if not lines:
        return "(동선 정보 없음)"

    return "\n".join(lines)


def build_profiles_section(user_profiles: list[UserProfile]) -> str:
    """사용자 프로필 목록을 프롬프트 텍스트로 구성한다.

    Args:
        user_profiles: 두 사용자의 프로필 정보.

    Returns:
        프로필 정보가 포함된 텍스트 문자열.
    """
    lines: list[str] = []

    for i, profile in enumerate(user_profiles, start=1):
        lines.append(f"사용자 {i}:")

        if profile.interests:
            lines.append(f"  - 관심사: {', '.join(profile.interests)}")

        if profile.personalityType:
            lines.append(f"  - 성격 유형: {', '.join(profile.personalityType)}")

        if not profile.interests and not profile.personalityType:
            lines.append("  - (프로필 정보 없음)")

    return "\n".join(lines)


def build_mission_prompt(
    venues: list[VenueResult],
    intersection_info: IntersectionInfo,
    user_profiles: list[UserProfile],
    template: str | None = None,
) -> str:
    """검색된 장소 + 사용자 컨텍스트로 미션 생성 프롬프트를 구성한다.

    검색된 상위 5개 장소 정보와 두 사용자의 동선/프로필 컨텍스트를
    프롬프트에 포함하여 Bedrock Claude에 전송할 완성된 프롬프트를 생성한다.

    Args:
        venues: ChromaDB 검색 결과 장소 리스트 (상위 5개).
        intersection_info: 동선 교집합 정보 (시간대, 건물 위치, 요일).
        user_profiles: 두 사용자의 프로필 정보.
        template: 커스텀 프롬프트 템플릿. None이면 기본 템플릿 사용.

    Returns:
        Bedrock에 전송할 완성된 프롬프트 문자열.
    """
    venues_section = build_venues_section(venues)
    intersection_section = build_intersection_section(intersection_info)
    profiles_section = build_profiles_section(user_profiles)

    prompt_template = template if template is not None else DEFAULT_MISSION_PROMPT_TEMPLATE

    return prompt_template.format(
        venue_count=len(venues),
        venues_section=venues_section,
        intersection_section=intersection_section,
        profiles_section=profiles_section,
    )
