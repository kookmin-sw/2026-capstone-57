package com.ilgiyebo.domain.matching.dto;

import java.util.List;

public record RouteOverlapDto(
    boolean hasOverlap,
    List<OverlapLocationDto> overlappingLocations
) {}
