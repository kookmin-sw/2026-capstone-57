package com.ilgiyebo.domain.game.dto.response;

import java.util.Map;

public record GameStateSnapshot(
        Map<String, PlayerStateDto> players,
        Map<String, Boolean> switches,
        boolean doorOpen,
        double remainingTimeMs,
        long elapsedTimeMs,
        int cooperationCount,
        int score
) {}
