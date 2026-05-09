package com.ilgiyebo.domain.chat.exception;

import com.ilgiyebo.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatException {

    SESSION_NOT_FOUND("채팅 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    SESSION_ALREADY_ENDED("이미 종료된 채팅 세션입니다", HttpStatus.BAD_REQUEST),
    MATCH_NOT_FOUND("매치를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MATCH_NOT_ACTIVE("매치가 활성 상태가 아닙니다", HttpStatus.BAD_REQUEST),
    NOT_PARTICIPANT("채팅 참여자가 아닙니다", HttpStatus.FORBIDDEN),
    INVALID_MESSAGE("유효하지 않은 메시지입니다", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    public BusinessException toException() {
        return new BusinessException(this.status, this.message);
    }
}
