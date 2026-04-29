package com.ilgiyebo.dto;

public record AuthTokenResponse(
    String userId,
    String token,
    String refreshToken
) {}
