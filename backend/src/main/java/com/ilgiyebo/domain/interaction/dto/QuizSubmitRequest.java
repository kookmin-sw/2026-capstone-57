package com.ilgiyebo.domain.interaction.dto;

import jakarta.validation.constraints.NotNull;

public record QuizSubmitRequest(
    @NotNull Integer quizIndex,
    @NotNull Integer answer
) {}
