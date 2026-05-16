package com.ilgiyebo.domain.game.dto.response;

public record PlayerStateDto(
        double x,
        double y,
        double velocityX,
        double velocityY,
        boolean onGround,
        boolean atGoal
) {}
