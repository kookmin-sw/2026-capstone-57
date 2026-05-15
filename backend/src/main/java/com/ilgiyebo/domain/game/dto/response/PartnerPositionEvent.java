package com.ilgiyebo.domain.game.dto.response;

/**
 * 상대방 위치 업데이트 이벤트.
 * 서버가 수신한 POSITION_UPDATE를 topic으로 브로드캐스트한다.
 * 클라이언트는 senderId를 확인하여 자신의 메시지를 무시한다.
 */
public record PartnerPositionEvent(
        String type,
        String senderId,
        double x,
        double y,
        double velocityX,
        double velocityY,
        String animation,
        boolean flipX,
        long timestamp
) implements GameSocketEvent {}
