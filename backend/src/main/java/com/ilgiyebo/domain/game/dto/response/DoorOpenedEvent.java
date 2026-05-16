package com.ilgiyebo.domain.game.dto.response;

/**
 * 문 열림 이벤트.
 * 모든 코인이 수집되면 양쪽 클라이언트에게 브로드캐스트한다.
 */
public record DoorOpenedEvent(
        String type
) implements GameSocketEvent {}
