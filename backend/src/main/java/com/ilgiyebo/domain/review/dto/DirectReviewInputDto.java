package com.ilgiyebo.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectReviewInputDto(
    @NotNull @Min(1) @Max(5) Integer satisfaction,
    @NotBlank @Size(max = 5000) String reflection,
    @NotNull Boolean wantToMeetAgain
) {}
