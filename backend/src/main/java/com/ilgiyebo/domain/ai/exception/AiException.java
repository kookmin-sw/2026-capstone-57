package com.ilgiyebo.domain.ai.exception;

import org.springframework.http.HttpStatus;
import com.ilgiyebo.common.exception.BusinessException;

public class AiException extends BusinessException {
    public AiException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
