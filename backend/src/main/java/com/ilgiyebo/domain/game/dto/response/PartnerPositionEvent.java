package com.ilgiyebo.domain.game.dto.response;

/**
 * 상대방 위치 업데이트 이벤트.
 * 서버가 수신한 POSITION_UPDATE를 상대 클라이언트에게 그대로 전달한다.
 */
public record PartnerPositionEvent(
        String type,
        double x,
        double y,
        double velocityX,
        double velocityY,
        String animation,
        boolean flipX,
        long timestamp
) implements GameSocketEvent {}
