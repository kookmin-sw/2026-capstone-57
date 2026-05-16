package com.ilgiyebo.domain.game.dto.response;

import com.ilgiyebo.domain.game.entity.GameFailReason;
import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import com.ilgiyebo.domain.game.entity.GameSessionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record GameSessionResponse(
        UUID id,
        UUID matchId,
        String gameType,
        GameSessionStatus status,
        GameFailReason failReason,
        Integer score,
        Long clearTimeMs,
        Integer intimacyPoints,
        Instant startedAt,
        Instant completedAt,
        LocalDateTime createdAt
) {
    public static GameSessionResponse from(GameSessionEntity entity) {
        return new GameSessionResponse(
                entity.getId(),
                entity.getMatchId(),
                entity.getGameType(),
                entity.getStatus(),
                entity.getFailReason(),
                entity.getScore(),
                entity.getClearTimeMs(),
                entity.getIntimacyPoints(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt()
        );
    }
}
