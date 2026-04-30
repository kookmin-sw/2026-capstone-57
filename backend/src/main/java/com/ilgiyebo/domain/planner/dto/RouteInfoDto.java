package com.ilgiyebo.dto;

import java.time.LocalDate;
import java.util.List;

public record RouteInfoDto(
    String userId,
    LocalDate date,
    List<LocationTimeDto> locations
) {}
