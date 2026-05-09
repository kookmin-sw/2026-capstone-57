package com.ilgiyebo.domain.planner.dto;

import com.ilgiyebo.domain.planner.entity.PlanItemType;
import java.time.LocalTime;

public record PlanItemDto(
    LocalTime startTime,
    LocalTime endTime,
    String location,
    String activity,
    PlanItemType type
) {}
