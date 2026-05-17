package com.ilgiyebo.domain.diary.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum DiarySessionException {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 일기 세션을 찾을 수 없습니다"),
    SESSION_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 세션에 대한 권한이 없습니다"),
    SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "해당 날짜에 이미 완료된 AI 일기 세션이 있습니다"),
    INVALID_SESSION_STATUS(HttpStatus.BAD_REQUEST, "현재 세션 상태에서는 해당 작업을 수행할 수 없습니다"),
    NO_PENDING_QUESTION(HttpStatus.BAD_REQUEST, "답변 대기 중인 질문이 없습니다"),
    SESSION_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "이미 완료되었거나 취소된 세션은 취소할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    DiarySessionException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
