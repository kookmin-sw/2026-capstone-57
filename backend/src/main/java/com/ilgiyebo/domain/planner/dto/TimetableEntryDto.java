package com.ilgiyebo.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableEntryDto(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String location,
    String courseName
) {}
