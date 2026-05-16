package com.ilgiyebo.domain.review.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum ReviewException {

    INTERACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "상호작용 정보를 찾을 수 없습니다"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 정보를 찾을 수 없습니다"),
    USER_NOT_IN_MATCH(HttpStatus.FORBIDDEN, "해당 매칭의 참여자가 아닙니다"),
    NOT_IN_REVIEW_STAGE(HttpStatus.BAD_REQUEST, "회고 단계에서만 가능한 요청입니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "회고 세션을 찾을 수 없습니다"),
    SESSION_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 세션에 대한 권한이 없습니다"),
    SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 회고 세션이 존재합니다"),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 회고가 작성되었습니다"),
    INVALID_SESSION_STATUS(HttpStatus.BAD_REQUEST, "현재 세션 상태에서는 해당 작업을 수행할 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 회고 질문을 찾을 수 없습니다"),
    QUESTION_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "이미 답변된 질문입니다"),
    NOT_ALL_QUESTIONS_ANSWERED(HttpStatus.BAD_REQUEST, "모든 질문에 답변해야 합니다"),
    INVALID_SATISFACTION(HttpStatus.BAD_REQUEST, "만족도는 1~5 사이여야 합니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "잘못된 날짜 범위입니다 (시작일이 종료일보다 늦을 수 없습니다)"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    SESSION_MODE_MISMATCH(HttpStatus.BAD_REQUEST, "이미 AI 모드로 세션이 생성되어 직접 작성이 불가합니다");

    private final HttpStatus status;
    private final String message;

    ReviewException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
