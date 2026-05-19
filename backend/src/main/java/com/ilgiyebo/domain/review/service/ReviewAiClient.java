package com.ilgiyebo.domain.review.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회고 AI 클라이언트 인터페이스.
 * 외부 AI 서버와 통신하여 멀티턴 회고 질문 및 회고 글 생성을 담당한다.
 * AI 서버 API: /api/retro/first-question, /api/retro/next-question, /api/retro/generate
 */
public interface ReviewAiClient {

    /** 첫 번째 회고 질문 요청 */
    FirstQuestionResponse getFirstQuestion(UUID userId, MeetingInfo meetingInfo);

    /** 다음 회고 질문 요청 (대화 히스토리 기반) */
    NextQuestionResponse getNextQuestion(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory);

    /** 대화 히스토리 기반 회고 글 생성 */
    GenerateResponse generateReview(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory);

    // --- Inner DTOs ---

    record MeetingInfo(
            String matchedUserName,
            String meetingDate,
            String meetingPlace,
            String missionActivity
    ) {}

    record ConversationTurn(
            int turnNumber,
            String question,
            String answer
    ) {}

    record FirstQuestionResponse(
            String question,
            int maxTurns
    ) {}

    record NextQuestionResponse(
            String question,
            boolean isConversationComplete,
            int currentTurn,
            int maxTurns
    ) {}

    record GenerateResponse(
            String compiledContent,
            Instant generatedAt
    ) {}
}
