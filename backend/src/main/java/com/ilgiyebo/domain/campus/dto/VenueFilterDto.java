package com.ilgiyebo.dto;

import com.ilgiyebo.domain.VenueType;

public record VenueFilterDto(
    VenueType type,
    Integer minMeetingSuitability,
    String nearBuildingId,
    OpenAtFilterDto openAt
) {}
