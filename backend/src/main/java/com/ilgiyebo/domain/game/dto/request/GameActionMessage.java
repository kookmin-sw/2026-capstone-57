package com.ilgiyebo.domain.game.dto.request;

import jakarta.validation.constraints.NotNull;

public record GameActionMessage(
        @NotNull String type,
        PlayerInputData input
) {}
