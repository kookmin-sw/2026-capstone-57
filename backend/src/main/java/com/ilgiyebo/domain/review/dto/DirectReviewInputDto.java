package com.ilgiyebo.dto;

public record DirectReviewInputDto(
    int satisfaction,
    String reflection,
    boolean wantToMeetAgain
) {}
