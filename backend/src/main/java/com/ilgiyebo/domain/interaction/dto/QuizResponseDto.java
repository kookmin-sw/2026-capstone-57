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
) {}
