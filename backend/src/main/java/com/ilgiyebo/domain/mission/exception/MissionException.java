package com.ilgiyebo.domain.mission.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum MissionException {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 정보를 찾을 수 없습니다"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 정보를 찾을 수 없습니다"),
    USER_NOT_IN_MATCH(HttpStatus.FORBIDDEN, "해당 매칭의 참여자가 아닙니다"),
    NOT_IN_MISSION_STAGE(HttpStatus.BAD_REQUEST, "미션 단계에서만 가능한 요청입니다"),
    ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 미션 수행을 확인하였습니다"),
    MISSION_EXPIRED(HttpStatus.BAD_REQUEST, "미션 기한이 만료되었습니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 미션입니다"),
    NO_SCHEDULE_DATA(HttpStatus.BAD_REQUEST, "시간표 데이터가 없어 미션을 생성할 수 없습니다"),
    NO_ROUTE_OVERLAP(HttpStatus.BAD_REQUEST, "동선 겹침이 없어 미션을 생성할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    MissionException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
