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
    public static final String TIME_EXPIRED = "TIME_EXPIRED";
    public static final String MANUAL = "MANUAL";
}
