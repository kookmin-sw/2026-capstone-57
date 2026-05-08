"""퀴즈 프롬프트 빌더.

대상 프로필의 모든 비어있지 않은 필드를 프롬프트에 포함하여
퀴즈 생성용 프롬프트를 구성한다.

Requirements: 4.2
"""

from __future__ import annotations

from app.features.quiz.models import TargetProfile

DEFAULT_QUIZ_PROMPT_TEMPLATE = """
      당신은 대학생 친구(이성-동성친구 모두) 매칭 서비스의 퀴즈 생성 AI입니다.
      상대방의 프로필 정보를 기반으로 재미있고 자연스러운 퀴즈 문항을 생성해주세요.
      
      상대방 프로필:
      {profile_section}
      
      다음 조건을 만족하는 퀴즈 5개를 JSON 형식으로 생성해주세요:
      1. 각 문제는 상대방에 대해 알아가는 데 도움이 되는 내용이어야 합니다
      2. 각 문제는 4개의 선택지를 가져야 합니다
      3. 정답은 상대방의 프로필 정보를 기반으로 해야 합니다
      4. 오답은 그럴듯하지만 명확히 구분 가능해야 합니다
      5. 각 문제에 정답의 근거를 설명하는 explanation을 포함해주세요
      6. 이름을 직접적으로 퀴즈 내용에 추가하지 않아야 합니다

      예시 : 
       {{
        "questions": [
          {{
            "questionText": "상대방의 MBTI 맨 앞 글자는?",
            "choices": ["I", "E],
            "correctIndex": 0,
            "explanation": "상대방은 밖에 있는걸 더 좋아해요."
          }}
        ]
      }}

      
      응답 형식 (JSON만 반환):
      {{
        "questions": [
          {{
            "questionText": "문제 텍스트",
            "choices": ["선택지1", "선택지2", "선택지3", "선택지4"],
            "correctIndex": 0,
            "explanation": "정답 근거 설명"
          }}
        ]
      }}"""


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
