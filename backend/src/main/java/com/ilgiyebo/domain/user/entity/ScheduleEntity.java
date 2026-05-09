package com.ilgiyebo.domain.user.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.function.Function;

@Entity
@Table(name = "`SCHEDULE`")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ScheduleEntity extends BaseSchema {

    @Column(nullable = false)
    @NotNull
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    @NotNull
    private LocalTime startedAt;

    @Column(nullable = false)
    @NotNull
    private LocalTime endedAt;

    @Column(nullable = false)
    @NotNull
    private String place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_building_id")
    private CampusBuildingEntity campusBuilding;

    @Column(name = "floor")
    private Integer floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public static ScheduleEntity fromEverytime(JsonNode name, JsonNode node, UserEntity user) {

        Function<Integer, LocalTime> fromEverytimeTime = time -> {
            int minute = time * 5;
            int hour = minute / 60;
            minute %= 60;
            return LocalTime.of(hour, minute, 0);
        };

        return ScheduleEntity.builder()
                .name(name.get("value").asText())
                .dayOfWeek(DayOfWeek.of(Integer.parseInt(node.get("day").asText()) + 1))
                .startedAt(fromEverytimeTime.apply(Integer.parseInt(node.get("starttime").asText())))
                .endedAt(fromEverytimeTime.apply(Integer.parseInt(node.get("endtime").asText())))
                .place(node.get("place").asText())
                .user(user)
                .build();
    }
}
