package com.ilgiyebo.domain.interaction.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuizSubmitRequest(
    @NotNull List<Integer> answers
) {}
