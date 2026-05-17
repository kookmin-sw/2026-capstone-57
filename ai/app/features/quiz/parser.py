"""퀴즈 응답 파서 및 폴백 로직.

Bedrock JSON 응답을 구조화된 QuizQuestion 객체로 파싱하고,
파싱 실패 시 프로필 기반 폴백 퀴즈를 생성한다.

Requirements: 4.3, 4.4
"""

import json
import random
from typing import Any

from app.common.logging import get_logger
from app.features.quiz.models import QuizQuestion, TargetProfile

logger = get_logger(__name__)


def parse_quiz_response(raw_response: str) -> list[QuizQuestion]:
    """Bedrock JSON 응답을 QuizQuestion 리스트로 파싱한다.

    응답에서 JSON을 추출하고, questions 배열의 각 항목을
    QuizQuestion 모델로 변환한다.

    Args:
        raw_response: Bedrock에서 반환된 원시 텍스트 응답.

    Returns:
        파싱된 QuizQuestion 리스트.

    Raises:
        ValueError: JSON 파싱 실패 또는 필수 필드 누락 시.
    """
    json_str = _extract_json(raw_response)
    data = json.loads(json_str)

    questions_data = _extract_questions(data)
    questions = _validate_and_build_questions(questions_data)

    if not questions:
        raise ValueError("유효한 퀴즈 문제가 없습니다")

    return questions


def generate_fallback_quiz(profile: TargetProfile) -> list[QuizQuestion]:
    """프로필 기반 폴백 퀴즈를 생성한다.

    파싱 실패 시 대상 프로필 정보를 활용하여 5문제, 각 4선택지,
    correctIndex 0-3 범위의 기본 퀴즈를 생성한다.

    Args:
        profile: 퀴즈 대상 유저 프로필.

    Returns:
        5개의 QuizQuestion으로 구성된 폴백 퀴즈 리스트.
    """
    templates = _get_fallback_templates(profile)
    questions: list[QuizQuestion] = []

    for template in templates[:5]:
        correct_index = random.randint(0, 3)
        choices = template["choices"][:]
        # correctIndex 위치에 정답 배치
        correct_answer = template["correct_answer"]
        # 정답을 choices에서 제거하고 correctIndex 위치에 삽입
        if correct_answer in choices:
            choices.remove(correct_answer)
        else:
            choices = choices[:3]
        choices.insert(correct_index, correct_answer)
        # 4개로 맞추기
        choices = choices[:4]

        questions.append(
            QuizQuestion(
                questionText=template["question"],
                choices=choices,
                correctIndex=correct_index,
                explanation=template["explanation"],
            )
        )

    return questions


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

    # 첫 번째 { 또는 [ 찾기
    json_start = -1
    for i, char in enumerate(text):
        if char in ("{", "["):
            json_start = i
            break

    if json_start == -1:
        raise ValueError("응답에서 JSON을 찾을 수 없습니다")

    # 매칭되는 닫는 괄호 찾기
    open_char = text[json_start]
    close_char = "}" if open_char == "{" else "]"
    depth = 0
    json_end = -1

    for i in range(json_start, len(text)):
        if text[i] == open_char:
            depth += 1
        elif text[i] == close_char:
            depth -= 1
            if depth == 0:
                json_end = i + 1
                break

    if json_end == -1:
        raise ValueError("JSON 괄호가 올바르게 닫히지 않았습니다")

    return text[json_start:json_end]


def _extract_questions(data: Any) -> list[dict]:
    """파싱된 JSON 데이터에서 questions 배열을 추출한다.

    Args:
        data: 파싱된 JSON 데이터 (dict 또는 list).

    Returns:
        questions 딕셔너리 리스트.

    Raises:
        ValueError: questions 배열을 찾을 수 없는 경우.
    """
    if isinstance(data, list):
        return data

    if isinstance(data, dict):
        if "questions" in data:
            questions = data["questions"]
            if isinstance(questions, list):
                return questions

    raise ValueError("응답에서 questions 배열을 찾을 수 없습니다")


