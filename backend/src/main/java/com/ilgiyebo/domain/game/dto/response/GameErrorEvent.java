package com.ilgiyebo.domain.game.dto.response;

public record GameErrorEvent(
        String type,
        String code,
        String message
) implements GameEvent {}
