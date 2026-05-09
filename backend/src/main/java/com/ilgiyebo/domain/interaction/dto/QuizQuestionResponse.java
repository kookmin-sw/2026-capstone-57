package com.ilgiyebo.domain.interaction.dto;

import java.util.List;

/**
 * 프론트엔드에 퀴즈 문항을 내려줄 때 사용하는 DTO.
 * correctAnswer를 제외하여 정답 노출을 방지한다.
 */
public record QuizQuestionResponse(
    int quizIndex,
    String question,
    List<String> options
) {
    public static QuizQuestionResponse from(QuizQuestionDto dto) {
        return new QuizQuestionResponse(
                dto.quizIndex(),
                dto.question(),
                dto.options()
        );
    }
}
