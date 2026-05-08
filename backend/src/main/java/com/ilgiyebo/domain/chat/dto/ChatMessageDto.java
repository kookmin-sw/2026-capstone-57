package com.ilgiyebo.domain.chat.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatMessageDto(
        UUID messageId,
        UUID sessionId,
        UUID senderId,
        String content,
        Instant createdAt
) {
}
