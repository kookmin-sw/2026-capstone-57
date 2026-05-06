package com.ilgiyebo.domain.interaction.dto;

import java.util.List;

public record QuizQuestionDto(
    String question,
    List<String> options,
    int correctAnswer,
    String explanation
) {}
