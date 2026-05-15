package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.review.entity.ReviewMode;
import jakarta.validation.constraints.NotNull;

public record SelectReviewModeRequest(
    @NotNull ReviewMode mode
) {}
