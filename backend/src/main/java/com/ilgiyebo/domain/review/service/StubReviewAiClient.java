package com.ilgiyebo.domain.review.service;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 로컬 개발용 스텁 AI 클라이언트.
 * 실제 AI 서버 없이 고정된 질문과 회고 내용을 반환한다.
 */
@Slf4j
public class StubReviewAiClient implements ReviewAiClient {

    private static final int DEFAULT_MAX_TURNS = 3;

    @Override
    public FirstQuestionResponse getFirstQuestion(UUID userId, MeetingInfo meetingInfo) {
        log.debug("스텁 AI 첫 번째 회고 질문: userId={}, matchedUser={}", userId, meetingInfo.matchedUserName());
        return new FirstQuestionResponse(
                "이번 만남에서 가장 기억에 남는 순간은 무엇인가요?",
                DEFAULT_MAX_TURNS
        );
    }

    @Override
    public NextQuestionResponse getNextQuestion(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory) {
        int currentTurn = conversationHistory.size() + 1;
        boolean isComplete = currentTurn >= DEFAULT_MAX_TURNS;

        log.debug("스텁 AI 다음 회고 질문: userId={}, currentTurn={}, isComplete={}", userId, currentTurn, isComplete);

        String question;
        if (currentTurn == 2) {
            question = "상대방과의 대화에서 새롭게 알게 된 점이 있나요?";
        } else {
            question = "다음에 다시 만난다면 어떤 활동을 함께 하고 싶나요?";
        }

        return new NextQuestionResponse(question, isComplete, currentTurn, DEFAULT_MAX_TURNS);
    }

    @Override
    public GenerateResponse generateReview(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory) {
        log.debug("스텁 AI 회고 글 생성: userId={}, 대화 턴 수={}", userId, conversationHistory.size());

        StringBuilder sb = new StringBuilder();
        sb.append("이번 만남을 돌아보며:\n\n");
        for (ConversationTurn turn : conversationHistory) {
            sb.append("Q: ").append(turn.question()).append("\n");
            sb.append("A: ").append(turn.answer()).append("\n\n");
        }
        sb.append("전반적으로 좋은 시간이었습니다.");

        return new GenerateResponse(sb.toString(), Instant.now());
    }
}
