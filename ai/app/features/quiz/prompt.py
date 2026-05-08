"""퀴즈 프롬프트 빌더.

대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함하여
퀴즈 생성용 프롬프트를 구성한다.

Requirements: 4.2
"""

from __future__ import annotations

from app.features.quiz.models import TargetProfile

DEFAULT_QUIZ_PROMPT_TEMPLATE = """당신은 친구 관계를 위한 퀴즈를 만드는 AI입니다.
아래 프로필 정보를 바탕으로 상대방에 대한 퀴즈 5문제를 생성해주세요.

각 문제는 4개의 선택지를 가지며, 정답은 하나입니다.

## 대상 프로필 정보
{profile_section}

## 출력 형식
다음 JSON 형식으로 정확히 응답해주세요:
{{
  "questions": [
    {{
      "questionText": "질문 내용",
      "choices": ["선택지1", "선택지2", "선택지3", "선택지4"],
      "correctIndex": 0,
      "explanation": "정답 설명"
    }}
  ]
}}

반드시 5개의 문제를 생성하고, correctIndex는 0~3 사이의 값이어야 합니다.
JSON만 출력하고 다른 텍스트는 포함하지 마세요."""


def build_profile_section(profile: TargetProfile) -> str:
    """프로필의 비어있지 않은 필드를 텍스트 섹션으로 구성한다.

    Args:
        profile: 퀴즈 대상 유저 프로필.

    Returns:
        프로필 정보가 포함된 텍스트 문자열.
    """
    lines: list[str] = []

    if profile.name:
        lines.append(f"- 이름: {profile.name}")

    if profile.nickname:
        lines.append(f"- 닉네임: {profile.nickname}")

    if profile.university:
        lines.append(f"- 대학교: {profile.university}")

    if profile.major:
        lines.append(f"- 전공: {profile.major}")

    if profile.hobbies:
        lines.append(f"- 취미: {', '.join(profile.hobbies)}")

    if profile.interests:
        lines.append(f"- 관심사: {', '.join(profile.interests)}")

    if profile.personalityType:
        lines.append(f"- 성격 유형: {', '.join(profile.personalityType)}")

    return "\n".join(lines)


def build_quiz_prompt(
    profile: TargetProfile,
    template: str | None = None,
) -> str:
    """프로필 기반 퀴즈 생성 프롬프트를 구성한다.

    대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함한다.
    설정 가능한 프롬프트 템플릿을 지원하며, 템플릿이 제공되지 않으면
    기본 템플릿을 사용한다.

    Args:
        profile: 퀴즈 대상 유저 프로필.
        template: 커스텀 프롬프트 템플릿. `{profile_section}` 플레이스홀더를
            포함해야 한다. None이면 기본 템플릿 사용.

    Returns:
        Bedrock에 전송할 완성된 프롬프트 문자열.
    """
    profile_section = build_profile_section(profile)
    prompt_template = template if template is not None else DEFAULT_QUIZ_PROMPT_TEMPLATE

    return prompt_template.format(profile_section=profile_section)
