package com.ilgiyebo.dto;

public record VerificationConfirmResponse(
    String verificationId,
    String email,
    boolean verified
) {
    public static VerificationConfirmResponse from(VerificationEntry entry) {
        return new VerificationConfirmResponse(entry.getId(), entry.getEmail(), entry.isVerified());
    }
}
