"""일기 작성 대화형 E2E 테스트 (Stateless API).

터미널에서 입력을 받아 HTTP로 AI 서비스에 전송하고,
AI의 질문을 화면에 표시하는 대화형 테스트.

사전 조건:
    - AI 서비스가 실행 중이어야 함
    - 서버 주소: http://localhost:8081

EC2에서 실행:
    python3 test_diary_interactive.py
"""

from __future__ import annotations

import uuid

import requests

BASE_URL = "http://localhost:8000"
SESSION_ID = f"diary-test-{uuid.uuid4().hex[:8]}"
USER_ID = "user-test-001"
DATE = "2026-05-14"

# 테스트용 일정 데이터
TODAY_SCHEDULE = [
    {"startTime": "09:00", "endTime": "10:30", "location": "미래관", "activity": "알고리즘"},
    {"startTime": "11:00", "endTime": "12:00", "location": None, "activity": "점심시간"},
    {"startTime": "13:00", "endTime": "14:30", "location": "미래관", "activity": "캡스톤디자인"},
    {"startTime": "15:00", "endTime": "16:30", "location": "미래관", "activity": "자율주행스튜디오에서 자습"},
]

PREVIOUS_DIARY = "어제는 캡스톤 발표 준비를 했다. 팀원들이랑 밤늦게까지 PPT를 만들었는데 결과가 꽤 괜찮았다."


def first_question():
    """첫 질문 요청."""
    response = requests.post(
        f"{BASE_URL}/api/diary/first-question",
        json={
            "userId": USER_ID,
            "date": DATE,
            "todaySchedule": TODAY_SCHEDULE,
            "previousDiaryContent": PREVIOUS_DIARY,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 첫 질문 생성 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def next_question(conversation_history: list[dict]):
    """다음 질문 요청."""
    response = requests.post(
        f"{BASE_URL}/api/diary/next-question",
        json={
            "userId": USER_ID,
            "date": DATE,
            "conversationHistory": conversation_history,
            "todaySchedule": TODAY_SCHEDULE,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 다음 질문 생성 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def generate_diary(conversation_history: list[dict]):
    """일기 생성 요청."""
    response = requests.post(
        f"{BASE_URL}/api/diary/generate",
        json={
            "sessionId": SESSION_ID,
            "userId": USER_ID,
            "date": DATE,
            "conversationHistory": conversation_history,
            "todaySchedule": TODAY_SCHEDULE,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 일기 생성 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def main():
    print("🚀 일기 작성 대화형 E2E 테스트 (Stateless)")
    print(f"   서버: {BASE_URL}")
    print(f"   날짜: {DATE}")
    print("   종료: 'q' 입력 | 일기 생성: 'done' 입력")

    # 1. 첫 질문
    print(f"\n{'=' * 60}")
    print("📝 일기 작성 시작")
    print(f"{'=' * 60}")

    result = first_question()
    if not result:
        return

    question = result["question"]
    max_turns = result.get("maxTurns", 5)
    conversation_history = []
    turn_number = 1

    # 2. 대화 루프
    while True:
        print(f"\n{'─' * 60}")
        print(f"🤖 AI ({turn_number}/{max_turns}):")
        print(f"   {question}")
        print(f"{'─' * 60}")

        user_input = input("👤 나: ").strip()

        if user_input.lower() == "q":
            print("\n⏹️ 테스트 종료")
            return

        if user_input.lower() == "done":
            if not conversation_history:
                print("   ⚠️ 최소 1개 답변이 필요합니다.")
                continue
            break

        if not user_input:
            continue

        # 대화 히스토리에 추가
        conversation_history.append({
            "turnNumber": turn_number,
            "question": question,
            "answer": user_input,
        })
        turn_number += 1

        # 다음 질문 요청
        result = next_question(conversation_history)
        if not result:
            return

        # AI가 완료 판단한 경우
        if result.get("isConversationComplete"):
            print(f"\n   ✅ AI가 충분한 답변이 수집되었다고 판단했습니다.")
            break

        question = result["question"]
        max_turns = result.get("maxTurns", max_turns)

    # 3. 일기 생성
    print(f"\n{'=' * 60}")
    print("📋 일기 생성 중...")
    print(f"{'=' * 60}")

    result = generate_diary(conversation_history)
    if result:
        print(f"\n✅ 일기 생성 완료!")
        print(f"\n{'─' * 60}")
        print("[응답 JSON]")
        import json
        print(json.dumps(result, ensure_ascii=False, indent=2))
        print(f"{'─' * 60}")


if __name__ == "__main__":
    main()
