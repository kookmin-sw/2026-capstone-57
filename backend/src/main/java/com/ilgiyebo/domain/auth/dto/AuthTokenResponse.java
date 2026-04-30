package com.ilgiyebo.dto;

import java.util.UUID;

public record AuthTokenResponse(
    String userId,
    String token,
    String refreshToken
) {
    public static AuthTokenResponse from(UUID userId, String accessToken, String refreshToken) {
        return new AuthTokenResponse(userId.toString(), accessToken, refreshToken);
    }
}
