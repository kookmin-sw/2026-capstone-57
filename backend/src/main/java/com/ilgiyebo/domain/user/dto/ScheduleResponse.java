package com.ilgiyebo.dto;

import com.ilgiyebo.domain.ScheduleEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponse(List<ScheduleItem> schedules) {

    public record ScheduleItem(
            String id,
            String name,
            DayOfWeek dayOfWeek,
            LocalTime startedAt,
            LocalTime endedAt,
            String place
    ) {
        public static ScheduleItem from(ScheduleEntity entity) {
            return new ScheduleItem(
                    entity.getId().toString(),
                    entity.getName(),
                    entity.getDayOfWeek(),
                    entity.getStartedAt(),
                    entity.getEndedAt(),
                    entity.getPlace()
            );
        }
    }

    public static ScheduleResponse from(List<ScheduleEntity> entities) {
        return new ScheduleResponse(
                entities.stream().map(ScheduleItem::from).toList()
        );
    }
}
