package com.ilgiyebo.domain.review.dto;

import java.util.UUID;

public record GeneratedReviewPreview(
    UUID sessionId,
    String generatedContent,
    int suggestedSatisfaction
) {}
