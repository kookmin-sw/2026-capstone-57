package com.ilgiyebo.domain.game.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameActionMessage(
        @NotNull String type,
        PlayerInputData input,
        Map<String, Object> payload
) {}
