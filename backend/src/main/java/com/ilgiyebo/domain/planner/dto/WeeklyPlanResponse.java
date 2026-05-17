package com.ilgiyebo.domain.planner.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyPlanResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        Map<LocalDate, List<PlanEntryResponse>> dailyPlans
) {}
