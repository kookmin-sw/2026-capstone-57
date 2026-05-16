package com.ilgiyebo.dto;

import java.util.List;

public record BatchMatchingResultDto(
    int totalProcessed,
    int matchesCreated,
    int quickMatches,
    int normalMatches,
    List<String> failedSlots
) {}
