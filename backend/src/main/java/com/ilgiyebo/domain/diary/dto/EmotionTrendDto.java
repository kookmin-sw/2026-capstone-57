package com.ilgiyebo.dto;

import com.ilgiyebo.domain.EmotionTag;
import java.time.LocalDate;

public record EmotionTrendDto(LocalDate date, EmotionTag emotion) {}
