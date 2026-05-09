package com.ilgiyebo.domain.auth.dto;

import com.ilgiyebo.domain.auth.entity.VerificationEntry;

public record VerificationConfirmResponse(
    String verificationId,
    String email,
    boolean verified
) {
    public static VerificationConfirmResponse from(VerificationEntry entry) {
        return new VerificationConfirmResponse(entry.getId(), entry.getEmail(), entry.isVerified());
    }
}
