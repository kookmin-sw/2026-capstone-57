package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.request.PlayerInputData;
import com.ilgiyebo.domain.game.dto.response.*;
import com.ilgiyebo.domain.game.engine.*;
import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import com.ilgiyebo.domain.game.entity.GameSessionStatus;
import com.ilgiyebo.domain.game.exception.GameException;
import com.ilgiyebo.domain.game.repository.GameSessionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

    private final GameRoomStore roomStore;
    private final GameSessionRepository gameSessionRepository;
    private final MatchRepository matchRepository;
    private final GameSessionService gameSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void registerParticipant(UUID sessionId, UUID userId) {
        GameSessionEntity session = findSession(sessionId);
        MatchEntity match = findMatch(session.getMatchId());

        // Validate userId is userA or userB
        UUID userAId = match.getUserA().getId();
        UUID userBId = match.getUserB().getId();
        if (!userId.equals(userAId) && !userId.equals(userBId)) {
            throw GameException.NOT_GAME_PARTICIPANT.toException();
        }

        // Create GameRoom if not exists
        GameRoom room = roomStore.get(sessionId).orElseGet(() -> {
            MapData mapData = createDefaultMap();
            GameRoom newRoom = new GameRoom(sessionId, session.getMatchId(), userAId, userBId, mapData);
            roomStore.put(sessionId, newRoom);
            return newRoom;
        });

        // Add to readyState (initially not ready)
        room.getReadyState().putIfAbsent(userId, false);

        log.info("참가자 등록: sessionId={}, userId={}", sessionId, userId);

        // Broadcast current room state
        broadcastRoomState(room);
    }

    @Override
    public void setReady(UUID sessionId, UUID userId) {
        GameRoom room = getRoom(sessionId);

        // Validate participant
        if (!room.getReadyState().containsKey(userId)) {
            throw GameException.NOT_GAME_PARTICIPANT.toException();
        }

        // Set ready
        room.getReadyState().put(userId, true);
        log.info("준비 완료: sessionId={}, userId={}", sessionId, userId);

        // Broadcast updated room state
        broadcastRoomState(room);

        // Check if both players are ready
        if (room.getReadyState().size() == 2 && room.getReadyState().values().stream().allMatch(Boolean::booleanValue)) {
            startGame(room);
        }
    }

    @Override
    public void bufferInput(UUID sessionId, UUID userId, PlayerInputData input) {
        GameRoom room = getRoom(sessionId);

        // Only accept input while PLAYING
        if (room.getStatus().get() != GameRoomStatus.PLAYING) {
            return;
        }

        // Buffer the input (latest snapshot overwrites previous)
        room.getInputBuffer().put(userId, input);
    }

    @Override
    public void handleDisconnect(UUID sessionId, UUID userId) {
        GameRoom room = getRoom(sessionId);

        // CAS: PLAYING → PAUSED
        if (room.getStatus().compareAndSet(GameRoomStatus.PLAYING, GameRoomStatus.PAUSED)) {
            room.setDisconnectedUser(userId);
            room.setDisconnectedAt(Instant.now());

            log.info("플레이어 연결 끊김 → 일시정지: sessionId={}, userId={}", sessionId, userId);

            // Broadcast PLAYER_DISCONNECTED to the other player
            PlayerDisconnectedEvent event = new PlayerDisconnectedEvent(
                    "PLAYER_DISCONNECTED", userId.toString());
            messagingTemplate.convertAndSend(gameTopic(sessionId), event);
        }
    }

    @Override
    public void handleReconnect(UUID sessionId, UUID userId) {
        GameRoom room = getRoom(sessionId);

        // CAS: PAUSED → PLAYING
        if (room.getStatus().compareAndSet(GameRoomStatus.PAUSED, GameRoomStatus.PLAYING)) {
            room.setDisconnectedUser(null);
            room.setDisconnectedAt(null);
            room.setLastTickTime(System.currentTimeMillis());

            log.info("플레이어 재연결 → 게임 재개: sessionId={}, userId={}", sessionId, userId);

            // Send current state to reconnected player
            if (room.getGameState() != null) {
                StateUpdateEvent stateEvent = new StateUpdateEvent(
                        "STATE_UPDATE", room.getGameState().toSnapshot());
                messagingTemplate.convertAndSend(gameTopic(sessionId), stateEvent);
            }

            // Broadcast PLAYER_RECONNECTED
            PlayerReconnectedEvent event = new PlayerReconnectedEvent(
                    "PLAYER_RECONNECTED", userId.toString());
            messagingTemplate.convertAndSend(gameTopic(sessionId), event);
        }
    }

    @Override
    public void requestRestart(UUID sessionId, UUID userId) {
        GameRoom room = getRoom(sessionId);

        // Only allow restart requests when game is FINISHED
        GameRoomStatus currentStatus = room.getStatus().get();
        if (currentStatus != GameRoomStatus.FINISHED && currentStatus != GameRoomStatus.WAITING) {
            return;
        }

        room.getRestartRequests().add(userId);

        log.info("재시작 요청: sessionId={}, userId={}", sessionId, userId);

        // Broadcast restart request
        RestartRequestedEvent event = new RestartRequestedEvent(
                "RESTART_REQUESTED", userId.toString());
        messagingTemplate.convertAndSend(gameTopic(sessionId), event);

        // Check if both players agreed to restart
        if (room.getRestartRequests().size() >= 2) {
            restartGame(room);
        }
    }

    // --- Private helpers ---

    private void startGame(GameRoom room) {
        // Initialize GameState
        GameState gameState = initializeGameState(room);
        room.setGameState(gameState);
        room.setLastTickTime(System.currentTimeMillis());

        // CAS: WAITING → PLAYING
        if (!room.getStatus().compareAndSet(GameRoomStatus.WAITING, GameRoomStatus.PLAYING)) {
            return; // Already started by another thread
        }

        // Update session status in DB
        gameSessionService.startGame(room.getSessionId());

        log.info("게임 시작: sessionId={}", room.getSessionId());

        // Broadcast GAME_STARTED with initial state
        PlayerAssignmentDto assignment = new PlayerAssignmentDto(
                room.getUserAId().toString(),
                room.getUserBId().toString()
        );
        int totalCoins = 21; // Default coin count matching client level
        long timeLimitMs = (long) room.getMapData().getTimeLimitMs();

        GameStartedEvent event = new GameStartedEvent(
                "GAME_STARTED",
                room.getSessionId().toString(),
                assignment,
                totalCoins,
                timeLimitMs,
                gameState.toSnapshot(),
                MapDataDto.from(room.getMapData()));
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);
    }

    private void restartGame(GameRoom room) {
        // Reset game state
        GameState gameState = initializeGameState(room);
        room.setGameState(gameState);
        room.setLastTickTime(System.currentTimeMillis());
        room.getRestartRequests().clear();
        room.getInputBuffer().clear();

        // CAS to PLAYING
        room.getStatus().set(GameRoomStatus.PLAYING);

        // Update session status in DB
        gameSessionService.startGame(room.getSessionId());

        log.info("게임 재시작: sessionId={}", room.getSessionId());

        // Broadcast GAME_STARTED with fresh state
        PlayerAssignmentDto assignment = new PlayerAssignmentDto(
                room.getUserAId().toString(),
                room.getUserBId().toString()
        );
        int totalCoins = 21; // Default coin count matching client level
        long timeLimitMs = (long) room.getMapData().getTimeLimitMs();

        GameStartedEvent event = new GameStartedEvent(
                "GAME_STARTED",
                room.getSessionId().toString(),
                assignment,
                totalCoins,
                timeLimitMs,
                gameState.toSnapshot(),
                MapDataDto.from(room.getMapData()));
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);
    }

    private GameState initializeGameState(GameRoom room) {
        MapData map = room.getMapData();

        // Initialize player states at spawn positions
        Map<UUID, PlayerState> players = new HashMap<>();
        players.put(room.getUserAId(), new PlayerState(
                map.getSpawnA().x(), map.getSpawnA().y(), 0, 0, true, false));
        players.put(room.getUserBId(), new PlayerState(
                map.getSpawnB().x(), map.getSpawnB().y(), 0, 0, true, false));

        // Initialize switches (all unpressed)
        Map<String, Boolean> switches = map.getSwitches().stream()
                .collect(Collectors.toMap(SwitchArea::getId, s -> false));

        return new GameState(
                players,
                switches,
                false,                  // doorOpen
                map.getTimeLimitMs(),   // remainingTimeMs
                0L,                     // elapsedTimeMs
                0,                      // cooperationCount
                0                       // score
        );
    }

    private void broadcastRoomState(GameRoom room) {
        Map<String, Boolean> playerStates = room.getReadyState().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue
                ));

        PlayerAssignmentDto assignment = new PlayerAssignmentDto(
                room.getUserAId().toString(),
                room.getUserBId().toString()
        );

        RoomStateEvent event = new RoomStateEvent("ROOM_STATE", playerStates, assignment);
        messagingTemplate.convertAndSend(gameTopic(room.getSessionId()), event);
    }

    private GameRoom getRoom(UUID sessionId) {
        return roomStore.get(sessionId)
                .orElseThrow(GameException.GAME_ROOM_NOT_FOUND::toException);
    }

    private GameSessionEntity findSession(UUID sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(GameException.GAME_SESSION_NOT_FOUND::toException);
    }

    private MatchEntity findMatch(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(GameException.MATCH_NOT_FOUND::toException);
    }

    private String gameTopic(UUID sessionId) {
        return "/topic/game/" + sessionId;
    }

    /**
     * Creates the default COOP_SWITCH map ("별빛 길 열기").
     * Two switches that must be pressed simultaneously to open a door leading to the goal.
     */
    private MapData createDefaultMap() {
        List<Platform> platforms = List.of(
                new Platform(0, 680, 1280, 40),       // Ground floor
                new Platform(100, 500, 200, 20),      // Left platform
                new Platform(980, 500, 200, 20),      // Right platform
                new Platform(500, 400, 280, 20)       // Middle platform (above door)
        );

        List<SwitchArea> switches = List.of(
                new SwitchArea("switch-a", 150, 470, 60, 30, null),
                new SwitchArea("switch-b", 1030, 470, 60, 30, null)
        );

        List<DoorArea> doors = List.of(
                new DoorArea(600, 400, 80, 280)       // Door in the middle
        );

        GoalArea goal = new GoalArea(580, 300, 120, 80);

        Position spawnA = new Position(100, 640);
        Position spawnB = new Position(1180, 640);

        return MapData.builder()
                .width(1280)
                .height(720)
                .platforms(platforms)
                .switches(switches)
                .doors(doors)
                .goal(goal)
                .spawnA(spawnA)
                .spawnB(spawnB)
                .timeLimitMs(180_000)  // 3 minutes
                .build();
    }
}
