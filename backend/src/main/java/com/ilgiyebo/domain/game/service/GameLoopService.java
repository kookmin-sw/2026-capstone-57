package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.response.*;
import com.ilgiyebo.domain.game.engine.*;
import com.ilgiyebo.domain.game.entity.GameFailReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameLoopService {

    private final GameRoomStore roomStore;
    private final PhysicsEngine physicsEngine;
    private final CollisionEngine collisionEngine;
    private final ScoreEngine scoreEngine;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;
    private final ObjectMapper objectMapper;

    private static final double MAX_DELTA_MS = 100.0;
    private static final Duration RECONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Main game loop tick at 20Hz (50ms fixed rate).
     * Iterates all active rooms and processes them based on status.
     */
    @Scheduled(fixedRate = 50)
    public void tick() {
        long now = System.currentTimeMillis();
        Collection<GameRoom> activeRooms = roomStore.getActiveRooms();

        for (GameRoom room : activeRooms) {
            GameRoomStatus status = room.getStatus().get();
            try {
                if (status == GameRoomStatus.PLAYING) {
                    double deltaMs = now - room.getLastTickTime();
                    deltaMs = Math.min(deltaMs, MAX_DELTA_MS);
                    room.setLastTickTime(now);
                    processRoom(room, deltaMs);
                } else if (status == GameRoomStatus.PAUSED) {
                    handlePausedRoom(room);
                }
            } catch (Exception e) {
                log.error("Error processing room {}: {}", room.getSessionId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Processes a single game room for one tick.
     */
    private void processRoom(GameRoom room, double deltaMs) {
        GameState state = room.getGameState();
        if (state == null) return;

        // 1. Apply input from buffer
        room.getInputBuffer().forEach((userId, input) -> {
            PlayerState player = state.getPlayers().get(userId);
            if (player != null) {
                physicsEngine.applyInput(player, input, deltaMs);
            }
        });

        // 2. Apply physics (gravity + movement)
        for (PlayerState player : state.getPlayers().values()) {
            physicsEngine.applyGravity(player, deltaMs);
            physicsEngine.applyMovement(player, deltaMs);
        }

        // 3. Resolve collisions
        MapData map = room.getMapData();
        for (PlayerState player : state.getPlayers().values()) {
            collisionEngine.resolveFloorCollision(player, map.getPlatforms());
        }
        collisionEngine.checkSwitches(state, map.getSwitches());
        collisionEngine.checkDoor(state, map.getDoors());
        collisionEngine.checkGoal(state, map.getGoal());

        // 4. Update time
        state.setRemainingTimeMs(state.getRemainingTimeMs() - deltaMs);
        state.setElapsedTimeMs(state.getElapsedTimeMs() + (long) deltaMs);

        // 5. Update score
        scoreEngine.updateScore(state);

        // 6. Check game end conditions (CAS to prevent duplicate completion)
        if (isGameCleared(state)) {
            if (room.getStatus().compareAndSet(GameRoomStatus.PLAYING, GameRoomStatus.FINISHED)) {
                handleGameCleared(room);
            }
        } else if (state.getRemainingTimeMs() <= 0) {
            if (room.getStatus().compareAndSet(GameRoomStatus.PLAYING, GameRoomStatus.FINISHED)) {
                handleGameOver(room);
            }
        } else {
            // 7. Broadcast state update (snapshot copy)
            broadcastState(room);
        }
    }

    /**
     * Handles a paused room: checks reconnect timeout.
     * If timeout exceeded, fails the game with DISCONNECTED reason.
     */
    private void handlePausedRoom(GameRoom room) {
        Instant disconnectedAt = room.getDisconnectedAt();
        if (disconnectedAt == null) return;

        if (Duration.between(disconnectedAt, Instant.now()).compareTo(RECONNECT_TIMEOUT) > 0) {
            // Reconnect timeout expired
            if (room.getStatus().compareAndSet(GameRoomStatus.PAUSED, GameRoomStatus.FINISHED)) {
                log.info("재연결 타임아웃 → 게임 종료: sessionId={}", room.getSessionId());
                handleGameOverDisconnected(room);
            }
        }
    }

    private void handleGameCleared(GameRoom room) {
        GameState state = room.getGameState();
        int finalScore = scoreEngine.calculateFinalScore(state);
        long clearTimeMs = state.getElapsedTimeMs();
        int intimacyPoints = scoreEngine.calculateIntimacyPoints(finalScore, clearTimeMs);
        String finalStateJson = serializeState(state);

        try {
            gameSessionService.completeGame(room.getSessionId(), finalScore, clearTimeMs, finalStateJson);
        } catch (Exception e) {
            log.error("Failed to save game completion for session {}: {}", room.getSessionId(), e.getMessage());
        }

        GameClearedEvent event = new GameClearedEvent(
                "GAME_CLEARED",
                finalScore,
                clearTimeMs,
                intimacyPoints
        );
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);

        roomStore.remove(room.getSessionId());
        log.info("게임 클리어: sessionId={}, score={}, clearTimeMs={}", room.getSessionId(), finalScore, clearTimeMs);
    }

    private void handleGameOver(GameRoom room) {
        GameState state = room.getGameState();
        int partialScore = scoreEngine.calculatePartialScore(state);
        String finalStateJson = serializeState(state);

        try {
            gameSessionService.failGame(room.getSessionId(), GameFailReason.TIMEOUT, partialScore, finalStateJson);
        } catch (Exception e) {
            log.error("Failed to save game failure for session {}: {}", room.getSessionId(), e.getMessage());
        }

        GameOverEvent event = new GameOverEvent(
                "GAME_OVER",
                new GameOverResultDto("TIMEOUT", partialScore, state.getElapsedTimeMs())
        );
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);

        roomStore.remove(room.getSessionId());
        log.info("게임 타임아웃: sessionId={}, partialScore={}", room.getSessionId(), partialScore);
    }

    private void handleGameOverDisconnected(GameRoom room) {
        GameState state = room.getGameState();
        int partialScore = state != null ? scoreEngine.calculatePartialScore(state) : 0;
        String finalStateJson = state != null ? serializeState(state) : "{}";

        try {
            gameSessionService.failGame(room.getSessionId(), GameFailReason.DISCONNECTED, partialScore, finalStateJson);
        } catch (Exception e) {
            log.error("Failed to save disconnection failure for session {}: {}", room.getSessionId(), e.getMessage());
        }

        GameOverEvent event = new GameOverEvent(
                "GAME_OVER",
                new GameOverResultDto("DISCONNECTED", partialScore,
                        state != null ? state.getElapsedTimeMs() : 0)
        );
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);

        roomStore.remove(room.getSessionId());
        log.info("연결 끊김으로 게임 종료: sessionId={}", room.getSessionId());
    }

    private boolean isGameCleared(GameState state) {
        return state.getPlayers().values().stream().allMatch(PlayerState::isAtGoal);
    }

    private void broadcastState(GameRoom room) {
        StateUpdateEvent event = new StateUpdateEvent("STATE_UPDATE", room.getGameState().toSnapshot());
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);
    }

    private String gameTopic(java.util.UUID sessionId) {
        return "/topic/game/" + sessionId;
    }

    private String serializeState(GameState state) {
        try {
            return objectMapper.writeValueAsString(state.toSnapshot());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize game state: {}", e.getMessage());
            return "{}";
        }
    }
}
