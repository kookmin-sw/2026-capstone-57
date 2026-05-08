package com.ilgiyebo.domain.review.dto;

public record DirectReviewInputDto(
    int satisfaction,
    String reflection,
    boolean wantToMeetAgain
) {}
