package com.ilgiyebo.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
    @NotBlank String verificationId,
    @NotBlank String code
) {}
