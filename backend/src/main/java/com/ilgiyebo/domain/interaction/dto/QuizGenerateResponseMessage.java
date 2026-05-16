package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * AI 퀴즈 생성 응답 SQS 메시지.
 * AI가 퀴즈를 생성 완료하면 이 형식으로 응답한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGenerateResponseMessage(
    String action,
    String status,
    String userId,
    QuizPayload quiz,
    int questionCount,
    Instant completedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizPayload(
        String userId,
        List<AiQuizQuestion> questions,
        Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiQuizQuestion(
        String questionText,
        List<String> choices,
        int correctIndex
    ) {}
}
