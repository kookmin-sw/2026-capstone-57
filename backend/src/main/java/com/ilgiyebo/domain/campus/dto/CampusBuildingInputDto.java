package com.ilgiyebo.dto;

import com.ilgiyebo.domain.BuildingPurpose;

public record CampusBuildingInputDto(
    String name,
    CoordinatesDto coordinates,
    BuildingPurpose purpose,
    String description
) {}
