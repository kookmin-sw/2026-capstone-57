package com.ilgiyebo.dto;

import java.util.List;

public record RouteOverlapDto(
    boolean hasOverlap,
    List<OverlapLocationDto> overlappingLocations
) {}
