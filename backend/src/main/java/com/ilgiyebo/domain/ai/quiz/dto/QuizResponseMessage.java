package com.ilgiyebo.domain.ai.quiz.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizResponseMessage(
    String action,
    String status,
    UUID matchId,
    UUID requesterId,
    UUID targetUserId,
    QuizData quiz,
    int questionCount,
    Instant completedAt
) {
    public record QuizData(
        UUID matchId,
        UUID targetUserId,
        List<QuestionItem> questions,
        Instant createdAt
    ) {}

    public record QuestionItem(
        String questionText,
        List<String> choices,
        int correctIndex,
        String explanation
    ) {}
}
