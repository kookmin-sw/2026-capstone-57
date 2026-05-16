package com.ilgiyebo.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EditReviewRequest(
    @NotBlank @Size(max = 5000) String reflection,
    @NotNull @Min(1) @Max(5) Integer satisfaction,
    @NotNull Boolean wantToMeetAgain
) {}
