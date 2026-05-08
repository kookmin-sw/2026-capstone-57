"""퀴즈 프롬프트 빌더.

대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함하여
퀴즈 생성용 프롬프트를 구성한다.

Requirements: 4.2
"""

from __future__ import annotations

from app.features.quiz.models import TargetProfile

DEFAULT_QUIZ_PROMPT_TEMPLATE = """
      당신은 센스 있는 '아이스브레이킹 퀴즈' 출제 위원입니다.
상대방의 프로필을 바탕으로 대화의 물꼬를 틀 수 있는 간결하고 유쾌한 퀴즈를 JSON 형식으로 만들어주세요.

[퀴즈 제작 필수 규칙]
1. 짧고 센스 있는 질문 (핵심): "전공은?" 같이 딱딱하게 묻지 않되, 문장은 무조건 1줄 이내로 짧고 임팩트 있게 작성하세요. (예: "이 친구가 팀플에서 가장 하드캐리할 전공은?")
2. 매력적인 오답: '별명1' 처럼 무성의한 오답은 금지합니다. 다른 대학생들이 가질 법한 구체적이고 그럴듯한 오답 3개를 만드세요.
3. 짧고 친근한 해설: "정답은 OOO입니다" 대신, "~라고 하네요!"처럼 대화하기 좋은 친근한 말투(해요체)로 간결하게 쓰세요.
4. 이름/닉네임 노출 절대 금지: 질문과 해설에서 이름 대신 "이 친구", "상대방"으로만 지칭하세요.
5. 정답 번호(correctIndex)는 한 곳에 쏠리지 않게 무작위로 분배하세요.

[좋은 퀴즈 예시]
{{
  "questions": [
    {{
      "questionText": "이 친구가 팀플에서 가장 하드캐리할 전공은?",
      "choices": ["경영학과", "소프트웨어학과", "시각디자인", "국어국문"],
      "correctIndex": 1,
      "explanation": "소프트웨어를 전공하고 있어요! 코딩 이야기로 친해져 볼까요?"
    }},
    {{
      "questionText": "주말 저녁, 이 친구가 가장 즐겨할 취미는?",
      "choices": ["넷플릭스 정주행", "요리 레시피 도전", "디스코드 켜고 게임", "조용히 독서"],
      "correctIndex": 2,
      "explanation": "게임을 아주 좋아하는 친구랍니다! 같이 게임 한 판 어때요?"
    }}
  ]
}}

프로필:
{profile_section}

위 규칙과 예시를 완벽히 준수하여 오직 JSON 데이터만 반환하세요.
"""


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
