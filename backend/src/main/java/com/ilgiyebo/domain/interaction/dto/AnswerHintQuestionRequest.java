package com.ilgiyebo.domain.interaction.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerHintQuestionRequest(
    @NotBlank String answer
) {}
