package com.ilgiyebo.domain.game.engine;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 릴레이 모드 전용 경량 게임 상태.
 * 서버가 물리 시뮬레이션 없이 이벤트 검증만 수행할 때 사용한다.
 */
@Getter
@Setter
public class RelayGameState {

    private final Set<String> collectedCoins = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> switchStates = new ConcurrentHashMap<>();
    private boolean doorOpen;
    private int totalCoins;
    private long startTimeMs;
    private long timeLimitMs;
    private final Map<UUID, PositionUpdateData> lastPositions = new ConcurrentHashMap<>();

    public RelayGameState(int totalCoins, long timeLimitMs) {
        this.totalCoins = totalCoins;
        this.timeLimitMs = timeLimitMs;
        this.startTimeMs = System.currentTimeMillis();
        this.doorOpen = false;
    }

    /**
     * 코인 수집 시도. 이미 수집된 코인이면 false 반환.
     */
    public boolean collectCoin(String coinId) {
        return collectedCoins.add(coinId);
    }

    /**
     * 모든 코인이 수집되었는지 확인.
     */
    public boolean allCoinsCollected() {
        return collectedCoins.size() >= totalCoins;
    }

    /**
     * 현재까지 수집된 코인 수.
     */
    public int getCollectedCoinCount() {
        return collectedCoins.size();
    }

    /**
     * 경과 시간(ms).
     */
    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * 제한 시간 초과 여부.
     */
    public boolean isTimedOut() {
        return getElapsedTimeMs() > timeLimitMs;
    }

    /**
     * 스위치 상태 업데이트.
     */
    public void setSwitchState(String switchId, boolean pressed) {
        switchStates.put(switchId, pressed);
    }

    /**
     * 마지막 위치 업데이트.
     */
    public void updateLastPosition(UUID userId, PositionUpdateData position) {
        lastPositions.put(userId, position);
    }
}
