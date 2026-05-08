package com.ilgiyebo.domain.ai.quiz.exception;

import lombok.Getter;

@Getter
public class AiResponseParseException extends QuizException {
    private final String rawResponse;
    private final String parseDetail;

    public AiResponseParseException(String rawResponse, String parseDetail) {
        super("AI 응답 파싱 실패: " + parseDetail);
        this.rawResponse = rawResponse;
        this.parseDetail = parseDetail;
    }
}
