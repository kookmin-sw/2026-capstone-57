package com.ilgiyebo.domain.game.dto.response;

public record GameStartedEvent(
        String type,
        String gameSessionId,
        PlayerAssignmentDto playerAssignment,
        int totalCoins,
        long timeLimitMs,
        GameStateSnapshot initialState,
        MapDataDto mapData
) implements GameSocketEvent {}
