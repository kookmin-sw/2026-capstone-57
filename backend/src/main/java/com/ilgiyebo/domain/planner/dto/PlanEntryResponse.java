package com.ilgiyebo.domain.planner.dto;

import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import com.ilgiyebo.domain.planner.entity.PlanItemType;
import com.ilgiyebo.domain.planner.entity.PlanSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PlanEntryResponse(
        UUID id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String name,
        PlanItemType type,
        PlanSource source,
        UUID sourceScheduleId
) {
    public static PlanEntryResponse from(PlanEntryEntity entity) {
        return new PlanEntryResponse(
                entity.getId(),
                entity.getDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getLocation(),
                entity.getName(),
                entity.getType(),
                entity.getSource(),
                entity.getSourceSchedule() != null ? entity.getSourceSchedule().getId() : null
        );
    }
}
