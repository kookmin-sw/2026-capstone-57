package com.ilgiyebo.domain.planner.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum PlannerException {

    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다"),
    ENTRY_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 일정에 대한 권한이 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    SEMESTER_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 활성 학기를 찾을 수 없습니다"),
    INVALID_TIME_UNIT(HttpStatus.BAD_REQUEST, "시간은 30분 단위로만 설정할 수 있습니다"),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 이후여야 합니다"),
    TIME_CONFLICT(HttpStatus.CONFLICT, "해당 시간대에 이미 일정이 존재합니다");

    private final HttpStatus status;
    private final String message;

    PlannerException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
