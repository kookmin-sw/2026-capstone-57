package com.ilgiyebo.domain.game.dto.response;

/**
 * 스위치 상태 변경 이벤트.
 * 스위치가 눌리거나 해제될 때 양쪽 클라이언트에게 브로드캐스트한다.
 */
public record SwitchStateEvent(
        String type,
        String switchId,
        boolean pressed,
        String pressedBy
) implements GameSocketEvent {}
