package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.request.PositionUpdateData;

import java.util.UUID;

/**
 * 릴레이 모드 서비스 인터페이스.
 * 클라이언트 간 위치 중계 및 게임 이벤트 검증을 담당한다.
 */
public interface RelayService {

    /**
     * 위치 데이터를 상대방에게 그대로 전달한다.
     * PLAYING 상태가 아니면 무시한다.
     */
    void relayPosition(UUID sessionId, UUID senderId, PositionUpdateData position);

    /**
     * 코인 수집 이벤트를 처리한다.
     * 이미 수집된 코인이면 COIN_REJECTED, 아니면 COIN_CONFIRMED를 브로드캐스트한다.
     */
    void handleCoinCollected(UUID sessionId, UUID userId, String coinId);

    /**
     * 스위치 이벤트를 처리한다.
     * 스위치 상태를 업데이트하고 SWITCH_STATE를 브로드캐스트한다.
     */
    void handleSwitchEvent(UUID sessionId, UUID userId, String switchId, boolean pressed);

    /**
     * 클리어 요청을 처리한다.
     * 문이 열려 있고 양쪽 플레이어가 문 근처에 있으면 GAME_CLEARED를 브로드캐스트한다.
     */
    void handleClearRequest(UUID sessionId, UUID userId);
}
