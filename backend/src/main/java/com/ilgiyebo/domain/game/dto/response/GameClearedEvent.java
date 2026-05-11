package com.ilgiyebo.domain.game.dto.response;

public record GameClearedEvent(
        String type,
        GameResultDto result
) implements GameSocketEvent {}
