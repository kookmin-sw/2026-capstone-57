package com.ilgiyebo.domain.planner.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanDto(
    LocalDate date,
    List<PlanItemDto> entries
) {}
