package com.ilgiyebo.domain.game.dto.response;

/**
 * 코인 수집 거부 이벤트.
 * 이미 수집된 코인을 다시 수집하려 할 때 요청자에게 전송한다.
 */
public record CoinRejectedEvent(
        String type,
        String coinId,
        String reason
) implements GameSocketEvent {}
