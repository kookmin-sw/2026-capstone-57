"""회고(Retrospective) HTTP 라우터 (Stateless).

AI 서버는 stateless하게 동작하며, 매 요청마다 전체 컨텍스트를 받아 처리한다.
세션 상태는 Backend(Spring Boot)가 DB에서 관리한다.

Endpoints:
    POST /api/retro/first-question  - 첫 질문 생성
    POST /api/retro/next-question   - 다음 질문 생성 (대화 완료 판단 포함)
    POST /api/retro/generate        - 대화 내용 기반 회고글 생성
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from app.common.logging import get_logger, logging_context

logger = get_logger(__name__)

router = APIRouter(prefix="/api/retro", tags=["retrospective"])


# --- Request / Response Schemas ---


class MeetingInfo(BaseModel):
    """만남 정보."""

    matchedUserName: str
    meetingDate: str
    meetingPlace: Optional[str] = None
    missionActivity: Optional[str] = None


class ConversationTurn(BaseModel):
    """대화 턴."""

    turnNumber: int
    question: str
    answer: str


class FirstQuestionRequest(BaseModel):
    """첫 질문 생성 요청."""

    userId: str
    meetingInfo: MeetingInfo


class NextQuestionRequest(BaseModel):
    """다음 질문 생성 요청."""

    userId: str
    meetingInfo: MeetingInfo
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)


class GenerateRequest(BaseModel):
    """회고글 생성 요청."""

    userId: str
    meetingInfo: MeetingInfo
    conversationHistory: list[ConversationTurn] = Field(default_factory=list)


class FirstQuestionResponse(BaseModel):
    """첫 질문 응답."""

    question: str
    maxTurns: int = 5


class NextQuestionResponse(BaseModel):
    """다음 질문 응답."""

    question: Optional[str] = None
    isConversationComplete: bool = False
    currentTurn: int
    maxTurns: int = 5


class ProfileUpdate(BaseModel):
    """성향 업데이트 정보."""

    hobbies: list[str] = Field(default_factory=list)
    interests: list[str] = Field(default_factory=list)


class GenerateResponse(BaseModel):
    """회고글 생성 응답."""

    compiledContent: str
    profileUpdate: Optional[ProfileUpdate] = None
    generatedAt: datetime


# --- Internal helpers ---


def _build_meeting_text(meeting_info: MeetingInfo) -> str:
    """만남 정보를 텍스트로 변환."""
    lines = [
        f"- 상대: {meeting_info.matchedUserName}",
        f"- 날짜: {meeting_info.meetingDate}",
    ]
    if meeting_info.meetingPlace:
        lines.append(f"- 장소: {meeting_info.meetingPlace}")
    if meeting_info.missionActivity:
        lines.append(f"- 미션: {meeting_info.missionActivity}")
    return "\n".join(lines)


def _build_first_question_system_prompt(meeting_info: MeetingInfo) -> str:
    """첫 질문 생성용 시스템 프롬프트."""
    meeting_text = _build_meeting_text(meeting_info)

    return f"""당신은 대학생 매칭 서비스에서 만남 후 회고를 도와주는 따뜻한 AI 친구입니다.
사용자가 매칭된 상대와의 만남을 돌아볼 수 있도록 자연스러운 질문을 해주세요.

[만남 정보]
{meeting_text}

[규칙]
1. 질문은 1개만 생성하세요.
2. 만남의 구체적인 상황(장소, 활동)을 언급하며 질문하세요.
3. 친근하고 부담 없는 해요체를 사용하세요.
4. 감정, 느낌, 인상에 초점을 맞추세요.
5. 예/아니오로 답할 수 없는 열린 질문을 하세요.
6. 첫 질문은 전반적인 만남의 느낌을 물어보세요.
"""


def _build_next_question_system_prompt(
    meeting_info: MeetingInfo,
    max_turns: int,
    current_turn: int,
) -> str:
    """다음 질문 생성용 시스템 프롬프트."""
    meeting_text = _build_meeting_text(meeting_info)

    return f"""당신은 대학생 매칭 서비스에서 만남 후 회고를 도와주는 따뜻한 AI 친구입니다.
