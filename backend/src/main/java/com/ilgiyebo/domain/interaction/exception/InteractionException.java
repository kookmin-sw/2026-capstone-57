package com.ilgiyebo.domain.interaction.exception;

import com.ilgiyebo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public enum InteractionException {

    INTERACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Interaction not found"),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Match not found"),
    USER_NOT_IN_MATCH(HttpStatus.FORBIDDEN, "User is not a participant of this match"),
    ALREADY_TERMINATED(HttpStatus.BAD_REQUEST, "Match is already terminated"),
    STAGE_NOT_ADVANCEABLE(HttpStatus.BAD_REQUEST, "Cannot advance from current stage"),
    QUIZ_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "Quiz must be completed first"),
    NOT_IN_QUIZ_STAGE(HttpStatus.BAD_REQUEST, "Hint questions can only be sent during quiz stage"),
    HINT_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Hint question not found"),
    HINT_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "Question is already answered"),
    NOT_RESPONDER(HttpStatus.FORBIDDEN, "User is not the responder of this question"),
    QUIZ_ANSWER_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "All questions must be answered"),
    QUIZ_NOT_GENERATED(HttpStatus.BAD_REQUEST, "Quiz has not been generated yet"),
    QUIZ_GENERATING(HttpStatus.ACCEPTED, "Quiz is being generated. Please try again shortly.");

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
