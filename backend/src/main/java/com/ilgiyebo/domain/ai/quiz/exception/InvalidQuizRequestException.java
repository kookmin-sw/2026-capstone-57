package com.ilgiyebo.domain.ai.quiz.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class InvalidQuizRequestException extends QuizException {
    private final List<String> missingFields;

    public InvalidQuizRequestException(List<String> missingFields) {
        super("필수 필드 누락: " + String.join(", ", missingFields));
        this.missingFields = missingFields;
    }
}
