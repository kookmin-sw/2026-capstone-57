package com.ilgiyebo.domain.interaction.dto;

import java.util.UUID;

public record QuizResponseDto(
    UUID matchId,
    int quizIndex,
    int correctAnswer,
    Integer userAnswer,
    boolean isCorrect,
    int correctCount,
    int totalCount,
    boolean allCompleted
) {
    public static QuizResponseDto from(UUID matchId, int quizIndex, int correctAnswer,
                                       int userAnswer, int correctCount, int totalCount,
                                       boolean allCompleted) {
        return new QuizResponseDto(
                matchId, quizIndex, correctAnswer, userAnswer,
                correctAnswer == userAnswer, correctCount, totalCount, allCompleted
        );
    }
}
