package com.ilgiyebo.domain.game.dto.response;

public record StateUpdateEvent(
        String type,
        GameStateSnapshot state
) implements GameEvent {}
