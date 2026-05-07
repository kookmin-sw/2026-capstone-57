package com.ilgiyebo.domain.ai.quiz.dto;

import java.util.List;

public record BedrockQuizResponse(
    List<QuestionItem> questions
) {
    public record QuestionItem(
        String questionText,
        List<String> choices,
        int correctIndex,
        String explanation
    ) {}
}
