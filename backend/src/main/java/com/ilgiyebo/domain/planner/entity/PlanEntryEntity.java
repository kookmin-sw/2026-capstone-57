package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "PLAN_ENTRY")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class PlanEntryEntity extends BaseSchema {

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column
    private String location;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PlanItemType type;
}
