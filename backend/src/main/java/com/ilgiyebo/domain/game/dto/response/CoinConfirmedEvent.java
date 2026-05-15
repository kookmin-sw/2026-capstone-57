package com.ilgiyebo.domain.game.dto.response;

/**
 * 코인 수집 확인 이벤트.
 * 서버가 코인 수집을 검증한 후 양쪽 클라이언트에게 브로드캐스트한다.
 */
public record CoinConfirmedEvent(
        String type,
        String coinId,
        String collectedBy,
        int totalCollected
) implements GameSocketEvent {}
