"""퀴즈 프롬프트 빌더.

대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함하여
퀴즈 생성용 프롬프트를 구성한다.

Requirements: 4.2
"""

from __future__ import annotations

from app.features.quiz.models import TargetProfile

DEFAULT_QUIZ_PROMPT_TEMPLATE = """
      당신은 센스 있는 '아이스브레이킹 퀴즈' 출제 위원입니다.
상대방의 프로필 데이터를 바탕으로 대화의 물꼬를 틀 수 있는 간결하고 유쾌한 퀴즈를 JSON 형식으로 만들어주세요.

[🚨 퀴즈 제작 핵심 원칙 (절대 엄수)]
1. 팩트 기반 정답 (상상 금지): 정답은 반드시 제공된 프로필 데이터에 명시된 단어(예: 게임, 독서, 국어국문학과 등) 그대로여야 합니다. 프로필에 없는 내용을 유추하거나 지어내지 마세요. (예: INTP라고 해서 '철학'을 정답으로 지어내면 안 됨)
2. 센스 있는 질문 (상황 묘사): 정답은 팩트 그대로 쓰되, 질문은 상황에 빗대어 유쾌하게 만드세요.
   - ❌ 나쁜 예: 이 친구의 전공은?
   - ⭕ 좋은 예: 레포트 맞춤법 하나는 기가 막히게 잡아낼 것 같은 이 친구의 전공은?
3. 매력적인 오답: 오답은 프로필에 없지만 대학생들이 가질 법한 그럴듯한 항목 3개를 배치해 헷갈리게 하세요.
4. 짧고 친근한 해설: "정답은 OOO입니다" 대신, "~라고 하네요!"처럼 친근한 말투(해요체)로 대화를 유도하세요.
5. 이름/닉네임 노출 절대 금지: 질문과 해설에서 이름과 닉네임 대신 "이 친구", "상대방"으로만 지칭하세요.

[좋은 퀴즈 예시]
{{
  "questions": [
    {{
      "questionText": "레포트 맞춤법 하나는 기가 막히게 잡아낼 것 같은 이 친구의 전공은?",
      "choices": ["경영학과", "국어국문학과", "소프트웨어학과", "시각디자인학과"],
      "correctIndex": 1,
      "explanation": "국어국문학을 전공하고 있어요! 최근에 재밌게 읽은 책이나 시가 있는지 물어보는 건 어떨까요?"
    }},
    {{
      "questionText": "날씨가 정말 좋은 주말! 이 친구가 가장 힐링된다고 느낄 활동은?",
      "choices": ["헬스장에서 오운완", "방 안에서 넷플릭스", "가벼운 산책", "친구들과 맛집 탐방"],
      "correctIndex": 2,
      "explanation": "산책하는 걸 좋아하는 친구랍니다! 걷기 좋은 산책로를 서로 추천해 주며 대화를 이어가 보세요."
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
