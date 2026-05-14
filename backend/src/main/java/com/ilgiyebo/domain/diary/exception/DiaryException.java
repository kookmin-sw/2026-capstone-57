package com.ilgiyebo.domain.diary.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum DiaryException {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "일기를 찾을 수 없습니다"),
    ENTRY_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 일기에 대한 권한이 없습니다"),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "일기 내용은 비어있을 수 없습니다"),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "유효하지 않은 날짜입니다");

    private final HttpStatus status;
    private final String message;

    DiaryException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
