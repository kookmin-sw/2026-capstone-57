package com.ilgiyebo.domain.game.dto.response;

public record GameOverEvent(
        String type,
        GameOverResultDto result
) implements GameEvent {}
