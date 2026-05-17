package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.DiaryConversationTurnEntity;

import java.time.LocalDateTime;

public record DiaryConversationTurnDto(
        int turnNumber,
        String question,
        String answer,
        LocalDateTime askedAt,
        LocalDateTime answeredAt
) {
    public static DiaryConversationTurnDto from(DiaryConversationTurnEntity entity) {
        return new DiaryConversationTurnDto(
                entity.getTurnNumber(),
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getAskedAt(),
                entity.getAnsweredAt()
        );
    }
}
