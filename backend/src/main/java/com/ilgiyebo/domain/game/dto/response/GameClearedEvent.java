package com.ilgiyebo.domain.game.dto.response;

public record GameClearedEvent(
        String type,
        int score,
        long clearTimeMs,
        int intimacyPoints
) implements GameSocketEvent {}
