package com.ilgiyebo.domain.chat.dto;

import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatSessionDto(
        UUID sessionId,
        UUID matchId,
        Instant startTime,
        Instant endTime,
        ChatSessionStatus status
) {
}
