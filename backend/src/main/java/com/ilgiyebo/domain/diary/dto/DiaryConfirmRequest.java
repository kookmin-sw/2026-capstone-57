package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.EmotionTag;
import jakarta.annotation.Nullable;

public record DiaryConfirmRequest(
        @Nullable String editedContent,
        @Nullable EmotionTag emotionTag
) {}
