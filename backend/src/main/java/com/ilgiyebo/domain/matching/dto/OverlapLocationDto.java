package com.ilgiyebo.domain.matching.dto;

public record OverlapLocationDto(
    String fromBuilding,
    String toBuilding,
    String timeRange
) {}
