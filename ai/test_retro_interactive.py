"""회고 작성 대화형 E2E 테스트.

터미널에서 입력을 받아 HTTP로 AI 서비스에 전송하고,
AI의 질문을 화면에 표시하는 대화형 테스트.

사전 조건:
    - AI 서비스가 실행 중이어야 함 (python -m app)
    - 서버 주소: http://localhost:8081

EC2에서 실행:
    python3 test_retro_interactive.py
"""

from __future__ import annotations

import json
import uuid

import requests

BASE_URL = "http://localhost:8081"
SESSION_ID = f"retro-test-{uuid.uuid4().hex[:8]}"
USER_ID = "user-test-001"

# 테스트용 만남 맥락
MEETING_CONTEXT = {
    "matchedUserId": "user-matched-001",
    "matchedUserName": "김민수",
    "meetingDate": "2026-05-12",
    "meetingPlace": "복지관 카페",
    "missionActivity": "커피 마시면서 대화 나누기",
}


def start_session():
    """세션 시작 → 첫 질문 받기."""
    print("\n" + "=" * 60)
    print("📝 회고 작성 세션 시작")
    print("=" * 60)
    print(f"   Session ID: {SESSION_ID}")
    print(f"   User ID: {USER_ID}")
    print(f"   매칭 상대: {MEETING_CONTEXT['matchedUserName']}")
    print(f"   만남 장소: {MEETING_CONTEXT['meetingPlace']}")
    print(f"   미션: {MEETING_CONTEXT['missionActivity']}")
    print()

    response = requests.post(
        f"{BASE_URL}/api/retro/start",
        json={
            "sessionId": SESSION_ID,
            "userId": USER_ID,
            "meetingContext": MEETING_CONTEXT,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 세션 시작 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    data = response.json()
    return data


def send_answer(user_message: str):
    """사용자 답변 전송 → 후속 질문 받기."""
    response = requests.post(
        f"{BASE_URL}/api/retro/answer",
        json={
            "sessionId": SESSION_ID,
            "userId": USER_ID,
            "userMessage": user_message,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 답변 전송 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def complete_session():
    """세션 완료 → 컴파일된 회고글 받기."""
    response = requests.post(
        f"{BASE_URL}/api/retro/complete",
        json={
            "sessionId": SESSION_ID,
            "userId": USER_ID,
        },
    )

    if response.status_code != 200:
        print(f"   ❌ 완료 실패: {response.status_code}")
        print(f"   {response.text}")
        return None

    return response.json()


def main():
    print("🚀 회고 작성 대화형 E2E 테스트")
    print(f"   서버: {BASE_URL}")
    print("   종료: 'q' 입력 | 완료: 'done' 입력")

    # 1. 세션 시작
    result = start_session()
    if not result:
        return

    question = result.get("question")
    current_turn = result.get("currentTurn", 0)
    max_turns = result.get("maxTurns", 5)

    # 2. 대화 루프
    while True:
        print(f"\n{'─' * 60}")
        print(f"🤖 AI ({current_turn}/{max_turns}):")
        print(f"   {question}")
        print(f"{'─' * 60}")

        user_input = input("👤 나: ").strip()

        if user_input.lower() == "q":
            print("\n⏹️ 테스트 종료")
            return

        if user_input.lower() == "done":
            # 완료 요청
            print("\n📋 회고글 컴파일 중...")
            result = complete_session()
            if result:
                print(f"\n{'=' * 60}")
                print("✅ 회고 작성 완료!")
                print(f"{'=' * 60}")
                compiled = result.get("compiledContent", "")
                print(f"\n{compiled}")
            return

        if not user_input:
            continue

        # 답변 전송
        result = send_answer(user_input)
        if not result:
            return

        # 완료된 경우
        if result.get("action") == "COMPLETED":
            print(f"\n{'=' * 60}")
            print("✅ 회고 작성 완료! (최대 턴 도달)")
            print(f"{'=' * 60}")
            compiled = result.get("compiledContent", "")
            print(f"\n{compiled}")
            return

        question = result.get("question", "")
        current_turn = result.get("currentTurn", current_turn + 1)
        max_turns = result.get("maxTurns", max_turns)


if __name__ == "__main__":
    main()
