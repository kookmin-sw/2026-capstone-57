package com.ilgiyebo.domain.game.dto.response;

public record PlayerDisconnectedEvent(
        String type,
        String userId
) implements GameEvent {}
