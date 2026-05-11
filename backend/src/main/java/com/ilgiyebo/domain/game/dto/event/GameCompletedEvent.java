package com.ilgiyebo.domain.game.dto.event;

import java.util.UUID;

public record GameCompletedEvent(
        UUID matchId,
        UUID gameSessionId,
        String gameType,
        int score,
        int intimacyPoints,
        long clearTimeMs,
        boolean cleared
) {}
