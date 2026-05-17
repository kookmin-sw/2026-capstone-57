package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.DiaryEntryEntity;
import com.ilgiyebo.domain.diary.entity.DiarySource;
import com.ilgiyebo.domain.diary.entity.EmotionTag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DiaryEntryResponse(
    UUID id,
    UUID userId,
    LocalDate entryDate,
    String content,
    EmotionTag emotionTag,
    DiarySource source,
    UUID aiSessionId,
    int streakCount,
    LocalDateTime createdAt
) {
    public static DiaryEntryResponse from(DiaryEntryEntity entity) {
        return new DiaryEntryResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getEntryDate(),
                entity.getContent(),
                entity.getEmotionTag(),
                entity.getSource(),
                entity.getAiSessionId(),
                entity.getStreakCount(),
                entity.getCreatedAt()
        );
    }
}
