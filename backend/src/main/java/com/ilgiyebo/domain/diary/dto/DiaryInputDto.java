package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.EmotionTag;
import java.time.LocalDate;

public record DiaryInputDto(
    String content,
    EmotionTag emotionTag,
    LocalDate date
) {}