def _validate_and_build_questions(questions_data: list[dict]) -> list[QuizQuestion]:
    """questions 데이터를 검증하고 QuizQuestion 객체로 변환한다.

    각 문제가 필수 필드를 가지고 있는지 검증하며,
    유효하지 않은 문제는 건너뛴다.

    Args:
        questions_data: 원시 questions 딕셔너리 리스트.

    Returns:
        유효한 QuizQuestion 리스트.
    """
    questions: list[QuizQuestion] = []

    for i, q_data in enumerate(questions_data):
        try:
            if not isinstance(q_data, dict):
                logger.warning(f"문제 {i}: dict가 아닌 데이터 건너뜀")
                continue

            question_text = q_data.get("questionText", "")
            choices = q_data.get("choices", [])
            correct_index = q_data.get("correctIndex")
            explanation = q_data.get("explanation", "")

            if not question_text:
                logger.warning(f"문제 {i}: questionText가 비어있음")
                continue

            if not isinstance(choices, list) or len(choices) != 4:
                logger.warning(f"문제 {i}: choices가 4개가 아님 (got {len(choices) if isinstance(choices, list) else 'non-list'})")
                continue

            if correct_index is None or not isinstance(correct_index, int):
                logger.warning(f"문제 {i}: correctIndex가 유효하지 않음")
                continue

            if correct_index < 0 or correct_index > 3:
                logger.warning(f"문제 {i}: correctIndex 범위 초과 ({correct_index})")
                continue

            questions.append(
                QuizQuestion(
                    questionText=question_text,
                    choices=choices,
                    correctIndex=correct_index,
                    explanation=explanation,
                )
            )
        except Exception as e:
            logger.warning(f"문제 {i} 파싱 중 예외: {e}")
            continue

    return questions


def _get_fallback_templates(profile: TargetProfile) -> list[dict]:
    """프로필 기반 폴백 퀴즈 템플릿을 생성한다.

    프로필의 비어있지 않은 필드를 활용하여 기본 퀴즈 문제를 구성한다.

    Args:
        profile: 퀴즈 대상 유저 프로필.

    Returns:
        폴백 퀴즈 템플릿 리스트 (최소 5개).
    """
    templates: list[dict] = []
    name = profile.name or "친구"

    # 1. 이름 관련 문제
    templates.append(
        {
            "question": f"{name}님의 이름은 무엇일까요?",
            "correct_answer": name,
            "choices": [name, "김철수", "이영희", "박민수"],
            "explanation": f"정답은 {name}입니다.",
        }
    )

    # 2. 닉네임 관련 문제
    nickname = profile.nickname or name
    templates.append(
        {
            "question": f"{name}님의 닉네임은 무엇일까요?",
            "correct_answer": nickname,
            "choices": [nickname, "별명1", "별명2", "별명3"],
            "explanation": f"{name}님의 닉네임은 {nickname}입니다.",
        }
    )

    # 3. 대학교 관련 문제
    university = profile.university or "알 수 없음"
    templates.append(
        {
            "question": f"{name}님이 다니는 대학교는 어디일까요?",
            "correct_answer": university,
            "choices": [university, "서울대학교", "연세대학교", "고려대학교"],
            "explanation": f"{name}님은 {university}에 다닙니다.",
        }
    )

    # 4. 전공 관련 문제
    major = profile.major or "알 수 없음"
    templates.append(
        {
            "question": f"{name}님의 전공은 무엇일까요?",
            "correct_answer": major,
            "choices": [major, "컴퓨터공학", "경영학", "심리학"],
            "explanation": f"{name}님의 전공은 {major}입니다.",
        }
    )

    # 5. 취미/관심사 관련 문제
    if profile.hobbies:
        hobby = profile.hobbies[0]
        templates.append(
            {
                "question": f"{name}님의 취미는 무엇일까요?",
                "correct_answer": hobby,
                "choices": [hobby, "독서", "운동", "요리"],
                "explanation": f"{name}님의 취미 중 하나는 {hobby}입니다.",
            }
        )
    elif profile.interests:
        interest = profile.interests[0]
        templates.append(
            {
                "question": f"{name}님의 관심사는 무엇일까요?",
                "correct_answer": interest,
                "choices": [interest, "음악", "영화", "여행"],
                "explanation": f"{name}님의 관심사 중 하나는 {interest}입니다.",
            }
        )
    else:
        templates.append(
            {
                "question": f"{name}님에 대해 더 알고 싶은 것은?",
                "correct_answer": "취미",
                "choices": ["취미", "좋아하는 음식", "여행지", "영화"],
                "explanation": f"{name}님의 취미를 알아보세요!",
            }
        )

    # 템플릿이 5개 미만이면 추가 문제 생성
    while len(templates) < 5:
        templates.append(
            {
                "question": f"{name}님과 친해지려면 어떤 주제로 대화하면 좋을까요?",
                "correct_answer": "관심사",
                "choices": ["관심사", "날씨", "뉴스", "스포츠"],
                "explanation": "상대방의 관심사에 대해 이야기하면 좋습니다.",
            }
        )

    return templates
