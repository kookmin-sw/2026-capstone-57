package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.review.entity.ReviewMode;
import com.ilgiyebo.domain.review.entity.ReviewSessionEntity;
import com.ilgiyebo.domain.review.entity.ReviewSessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewSessionResponse(
    UUID sessionId,
    UUID interactionId,
    UUID userId,
    ReviewMode mode,
    ReviewSessionStatus status,
    LocalDateTime createdAt
) {
    public static ReviewSessionResponse from(ReviewSessionEntity entity) {
        return new ReviewSessionResponse(
            entity.getId(),
            entity.getInteraction().getId(),
            entity.getUser().getId(),
            entity.getMode(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
