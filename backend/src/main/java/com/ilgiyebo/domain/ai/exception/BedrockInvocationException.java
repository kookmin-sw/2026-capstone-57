package com.ilgiyebo.domain.ai.exception;

public class BedrockInvocationException extends AiException {
    public BedrockInvocationException(String message, Throwable cause) {
        super("Bedrock 호출 실패: " + message);
        initCause(cause);
    }
}
