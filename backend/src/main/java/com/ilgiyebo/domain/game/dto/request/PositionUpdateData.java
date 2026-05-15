package com.ilgiyebo.domain.game.dto.request;

/**
 * 클라이언트에서 전송하는 위치 업데이트 데이터.
 * 릴레이 모드에서 상대방에게 그대로 전달된다.
 */
public record PositionUpdateData(
        double x,
        double y,
        double velocityX,
        double velocityY,
        String animation,
        boolean flipX,
        long timestamp
) {}
