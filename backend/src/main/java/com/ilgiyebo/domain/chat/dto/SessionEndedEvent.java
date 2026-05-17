package com.ilgiyebo.domain.chat.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SessionEndedEvent(
        UUID sessionId,
        Instant endedAt,
        String reason
) {
    public static final String TOKEN_LIMIT_REACHED = "TOKEN_LIMIT_REACHED";
}
