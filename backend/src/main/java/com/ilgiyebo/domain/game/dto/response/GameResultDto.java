package com.ilgiyebo.domain.game.dto.response;

public record GameResultDto(
        boolean clear,
        int score,
        long clearTimeMs,
        int intimacyPoints
) {}
