package com.ilgiyebo.dto;

import com.ilgiyebo.domain.PlanItemType;
import java.time.LocalTime;

public record PlanItemDto(
    LocalTime startTime,
    LocalTime endTime,
    String location,
    String activity,
    PlanItemType type
) {}
