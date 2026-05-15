package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.request.PositionUpdateData;
import com.ilgiyebo.domain.game.dto.response.*;
import com.ilgiyebo.domain.game.engine.*;
import com.ilgiyebo.domain.game.entity.GameFailReason;
import com.ilgiyebo.domain.game.exception.GameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 릴레이 모드 서비스 구현.
 * 클라이언트 측 물리를 유지하면서 서버가 위치 중계 및 게임 이벤트 검증을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelayServiceImpl implements RelayService {

    private final GameRoomStore roomStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;

    /**
     * 릴레이 모드 게임 상태 저장소.
     * GameRoom과 별도로 릴레이 전용 경량 상태를 관리한다.
     */
    private final Map<UUID, RelayGameState> relayStates = new ConcurrentHashMap<>();

    private static final double DOOR_PROXIMITY_THRESHOLD = 80.0;

    @Override
    public void relayPosition(UUID sessionId, UUID senderId, PositionUpdateData position) {
        GameRoom room = getRoom(sessionId);

        // PLAYING 상태가 아니면 무시
        if (room.getStatus().get() != GameRoomStatus.PLAYING) {
            return;
        }

        // 릴레이 상태에 마지막 위치 저장
        RelayGameState relayState = getOrCreateRelayState(sessionId, room);
        relayState.updateLastPosition(senderId, position);

        // 상대방에게 topic으로 브로드캐스트 (클라이언트가 자신의 메시지는 무시)
        PartnerPositionEvent event = new PartnerPositionEvent(
                "PARTNER_POSITION",
                senderId.toString(),
                position.x(),
                position.y(),
                position.velocityX(),
                position.velocityY(),
                position.animation(),
                position.flipX(),
                position.timestamp()
        );

        messagingTemplate.convertAndSend(gameTopic(sessionId), event);
    }

    @Override
    public void handleCoinCollected(UUID sessionId, UUID userId, String coinId) {
        GameRoom room = getRoom(sessionId);

        if (room.getStatus().get() != GameRoomStatus.PLAYING) {
            return;
        }

        RelayGameState relayState = getOrCreateRelayState(sessionId, room);

        // 코인 수집 시도 (이미 수집된 코인이면 false)
        boolean collected = relayState.collectCoin(coinId);

        if (collected) {
            // 수집 확인 → 양쪽에 브로드캐스트
            CoinConfirmedEvent event = new CoinConfirmedEvent(
                    "COIN_CONFIRMED",
                    coinId,
                    userId.toString(),
                    relayState.getCollectedCoinCount()
            );
            messagingTemplate.convertAndSend(gameTopic(sessionId), event);

            log.debug("코인 수집 확인: sessionId={}, coinId={}, userId={}, total={}",
                    sessionId, coinId, userId, relayState.getCollectedCoinCount());

            // 모든 코인 수집 완료 시 문 열림
            if (relayState.allCoinsCollected() && !relayState.isDoorOpen()) {
                relayState.setDoorOpen(true);
                DoorOpenedEvent doorEvent = new DoorOpenedEvent("DOOR_OPENED");
                messagingTemplate.convertAndSend(gameTopic(sessionId), doorEvent);

                log.info("문 열림: sessionId={}", sessionId);
            }
        } else {
            // 이미 수집된 코인 → 요청자에게만 거부 전송
            CoinRejectedEvent event = new CoinRejectedEvent(
                    "COIN_REJECTED",
                    coinId,
                    userId.toString(),
                    relayState.getCollectedCoinCount()
            );
            messagingTemplate.convertAndSend(gameTopic(sessionId), event);

            log.debug("코인 수집 거부 (이미 수집됨): sessionId={}, coinId={}, userId={}",
                    sessionId, coinId, userId);
        }
    }

    @Override
    public void handleSwitchEvent(UUID sessionId, UUID userId, String switchId, boolean pressed) {
        GameRoom room = getRoom(sessionId);

        if (room.getStatus().get() != GameRoomStatus.PLAYING) {
            return;
        }

        RelayGameState relayState = getOrCreateRelayState(sessionId, room);
        relayState.setSwitchState(switchId, pressed);

        // 양쪽에 브로드캐스트
        SwitchStateEvent event = new SwitchStateEvent(
                "SWITCH_STATE",
                switchId,
                pressed,
                userId.toString()
        );
        messagingTemplate.convertAndSend(gameTopic(sessionId), event);

        log.debug("스위치 상태 변경: sessionId={}, switchId={}, pressed={}, userId={}",
                sessionId, switchId, pressed, userId);
    }

    @Override
    public void handleClearRequest(UUID sessionId, UUID userId) {
        GameRoom room = getRoom(sessionId);

        if (room.getStatus().get() != GameRoomStatus.PLAYING) {
            return;
        }

        RelayGameState relayState = getOrCreateRelayState(sessionId, room);

        // 문이 열려 있는지 확인
        if (!relayState.isDoorOpen()) {
            log.debug("클리어 요청 거부 (문 닫힘): sessionId={}, userId={}", sessionId, userId);
            return;
        }

        // 양쪽 플레이어가 문 근처에 있는지 확인
        if (!areBothPlayersNearDoor(room, relayState)) {
            log.debug("클리어 요청 거부 (플레이어 위치 불충분): sessionId={}, userId={}", sessionId, userId);
            return;
        }

        // 게임 클리어 처리
        if (room.getStatus().compareAndSet(GameRoomStatus.PLAYING, GameRoomStatus.FINISHED)) {
            long clearTimeMs = relayState.getElapsedTimeMs();
            int score = calculateScore(relayState);
            int intimacyPoints = calculateIntimacyPoints(score, clearTimeMs);

            // DB 저장
            try {
                gameSessionService.completeGame(sessionId, score, clearTimeMs, "{}");
            } catch (Exception e) {
                log.error("게임 완료 저장 실패: sessionId={}", sessionId, e);
            }

            // 양쪽에 브로드캐스트
            GameClearedEvent event = new GameClearedEvent(
                    "GAME_CLEARED",
                    score,
                    clearTimeMs,
                    intimacyPoints
            );
            messagingTemplate.convertAndSend(gameTopic(sessionId), event);

            // 릴레이 상태 정리
            relayStates.remove(sessionId);
            roomStore.remove(sessionId);

            log.info("게임 클리어: sessionId={}, score={}, clearTimeMs={}", sessionId, score, clearTimeMs);
        }
    }

    // --- Package-private for testing ---

    RelayGameState getOrCreateRelayState(UUID sessionId, GameRoom room) {
        return relayStates.computeIfAbsent(sessionId, id -> {
            int totalCoins = countTotalCoins(room);
            long timeLimitMs = (long) room.getMapData().getTimeLimitMs();
            return new RelayGameState(totalCoins, timeLimitMs);
        });
    }

    /**
     * 릴레이 상태를 외부에서 설정 (테스트용).
     */
    void setRelayState(UUID sessionId, RelayGameState state) {
        relayStates.put(sessionId, state);
    }

    /**
     * 릴레이 상태 제거 (세션 종료 시).
     */
    public void removeRelayState(UUID sessionId) {
        relayStates.remove(sessionId);
    }

    // --- Private helpers ---

    private boolean areBothPlayersNearDoor(GameRoom room, RelayGameState relayState) {
        PositionUpdateData posA = relayState.getLastPositions().get(room.getUserAId());
        PositionUpdateData posB = relayState.getLastPositions().get(room.getUserBId());

        if (posA == null || posB == null) {
            return false;
        }

        // 맵의 첫 번째 문 영역 사용
        if (room.getMapData().getDoors() == null || room.getMapData().getDoors().isEmpty()) {
            return false;
        }

        DoorArea door = room.getMapData().getDoors().get(0);
        double doorCenterX = door.x() + door.width() / 2;
        double doorCenterY = door.y() + door.height() / 2;

        boolean playerANear = isNearDoor(posA.x(), posA.y(), doorCenterX, doorCenterY);
        boolean playerBNear = isNearDoor(posB.x(), posB.y(), doorCenterX, doorCenterY);

        return playerANear && playerBNear;
    }

    private boolean isNearDoor(double playerX, double playerY, double doorX, double doorY) {
        double dx = playerX - doorX;
        double dy = playerY - doorY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= DOOR_PROXIMITY_THRESHOLD;
    }

    private UUID getPartnerId(GameRoom room, UUID senderId) {
        if (senderId.equals(room.getUserAId())) {
            return room.getUserBId();
        }
        return room.getUserAId();
    }

    private GameRoom getRoom(UUID sessionId) {
        return roomStore.get(sessionId)
                .orElseThrow(GameException.GAME_ROOM_NOT_FOUND::toException);
    }

    private int countTotalCoins(GameRoom room) {
        // 맵 데이터에서 코인 수를 결정한다.
        // 현재 MapData에 코인 목록이 없으므로 기본값 사용.
        // 클라이언트가 GAME_STARTED에서 totalCoins를 받아 사용한다.
        return 21; // 클라이언트 레벨의 실제 코인 수
    }

    private int calculateScore(RelayGameState relayState) {
        // 수집된 코인 수 × 100
        return relayState.getCollectedCoinCount() * 100;
    }

    private int calculateIntimacyPoints(int score, long clearTimeMs) {
        // 기본 10점 + 빠른 클리어 보너스
        int base = 10;
        if (clearTimeMs < 60_000) {
            base += 5; // 1분 이내 클리어 보너스
        }
        return base;
    }

    private String gameTopic(UUID sessionId) {
        return "/topic/game/" + sessionId;
    }
}
