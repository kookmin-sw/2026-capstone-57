package com.ilgiyebo.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpenAtFilterDto(DayOfWeek dayOfWeek, LocalTime time) {}
