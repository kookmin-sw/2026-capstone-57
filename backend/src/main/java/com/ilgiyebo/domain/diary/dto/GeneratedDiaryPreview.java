package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.EmotionTag;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeneratedDiaryPreview(
        UUID sessionId,
        String generatedContent,
        EmotionTag suggestedEmotion,
        LocalDateTime generatedAt
) {}
