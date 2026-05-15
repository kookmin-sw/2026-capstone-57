package com.ilgiyebo.domain.game.engine;

import com.ilgiyebo.domain.game.dto.request.PlayerInputData;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class GameRoom {

    // Immutable identifiers (set at construction)
    private final UUID sessionId;
    private final UUID matchId;
    private final UUID userAId;
    private final UUID userBId;
    private final MapData mapData;

    // Participant state (STOMP thread: write, GameLoop thread: read)
    private final ConcurrentHashMap<UUID, Boolean> readyState = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<UUID> connectedUsers = new CopyOnWriteArraySet<>();

    // Input buffer (STOMP thread: put, GameLoop thread: read)
    private final ConcurrentHashMap<UUID, PlayerInputData> inputBuffer = new ConcurrentHashMap<>();

    // Room status (both threads use CAS)
    private final AtomicReference<GameRoomStatus> status = new AtomicReference<>(GameRoomStatus.WAITING);

    // Game state (GameLoop thread only)
    @Setter
    private GameState gameState;

    // Timing (GameLoop thread writes, volatile for visibility)
    @Setter
    private volatile long lastTickTime;

    // Restart requests (STOMP thread: add, GameLoop thread: read+clear)
    private final CopyOnWriteArraySet<UUID> restartRequests = new CopyOnWriteArraySet<>();

    // Disconnection tracking (Event thread: write, GameLoop thread: read)
    @Setter
    private volatile UUID disconnectedUser;
    @Setter
    private volatile Instant disconnectedAt;

    // Room mode (relay vs server-authoritative)
    @Setter
    private boolean relayMode = false;

    public GameRoom(UUID sessionId, UUID matchId, UUID userAId, UUID userBId, MapData mapData) {
        this.sessionId = sessionId;
        this.matchId = matchId;
        this.userAId = userAId;
        this.userBId = userBId;
        this.mapData = mapData;
    }
}
