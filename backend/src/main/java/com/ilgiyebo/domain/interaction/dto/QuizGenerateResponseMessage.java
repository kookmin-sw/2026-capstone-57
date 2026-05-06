package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * AI quiz generation response SQS message.
 * Received from AI to interaction direction.
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
