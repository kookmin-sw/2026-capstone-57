package com.ilgiyebo.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatSessionCreateRequest(
        @NotNull UUID matchId
) {
}
