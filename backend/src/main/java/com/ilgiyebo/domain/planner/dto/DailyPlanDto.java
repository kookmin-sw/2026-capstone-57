package com.ilgiyebo.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanDto(
    LocalDate date,
    List<PlanItemDto> entries
) {}
