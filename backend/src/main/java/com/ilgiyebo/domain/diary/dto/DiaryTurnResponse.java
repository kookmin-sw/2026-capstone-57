package com.ilgiyebo.domain.diary.dto;

import java.util.UUID;

public record DiaryTurnResponse(
        UUID sessionId,
        boolean isCompleted,
        String nextQuestion,
        int currentTurnNumber,
        int maxTurns
) {}
