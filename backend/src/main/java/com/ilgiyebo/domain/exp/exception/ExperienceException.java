package com.ilgiyebo.domain.exp.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum ExperienceException {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    INVALID_ACTIVITY(HttpStatus.BAD_REQUEST, "유효하지 않은 활동 유형입니다"),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘의 경험치 지급 한도를 초과했습니다");

    private final HttpStatus status;
    private final String message;

    ExperienceException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
