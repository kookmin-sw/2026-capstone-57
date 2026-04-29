package com.ilgiyebo.domain.auth.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum AuthException {

    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 형식입니다"),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "허용되지 않은 이메일 도메인입니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 이메일입니다"),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다"),
    VERIFICATION_EXPIRED(HttpStatus.GONE, "인증 코드가 만료되었습니다"),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.UNAUTHORIZED, "인증 코드가 일치하지 않습니다"),
    VERIFICATION_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않았습니다"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다");

    private final HttpStatus status;
    private final String message;

    AuthException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
