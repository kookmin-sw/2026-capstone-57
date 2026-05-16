package com.ilgiyebo.dto;

import java.time.Instant;

public class VerificationEntry {

    private final String id;
    private final String email;
    private final String code;
    private final Instant expiresAt;
    private int attemptCount;
    private boolean verified;

    public VerificationEntry(String id, String email, String code, Instant expiresAt) {
        this.id = id;
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
        this.verified = false;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getCode() { return code; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttemptCount() { return attemptCount; }
    public void incrementAttemptCount() { this.attemptCount++; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
