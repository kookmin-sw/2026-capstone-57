package com.ilgiyebo.domain.game.dto.response;

public record RestartRequestedEvent(
        String type,
        String userId
) implements GameEvent {}
