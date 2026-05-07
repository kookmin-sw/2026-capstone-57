package com.ilgiyebo.domain.interaction.dto;

import java.util.List;

public record QuizQuestionDto(
    int quizIndex,
    String question,
    List<String> options,
    int correctAnswer,
    Integer quizAnswer
) {}