이전 대화를 바탕으로 만남에 대해 더 깊이 있는 후속 질문을 해주세요.

[만남 정보]
{meeting_text}

[규칙]
1. 질문은 1개만 생성하세요.
2. 이전 답변에서 언급된 감정이나 에피소드를 더 깊이 탐색하세요.
3. 이미 물어본 내용을 반복하지 마세요.
4. 친근하고 부담 없는 해요체를 사용하세요.
5. 현재 {current_turn}/{max_turns} 턴입니다.
6. 사용자가 충분히 풍부한 답변을 했다고 판단되면, 질문 대신 정확히 "[COMPLETE]"만 출력하세요.
7. 아직 더 물어볼 내용이 있다면 질문을 생성하세요.
8. 다음 만남에 대한 기대나 개선점도 물어볼 수 있습니다.
"""


def _build_generate_prompt(
    meeting_info: MeetingInfo,
    conversation_history: list[ConversationTurn],
) -> str:
    """회고글 생성 프롬프트."""
    meeting_text = _build_meeting_text(meeting_info)

    conversation_text = ""
    for turn in conversation_history:
        conversation_text += f"Q: {turn.question}\nA: {turn.answer}\n\n"

    return f"""당신은 대학생의 만남 회고글을 작성해주는 AI입니다.
아래 대화 내용을 바탕으로 자연스러운 회고글을 작성해주세요.

[만남 정보]
{meeting_text}

[대화 내용]
{conversation_text}

[규칙]
1. 1인칭 시점으로 작성하세요 (나는, 내가).
2. 대화에서 나온 감정과 에피소드를 자연스럽게 녹여내세요.
3. 만남의 좋았던 점, 느낀 점, 다음에 대한 기대를 포함하세요.
4. 3~5문단 분량으로 작성하세요.
5. 친근하고 자연스러운 문체를 사용하세요 (반말 일기체).
6. 대화 형식이 아닌 회고글 형식으로 작성하세요.
"""


def _build_profile_analysis_prompt(
    conversation_history: list[ConversationTurn],
) -> str:
    """성향 분석 프롬프트."""
    conversation_text = ""
    for turn in conversation_history:
        conversation_text += f"Q: {turn.question}\nA: {turn.answer}\n\n"

    return f"""아래 대화 내용에서 사용자의 취미(hobbies)와 관심사(interests)를 추출해주세요.

[대화 내용]
{conversation_text}

[규칙]
1. 사용자가 자발적으로 즐기거나 좋아한다고 언급한 것만 추출하세요.
2. 수업, 과제, 일정에 포함된 활동은 제외하세요. 학교 수업은 관심사가 아닙니다.
3. 여가 시간에 하는 활동, 취미, 자발적 관심사만 포함하세요.
4. 각 항목은 짧은 키워드로 (2~4글자 권장).
5. 각 리스트는 최대 5개까지만.
6. 추출할 내용이 없으면 빈 리스트로.
7. 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

