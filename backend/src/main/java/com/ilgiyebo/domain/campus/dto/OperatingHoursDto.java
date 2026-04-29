package com.ilgiyebo.dto;

public record OperatingHoursDto(
    TimeRangeDto monday,
    TimeRangeDto tuesday,
    TimeRangeDto wednesday,
    TimeRangeDto thursday,
    TimeRangeDto friday,
    TimeRangeDto saturday,
    TimeRangeDto sunday
) {}
