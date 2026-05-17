package com.ilgiyebo.domain.game.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum GameException {

    GAME_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "게임 세션을 찾을 수 없습니다"),
    NOT_GAME_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 게임의 참여자가 아닙니다"),
    GAME_SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성 상태가 아닌 게임 세션입니다"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 정보를 찾을 수 없습니다"),
    MATCH_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성 상태가 아닌 매칭입니다"),
    GAME_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "게임 방을 찾을 수 없습니다"),
    INVALID_GAME_ACTION(HttpStatus.BAD_REQUEST, "유효하지 않은 게임 액션입니다");

    private final HttpStatus status;
    private final String message;

    GameException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
