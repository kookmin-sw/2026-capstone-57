package com.ilgiyebo.domain.interaction.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum InteractionException {

    INTERACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "상호작용 정보를 찾을 수 없습니다"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 정보를 찾을 수 없습니다"),
    USER_NOT_IN_MATCH(HttpStatus.FORBIDDEN, "해당 매칭의 참여자가 아닙니다"),
    ALREADY_TERMINATED(HttpStatus.BAD_REQUEST, "이미 종료된 매칭입니다"),
    STAGE_NOT_ADVANCEABLE(HttpStatus.BAD_REQUEST, "현재 단계에서 다음 단계로 진행할 수 없습니다"),
    QUIZ_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "퀴즈를 먼저 완료해야 합니다"),
    NOT_IN_QUIZ_STAGE(HttpStatus.BAD_REQUEST, "퀴즈 단계에서만 가능한 요청입니다"),
    HINT_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "힌트 질문을 찾을 수 없습니다"),
    HINT_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "이미 답변이 완료된 질문입니다"),
    NOT_RESPONDER(HttpStatus.FORBIDDEN, "해당 질문의 답변자가 아닙니다"),
    QUIZ_ANSWER_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "모든 문항에 답변해야 합니다"),
    QUIZ_NOT_GENERATED(HttpStatus.BAD_REQUEST, "퀴즈가 아직 생성되지 않았습니다"),
    QUIZ_GENERATING(HttpStatus.ACCEPTED, "퀴즈를 생성 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    InteractionException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public BusinessException toException() {
        return new BusinessException(status, message);
    }
}
