package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.review.entity.AiReviewQuestionEntity;

import java.time.Instant;
import java.util.UUID;

public record AiReviewQuestionDto(
    UUID id,
    UUID sessionId,
    String question,
    String answer,
    int questionOrder,
    Instant answeredAt
) {
    public static AiReviewQuestionDto from(AiReviewQuestionEntity entity) {
        return new AiReviewQuestionDto(
            entity.getId(),
            entity.getSessionId(),
            entity.getQuestion(),
            entity.getAnswer(),
            entity.getQuestionOrder(),
            entity.getAnsweredAt()
        );
    }
}
