package com.ilgiyebo.domain.diary.dto;

import com.ilgiyebo.domain.diary.entity.DiarySessionEntity;
import com.ilgiyebo.domain.diary.entity.DiarySessionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiarySessionResponse(
        UUID sessionId,
        UUID userId,
        LocalDate targetDate,
        DiarySessionStatus status,
        String currentQuestion,
        int currentTurnNumber,
        int maxTurns,
        List<DiaryConversationTurnDto> conversationHistory
) {
    public static DiarySessionResponse from(DiarySessionEntity entity, String currentQuestion) {
        List<DiaryConversationTurnDto> history = entity.getConversationTurns().stream()
                .map(DiaryConversationTurnDto::from)
                .toList();

        return new DiarySessionResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTargetDate(),
                entity.getStatus(),
                currentQuestion,
                entity.getCurrentTurn(),
                entity.getMaxTurns(),
                history
        );
    }
}
