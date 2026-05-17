package com.ilgiyebo.domain.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleAutoGenerateResult(
        int createdCount,
        int skippedCount,
        List<SkippedSchedule> skippedSchedules
) {
    public record SkippedSchedule(
            LocalDate date,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {}

    public static ScheduleAutoGenerateResult success(int createdCount) {
        return new ScheduleAutoGenerateResult(createdCount, 0, List.of());
    }
}
