package com.ilgiyebo.domain.interaction.dto;

import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import jakarta.validation.constraints.NotNull;

public record TerminateMatchRequest(
    @NotNull TerminationReason reason
) {}
