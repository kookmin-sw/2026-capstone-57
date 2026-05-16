package com.ilgiyebo.domain.game.dto.response;

public record GameOverResultDto(
        String reason,
        int partialScore,
        long elapsedTimeMs
) {}
