package com.ilgiyebo.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatSendRequest(
        @NotBlank String content
) {
}
