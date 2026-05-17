"""회고 프롬프트 빌더.

회고 전용 시스템 프롬프트 및 질문 생성 로직을 구현한다.
만남 맥락(matchedUserName, meetingDate, meetingLocation)과
RAG 검색 결과(과거 회고글) 컨텍스트를 포함한다.

Requirements: 6.2, 6.4
"""

from __future__ import annotations

from app.features.retrospective.models import MeetingContext
from app.rag.pipeline import Document


RETROSPECTIVE_SYSTEM_PROMPT_TEMPLATE = """당신은 사용자의 사회적 만남을 되돌아보고 회고글을 작성하도록 돕는 AI 어시스턴트입니다.
사용자가 최근 만남에 대해 자연스럽게 이야기할 수 있도록 따뜻하고 공감적인 질문을 해주세요.

## 만남 정보
{meeting_section}

{rag_section}

## 지침
- 한 번에 하나의 질문만 해주세요.
- 사용자의 감정과 경험에 초점을 맞춰주세요.
- 이전 답변을 바탕으로 더 깊이 있는 후속 질문을 해주세요.
- 질문은 간결하고 자연스러운 한국어로 작성해주세요.
- 만남의 구체적인 상황과 맥락을 활용하여 개인화된 질문을 해주세요."""


COMPILE_PROMPT_TEMPLATE = """아래는 사용자가 만남에 대해 답변한 내용입니다.
이 답변들을 바탕으로 자연스럽고 따뜻한 회고글을 작성해주세요.

## 만남 정보
{meeting_section}

## 사용자 답변
{answers_section}

## 작성 지침
- 1인칭 시점으로 작성해주세요.
- 사용자의 감정과 경험을 잘 담아주세요.
- 자연스러운 한국어 문체를 사용해주세요.
- 200~500자 분량으로 작성해주세요.
- 회고글만 출력하고 다른 텍스트는 포함하지 마세요."""


def build_meeting_section(meeting_context: MeetingContext) -> str:
    """만남 맥락 정보를 텍스트 섹션으로 구성한다.

    Args:
        meeting_context: 만남 맥락 정보.

    Returns:
        만남 정보가 포함된 텍스트 문자열.
    """
    lines: list[str] = []

    lines.append(f"- 만난 사람: {meeting_context.matchedUserName}")
    lines.append(f"- 만남 날짜: {meeting_context.meetingDate}")

    if meeting_context.meetingLocation:
        lines.append(f"- 만남 장소: {meeting_context.meetingLocation}")

    return "\n".join(lines)


def build_rag_section(rag_documents: list[Document]) -> str:
    """RAG 검색 결과를 프롬프트 컨텍스트 섹션으로 구성한다.

    Args:
        rag_documents: RAG 파이프라인에서 검색된 과거 회고 문서 리스트.

    Returns:
        과거 회고 컨텍스트가 포함된 텍스트 문자열.
        문서가 없으면 빈 문자열을 반환한다.
    """
    if not rag_documents:
        return ""

    lines: list[str] = ["## 과거 회고 참고 자료"]

    for i, doc in enumerate(rag_documents, 1):
        lines.append(f"\n### 참고 {i}")
        lines.append(doc.content)

    return "\n".join(lines)


def build_retrospective_system_prompt(
    meeting_context: MeetingContext,
    rag_documents: list[Document] | None = None,
) -> str:
    """회고 전용 시스템 프롬프트를 생성한다.

    만남 맥락과 RAG 검색 결과를 포함하여 개인화된 시스템 프롬프트를 구성한다.

    Args:
        meeting_context: 만남 맥락 정보.
        rag_documents: RAG 파이프라인에서 검색된 과거 회고 문서 리스트.

    Returns:
        Bedrock에 전송할 시스템 프롬프트 문자열.
    """
    meeting_section = build_meeting_section(meeting_context)
    rag_section = build_rag_section(rag_documents or [])

    return RETROSPECTIVE_SYSTEM_PROMPT_TEMPLATE.format(
        meeting_section=meeting_section,
        rag_section=rag_section,
    )


def build_compile_prompt(
    meeting_context: MeetingContext,
    user_answers: list[str],
) -> str:
    """회고글 컴파일용 프롬프트를 생성한다.

    사용자의 모든 답변을 종합하여 회고글을 작성하도록 하는 프롬프트를 구성한다.

    Args:
        meeting_context: 만남 맥락 정보.
        user_answers: 사용자가 제공한 답변 리스트.

    Returns:
        Bedrock에 전송할 컴파일 프롬프트 문자열.
    """
    meeting_section = build_meeting_section(meeting_context)

    answers_lines: list[str] = []
    for i, answer in enumerate(user_answers, 1):
        answers_lines.append(f"{i}. {answer}")

    answers_section = "\n".join(answers_lines)

    return COMPILE_PROMPT_TEMPLATE.format(
        meeting_section=meeting_section,
        answers_section=answers_section,
    )
