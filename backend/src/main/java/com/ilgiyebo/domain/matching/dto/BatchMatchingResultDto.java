package com.ilgiyebo.domain.matching.dto;

import java.util.List;

public record BatchMatchingResultDto(
    int totalProcessed,
    int matchesCreated,
    int quickMatches,
    int normalMatches,
    List<String> failedSlots
) {}
