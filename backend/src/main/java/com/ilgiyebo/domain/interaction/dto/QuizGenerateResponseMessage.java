package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI 퀴즈 생성 완료 응답 SQS 메시지.
 *
 * 예시:
 * {
 *   "action": "QUIZ_GENERATED",
 *   "status": "SUCCESS",
 *   "matchId": "match-abc123",
 *   "requesterId": "user-111",
 *   "targetUserId": "user-456",
 *   "quiz": {
 *     "matchId": "match-abc123",
 *     "targetUserId": "user-456",
 *     "questions": [
 *       {
 *         "questionText": "이 사람이 주말에 가장 즐길 만한 활동은?",
 *         "choices": ["등산", "요리 클래스", "게임", "쇼핑"],
 *         "correctIndex": 1,
 *         "explanation": "요리에 관심이 많고 새로운 경험을 즐기는 성향"
 *       }
 *     ],
 *     "createdAt": "2026-05-05T09:30:05.000Z"
 *   },
 *   "questionCount": 5,
 *   "completedAt": "2026-05-05T09:30:05.000Z"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGenerateResponseMessage(
    String action,
    String status,
    String matchId,
    String requesterId,
    String targetUserId,
    QuizPayload quiz,
    int questionCount,
    Instant completedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizPayload(
        String matchId,
        String targetUserId,
        List<AiQuizQuestion> questions,
        Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiQuizQuestion(
        String questionText,
        List<String> choices,
        int correctIndex,
        String explanation
    ) {}
}
