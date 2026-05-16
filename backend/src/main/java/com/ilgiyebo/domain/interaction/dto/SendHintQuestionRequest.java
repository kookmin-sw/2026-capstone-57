package com.ilgiyebo.domain.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record SendHintQuestionRequest(
    @NotBlank String question,
    @Min(0) int quizIndex
) {}
