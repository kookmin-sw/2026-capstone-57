package com.ilgiyebo.dto;

public record VerificationConfirmResponse(
    String verificationId,
    String email,
    boolean verified
) {}
