package com.ilgiyebo.dto;

import com.ilgiyebo.domain.EmotionTag;
import java.time.LocalDate;

public record DiaryInputDto(
    String content,
    EmotionTag emotionTag,
    LocalDate date
) {}
