package com.ilgiyebo.dto;

import com.ilgiyebo.domain.VenueType;
import java.util.List;

public record CampusVenueInputDto(
    String name,
    String buildingId,
    CoordinatesDto coordinates,
    VenueType type,
    int meetingSuitability,
    OperatingHoursDto operatingHours,
    List<String> characteristics
) {}
