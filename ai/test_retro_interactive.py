"""회고 작성 대화형 E2E 테스트 (Stateless API).

터미널에서 입력을 받아 HTTP로 AI 서비스에 전송하고,
AI의 질문을 화면에 표시하는 대화형 테스트.

사전 조건:
    - AI 서비스가 실행 중이어야 함
    - 서버 주소: http://localhost:8081

EC2에서 실행:
    python3 test_retro_interactive.py
"""

from __future__ import annotations

import requests

BASE_URL = "http://localhost:8081"
USER_ID = "user-test-001"

# 테스트용 만남 정보
MEETING_INFO = {
    "matchedUserName": "김민수",
    "meetingDate": "2026-05-12",
    "meetingPlace": "복지관 카페",
    "missionActivity": "커피 마시면서 대화 나누기",
}


def first_question():
    """첫 질문 요청."""
    response = requests.post(
        f"{BASE_URL}/api/retro/first-question",
        json={
            "userId": USER_ID,
            "meetingInfo": MEETING_INFO,
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
        f"{BASE_URL}/api/retro/next-question",
        json={
            "userId": USER_ID,
            "meetingInfo": MEETING_INFO,
            "conversationHistory": conversation_history,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 다음 질문 생성 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def generate_retro(conversation_history: list[dict]):
    """회고글 생성 요청."""
    response = requests.post(
        f"{BASE_URL}/api/retro/generate",
        json={
            "userId": USER_ID,
            "meetingInfo": MEETING_INFO,
            "conversationHistory": conversation_history,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 회고글 생성 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def main():
    print("🚀 회고 작성 대화형 E2E 테스트 (Stateless)")
    print(f"   서버: {BASE_URL}")
    print(f"   상대: {MEETING_INFO['matchedUserName']}")
    print(f"   장소: {MEETING_INFO['meetingPlace']}")
    print("   종료: 'q' 입력 | 회고 생성: 'done' 입력")

    # 1. 첫 질문
    print(f"\n{'=' * 60}")
    print("📝 회고 작성 시작")
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

    # 3. 회고글 생성
    print(f"\n{'=' * 60}")
    print("📋 회고글 생성 중...")
    print(f"{'=' * 60}")

    result = generate_retro(conversation_history)
    if result:
        print(f"\n✅ 회고글 생성 완료!")
        print(f"   생성 시각: {result.get('generatedAt')}")
        print(f"\n{'─' * 60}")
        print(result["compiledContent"])
        print(f"{'─' * 60}")


if __name__ == "__main__":
    main()
