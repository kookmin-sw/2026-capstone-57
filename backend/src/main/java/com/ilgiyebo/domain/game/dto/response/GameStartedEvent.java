package com.ilgiyebo.domain.game.dto.response;

public record GameStartedEvent(
        String type,
        GameStateSnapshot initialState,
        MapDataDto mapData
) implements GameSocketEvent {}
