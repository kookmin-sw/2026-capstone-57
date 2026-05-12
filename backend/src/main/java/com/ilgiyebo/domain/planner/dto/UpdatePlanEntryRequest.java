package com.ilgiyebo.domain.planner.dto;

import com.ilgiyebo.domain.planner.entity.PlanItemType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdatePlanEntryRequest(
        @NotNull(message = "날짜는 필수입니다") LocalDate date,
        @NotNull(message = "시작 시간은 필수입니다") LocalTime startTime,
        @NotNull(message = "종료 시간은 필수입니다") LocalTime endTime,
        String location,
        String name,
        @NotNull(message = "일정 유형은 필수입니다") PlanItemType type
) {}
