package com.ilgiyebo.dto;

import java.util.List;

public record CampusPathInputDto(
    String fromBuildingId,
    String toBuildingId,
    int walkingTimeMinutes,
    List<String> passingVenueIds,
    String description
) {}