{{"hobbies": ["취미1", "취미2"], "interests": ["관심사1", "관심사2"]}}
"""


# --- Endpoints ---


@router.post("/first-question", response_model=FirstQuestionResponse)
async def first_question(body: FirstQuestionRequest, request: Request):
    """첫 질문을 생성한다.

    만남 정보를 기반으로 첫 번째 질문을 생성한다.
    """
    bedrock_client = request.app.state.bedrock_client
    settings = request.app.state.settings

    with logging_context(correlation_id=f"retro-{body.userId}", feature="retrospective"):
        try:
            system_prompt = _build_first_question_system_prompt(body.meetingInfo)

            initial_messages = [
                {
                    "role": "user",
                    "content": "만남에 대한 회고를 시작하겠습니다. 첫 번째 질문을 해주세요.",
                }
            ]
            question = await bedrock_client.invoke_with_messages(
                system_prompt=system_prompt,
                messages=initial_messages,
            )

            return FirstQuestionResponse(
                question=question,
                maxTurns=settings.conversation_max_turns,
            )

        except Exception as exc:
            logger.exception("회고 첫 질문 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/next-question", response_model=NextQuestionResponse)
async def next_question(body: NextQuestionRequest, request: Request):
    """다음 질문을 생성하거나 대화 완료를 판단한다."""
    bedrock_client = request.app.state.bedrock_client
    settings = request.app.state.settings
    max_turns = settings.conversation_max_turns
    current_turn = len(body.conversationHistory)

    with logging_context(correlation_id=f"retro-{body.userId}", feature="retrospective"):
        try:
            # maxTurns 도달 시 강제 완료
            if current_turn >= max_turns:
                return NextQuestionResponse(
                    question=None,
                    isConversationComplete=True,
                    currentTurn=current_turn,
                    maxTurns=max_turns,
                )

            system_prompt = _build_next_question_system_prompt(
                meeting_info=body.meetingInfo,
                max_turns=max_turns,
                current_turn=current_turn,
            )

            # 대화 히스토리를 메시지로 변환
            messages = []
            for turn in body.conversationHistory:
                messages.append({"role": "assistant", "content": turn.question})
                messages.append({"role": "user", "content": turn.answer})

            messages.append({
                "role": "user",
                "content": "다음 질문을 해주세요. 충분하다고 판단되면 [COMPLETE]만 출력하세요.",
            })

            response = await bedrock_client.invoke_with_messages(
                system_prompt=system_prompt,
                messages=messages,
            )

            if "[COMPLETE]" in response:
                return NextQuestionResponse(
                    question=None,
                    isConversationComplete=True,
                    currentTurn=current_turn,
                    maxTurns=max_turns,
                )

            return NextQuestionResponse(
                question=response.strip(),
                isConversationComplete=False,
                currentTurn=current_turn,
                maxTurns=max_turns,
            )

        except Exception as exc:
            logger.exception("회고 다음 질문 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))


@router.post("/generate", response_model=GenerateResponse)
async def generate_retro(body: GenerateRequest, request: Request):
    """대화 내용을 기반으로 회고글을 생성한다.

    전체 대화 히스토리를 받아 회고글을 컴파일하고,
    대화 내용에서 사용자 성향(hobbies, interests)을 추출한다.
    """
    bedrock_client = request.app.state.bedrock_client

    with logging_context(correlation_id=f"retro-{body.userId}", feature="retrospective"):
        try:
            if not body.conversationHistory:
                raise HTTPException(status_code=400, detail="대화 내역이 없습니다")

            # 1. 회고글 생성
            prompt = _build_generate_prompt(
                meeting_info=body.meetingInfo,
                conversation_history=body.conversationHistory,
            )
            compiled_content = await bedrock_client.invoke(prompt)

            # 2. 성향 분석
            profile_update = await _analyze_profile(
                bedrock_client, body.conversationHistory
            )

            return GenerateResponse(
                compiledContent=compiled_content,
                profileUpdate=profile_update,
                generatedAt=datetime.now(timezone.utc),
            )

        except HTTPException:
            raise
        except Exception as exc:
            logger.exception("회고글 생성 실패")
            raise HTTPException(status_code=500, detail=str(exc))


async def _analyze_profile(
    bedrock_client,
    conversation_history: list[ConversationTurn],
) -> Optional[ProfileUpdate]:
    """대화 내용에서 성향을 분석한다."""
    import json as _json

    try:
        prompt = _build_profile_analysis_prompt(conversation_history)
        raw_response = await bedrock_client.invoke(prompt)

        # JSON 추출
        text = raw_response.strip()
        start = text.find("{")
        end = text.rfind("}") + 1
        if start == -1 or end == 0:
            return None

        data = _json.loads(text[start:end])
        hobbies = data.get("hobbies", [])
        interests = data.get("interests", [])

        if not hobbies and not interests:
            return None

        return ProfileUpdate(hobbies=hobbies, interests=interests)

    except Exception:
        logger.warning("성향 분석 실패 (계속 진행)")
        return None
