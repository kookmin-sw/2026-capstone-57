package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.review.entity.ReviewEntity;
import com.ilgiyebo.domain.review.entity.ReviewMode;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID interactionId,
    UUID userId,
    ReviewMode mode,
    int satisfaction,
    String reflection,
    boolean wantToMeetAgain,
    boolean aiGenerated,
    LocalDateTime createdAt
) {
    public static ReviewResponse from(ReviewEntity entity) {
        return new ReviewResponse(
            entity.getId(),
            entity.getInteraction().getId(),
            entity.getUser().getId(),
            entity.getMode(),
            entity.getSatisfaction(),
            entity.getReflection(),
            entity.isWantToMeetAgain(),
            entity.isAiGenerated(),
            entity.getCreatedAt()
        );
    }
}
