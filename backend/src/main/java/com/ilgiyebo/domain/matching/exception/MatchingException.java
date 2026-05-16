package com.ilgiyebo.domain.matching.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum MatchingException {

    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "슬롯을 찾을 수 없습니다"),
    SLOT_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 슬롯에 대한 권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    MatchingException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
