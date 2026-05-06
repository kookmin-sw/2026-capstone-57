package com.ilgiyebo.domain.interaction.dto;

import com.ilgiyebo.domain.interaction.entity.HintQuestionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record HintQuestionDto(
    UUID id,
    UUID matchId,
    UUID senderId,
    UUID responderId,
    String question,
    String answer,
    HintQuestionStatus status,
    LocalDateTime createdAt,
    Instant answeredAt
) {}
