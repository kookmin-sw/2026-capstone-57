package com.ilgiyebo.domain.user.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum UserException {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    PROFILE_ALREADY_SET(HttpStatus.CONFLICT, "프로필이 이미 설정되어 있습니다"),
    SEMESTER_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 활성 학기를 찾을 수 없습니다"),
    SCHEDULE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "시간표를 가져오는데 실패했습니다. 식별자를 확인해주세요");

    private final HttpStatus status;
    private final String message;

    UserException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
