package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.DiarySource;
import com.ilgiyebo.domain.diary.entity.EmotionTag;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record DiaryInputDto(
    @NotBlank(message = "일기 내용은 비어있을 수 없습니다")
    String content,

    @Nullable
    EmotionTag emotionTag,

    @NotNull(message = "날짜는 필수입니다")
    LocalDate date,

    @Nullable
    DiarySource source,

    @Nullable
    UUID aiSessionId
) {}
