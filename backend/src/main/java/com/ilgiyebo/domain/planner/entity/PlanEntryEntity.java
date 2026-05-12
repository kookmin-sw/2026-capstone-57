package com.ilgiyebo.domain.planner.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "plan_entry", indexes = {
        @Index(name = "idx_plan_entry_user_date", columnList = "user_id, date"),
        @Index(name = "idx_plan_entry_user_source", columnList = "user_id, source")
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class PlanEntryEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Schema(type = "string", example = "09:00:00")
    @Column(name = "start_time", nullable = false)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(type = "string", example = "10:00:00")
    @Column(name = "end_time", nullable = false)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @Column
    private String location;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PlanItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private PlanSource source = PlanSource.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_schedule_id")
    private ScheduleEntity sourceSchedule;
}
