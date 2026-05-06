package com.ilgiyebo.domain.interaction.dto;

import jakarta.validation.constraints.NotNull;

public record StageAdvanceRequest(
    @NotNull Boolean accept
) {}
