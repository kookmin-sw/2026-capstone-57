package com.ilgiyebo.domain.game.dto.response;

public record PlayerReconnectedEvent(
        String type,
        String userId
) implements GameSocketEvent {}
