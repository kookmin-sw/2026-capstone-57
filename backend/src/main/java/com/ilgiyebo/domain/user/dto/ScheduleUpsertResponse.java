package com.ilgiyebo.domain.user.dto;

import com.ilgiyebo.domain.planner.dto.ScheduleAutoGenerateResult;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;

import java.util.List;

public record ScheduleUpsertResponse(
        List<ScheduleResponse.ScheduleItem> schedules,
        PlannerAutoGenerateInfo plannerResult
) {
    public record PlannerAutoGenerateInfo(
            int createdCount,
            int skippedCount,
            List<ScheduleAutoGenerateResult.SkippedSchedule> skippedSchedules
    ) {
        public static PlannerAutoGenerateInfo from(ScheduleAutoGenerateResult result) {
            return new PlannerAutoGenerateInfo(
                    result.createdCount(),
                    result.skippedCount(),
                    result.skippedSchedules()
            );
        }
    }

    public static ScheduleUpsertResponse from(List<ScheduleEntity> entities, ScheduleAutoGenerateResult result) {
        return new ScheduleUpsertResponse(
                entities.stream().map(ScheduleResponse.ScheduleItem::from).toList(),
                PlannerAutoGenerateInfo.from(result)
        );
    }
}
