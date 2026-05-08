package com.ilgiyebo.domain.interaction.dto;

import java.util.List;
import java.util.UUID;

public record QuizResponseDto(
    UUID matchId,
    List<QuizQuestionDto> questions,
    boolean completed,
    Integer correctCount,
    Integer totalCount,
    String partnerSummary
) {
    // from 메서드 추가
    public static QuizResponseDto from(UUID matchId, List<QuizQuestionDto> questions,
                                       int correctCount, int totalCount, String summary) {
        return new QuizResponseDto(matchId, questions, true, correctCount, totalCount, summary);
    }
}
