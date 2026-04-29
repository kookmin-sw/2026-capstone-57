package com.ilgiyebo.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
    @NotBlank String verificationId,
    @NotBlank String password,
    @NotBlank String nickname,
    @NotBlank String major,
    @NotBlank String studentId
) {}
