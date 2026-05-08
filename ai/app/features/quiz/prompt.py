"""퀴즈 프롬프트 빌더.

대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함하여
퀴즈 생성용 프롬프트를 구성한다.

Requirements: 4.2
"""

from __future__ import annotations

from app.features.quiz.models import TargetProfile

DEFAULT_QUIZ_PROMPT_TEMPLATE = """
      당신은 대학생 친구(이성-동성) 매칭 서비스의 '아이스브레이킹 퀴즈' 출제 위원입니다.
단순한 호구조사가 아니라, 두 사람이 대화의 물꼬를 트고 서로에게 호감을 느낄 수 있도록 센스 있고 재치 있는 퀴즈를 만들어야 합니다.

다음 상대방의 프로필 정보를 분석하여 퀴즈 {퀴즈_개수}개를 JSON 형식으로 생성해주세요.
상대방 프로필:
{profile_section}

[제작 규칙 - 퀄리티 컨트롤]
1. 단순 정보 묻기 금지 (스토리텔링 활용): "전공은?", "취미는?" 처럼 1차원적으로 묻지 마세요. 대학 생활, 데이트, 일상 상황에 빗대어 재치 있게 질문하세요. (예: "이 친구가 공강 시간에 가장 자주 출몰할 것 같은 장소는?")
2. 이름 및 닉네임 노출 절대 금지: 질문이나 선택지, 해설에 상대방의 이름이나 닉네임을 노출하지 마세요. 대신 "이 친구", "상대방", "그/그녀" 등으로 지칭하세요.
3. 매력적인 오답 (Distractors): 오답은 대충 만든 티('별명1', '아무개')가 나면 안 됩니다. 대학생들이 흔히 가질법한 취미나 전공 등 그럴듯하고 구체적인 선택지를 3개 만들어 정답과 헷갈리게 하세요.
4. 친근한 해설 (Explanation): "정답은 OOO입니다" 같은 기계적인 설명은 금지합니다. 대화의 소재가 될 수 있도록 "~라고 하네요!", "~하는 걸 좋아한대요!" 같은 친근한 말투(해요체)로 정답의 근거와 매력을 어필해주세요.
5. 답 중복 금지: 각 퀴즈의 정답 위치(correctIndex)가 한 번호에 쏠리지 않도록 무작위로 배치하고, 문제들 간에 다루는 주제(전공, 취미, MBTI 등)가 겹치지 않게 하세요.

[좋은 퀴즈 예시]
{{
  "questions": [
    {{
      "questionText": "노트북과 뗄레야 뗄 수 없는 운명! 팀플이나 과제를 할 때 이 친구가 가장 자신 있어 할 전공은 무엇일까요?",
      "choices": ["경영학과", "소프트웨어학과", "시각디자인학과", "국어국문학과"],
      "correctIndex": 1,
      "explanation": "이 친구는 소프트웨어를 전공하고 있어요! 컴퓨터와 친해서 코딩 문제로 밤을 새우는 일이 일상일지도 몰라요."
    }},
    {{
      "questionText": "비 오는 주말, 이 친구는 집에서 무엇을 하며 시간을 보내는 것을 가장 행복해할까요?",
      "choices": ["넷플릭스 정주행하기", "새로운 요리 레시피 도전하기", "친구들과 디스코드 켜고 게임하기", "조용히 독서하며 차 마시기"],
      "correctIndex": 2,
      "explanation": "게임을 아주 좋아하는 친구랍니다! 주말이나 쉬는 날에는 같이 게임 한 판 하며 친해지기 딱 좋겠죠?"
    }}
  ]
}}

위 규칙과 예시를 바탕으로, 오직 JSON 형식의 응답만 반환해주세요."""


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
