package com.ilgiyebo.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
    @NotBlank String verificationId,
    @NotBlank String code
) {}
