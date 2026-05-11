# Implementation Plan: 실시간 협동 게임 도메인 (domain.game)

## Overview

서버 authoritative 방식의 2인 협동 퍼즐 플랫포머 게임 백엔드와 Phaser.js 테스트 클라이언트를 구현한다. 단일 GameLoopService가 활성 GameRoom을 순회하며 20 tick/sec로 상태를 갱신하고, STOMP WebSocket을 통해 클라이언트와 실시간 통신한다.

## Tasks

- [x] 1. Entity layer and repository
  - [x] 1.1 Create GameSessionStatus and GameFailReason enums
    - Create `GameSessionStatus` enum: WAITING, PLAYING, COMPLETED, FAILED, EXPIRED
    - Create `GameFailReason` enum: TIMEOUT, DISCONNECTED
    - Package: `com.ilgiyebo.domain.game.entity`
    - _Requirements: 1.5, 6.2, 7.3_

  - [x] 1.2 Create GameSessionEntity
    - Extend `BaseSchema` (UUID id, createdAt, updatedAt)
    - Fields: matchId (UUID), match (ManyToOne lazy to MatchEntity), gameType (String), status (GameSessionStatus), failReason (GameFailReason), score (Integer), clearTimeMs (Long), intimacyPoints (Integer), finalState (String, JSON columnDefinition), startedAt (Instant), completedAt (Instant)
    - Use `@SuperBuilder(toBuilder = true)`, `@NoArgsConstructor`, `@AllArgsConstructor` pattern matching existing entities
    - Add `@Table(indexes = { @Index(name = "idx_game_session_match_id", columnList = "match_id"), @Index(name = "idx_game_session_status", columnList = "status"), @Index(name = "idx_game_session_match_status", columnList = "match_id, status") })`
    - 테이블은 `spring.jpa.hibernate.ddl-auto: update`에 의해 자동 생성됨 (Flyway 미사용)
    - Package: `com.ilgiyebo.domain.game.entity`
    - _Requirements: 1.5, 5.3, 6.2, 8.3, 8.4_

  - [x] 1.3 Create GameSessionRepository
    - JPA Repository for GameSessionEntity
    - Add query: `findByMatchIdAndStatusIn(UUID matchId, List<GameSessionStatus> statuses)` for idempotent session creation
    - Add query: `findByStatusAndCreatedAtBefore(GameSessionStatus status, LocalDateTime before)` for scheduler
    - Package: `com.ilgiyebo.domain.game.repository`
    - _Requirements: 1.4, 9.3_

- [x] 2. Exception and DTO definitions
  - [x] 2.1 Create GameException enum
    - Define enum-based exception following existing `InteractionException` pattern with `toException()` method
    - Error codes: GAME_SESSION_NOT_FOUND, NOT_GAME_PARTICIPANT, GAME_SESSION_NOT_ACTIVE, MATCH_NOT_FOUND, MATCH_NOT_ACTIVE, GAME_ROOM_NOT_FOUND, INVALID_GAME_ACTION
    - Package: `com.ilgiyebo.domain.game.exception`
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

  - [x] 2.2 Create request DTOs
    - `GameActionMessage` record: type (String, @NotNull), input (PlayerInputData, nullable)
    - `PlayerInputData` record: left (boolean), right (boolean), jump (boolean)
    - Package: `com.ilgiyebo.domain.game.dto.request`
    - _Requirements: 3.1, 3.3, 10.3_

  - [x] 2.3 Create response DTOs (GameEvent sealed interface and implementations)
    - `GameEvent` sealed interface with `String type()` method
    - Implementations: `RoomStateEvent`, `GameStartedEvent`, `StateUpdateEvent`, `GameClearedEvent`, `GameOverEvent`, `PlayerDisconnectedEvent`, `PlayerReconnectedEvent`, `RestartRequestedEvent`, `GameErrorEvent`
    - Supporting records: `GameResultDto`, `GameOverResultDto`, `GameStateSnapshot`, `PlayerStateDto`
    - Package: `com.ilgiyebo.domain.game.dto.response`
    - _Requirements: 10.4, 10.6, 5.1, 6.1_

  - [x] 2.4 Create GameSessionResponse DTO
    - Record with fields: id, matchId, gameType, status, failReason, score, clearTimeMs, intimacyPoints, startedAt, completedAt, createdAt
    - Static factory method `from(GameSessionEntity)` for conversion
    - Package: `com.ilgiyebo.domain.game.dto.response`
    - _Requirements: 1.1, 11.1, 11.3_

  - [x] 2.5 Create GameCompletedEvent (Spring ApplicationEvent)
    - Record: matchId, gameSessionId, gameType, score, intimacyPoints, clearTimeMs, cleared (boolean)
    - Package: `com.ilgiyebo.domain.game.dto.event`
    - _Requirements: 5.4, 5.5_

- [ ] 3. Engine layer (runtime objects)
  - [ ] 3.1 Create MapData and supporting geometry classes
    - `MapData` class: width, height, platforms (List<Platform>), switches (List<SwitchArea>), doors (List<DoorArea>), goal (GoalArea), spawnA (Position), spawnB (Position), timeLimitMs (double)
    - `Platform` record: x, y, width, height
    - `SwitchArea` class: id (String), x, y, width, height, assignedTo (UUID nullable)
    - `DoorArea` record: x, y, width, height
    - `GoalArea` record: x, y, width, height
    - `Position` record: x, y
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 4.4_

  - [ ] 3.2 Create PlayerState class
    - Mutable class: x, y, velocityX, velocityY, onGround (boolean), atGoal (boolean)
    - `toDto()` method returning `PlayerStateDto`
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 4.2, 4.3_

  - [ ] 3.3 Create GameState class
    - Fields: players (Map<UUID, PlayerState>), switches (Map<String, Boolean>), doorOpen (boolean), remainingTimeMs (double), elapsedTimeMs (long), cooperationCount (int), score (int)
    - `toSnapshot()` method creating immutable `GameStateSnapshot` for broadcast
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 4.1, 4.5, 8.2_

  - [ ] 3.4 Create GameRoomStatus enum and GameRoom class
    - `GameRoomStatus` enum: WAITING, PLAYING, PAUSED, FINISHED
    - `GameRoom` class with concurrency-safe fields:
      - `sessionId`, `matchId`, `userAId`, `userBId` (final)
      - `readyState`: ConcurrentHashMap<UUID, Boolean>
      - `connectedUsers`: CopyOnWriteArraySet<UUID>
      - `inputBuffer`: ConcurrentHashMap<UUID, PlayerInputData>
      - `status`: AtomicReference<GameRoomStatus>
      - `gameState`: GameState (GameLoop thread only)
      - `lastTickTime`: volatile long
      - `restartRequests`: CopyOnWriteArraySet<UUID>
      - `disconnectedUser`: volatile UUID
      - `disconnectedAt`: volatile Instant
      - `mapData`: final MapData
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 2.2, 2.3, 12.1, 8.5_

  - [ ] 3.5 Create PhysicsEngine
    - `applyInput(PlayerState, PlayerInputData, double deltaMs)`: set velocityX based on left/right, handle jump if onGround
    - `applyGravity(PlayerState, double deltaMs)`: apply gravity to velocityY
    - `applyMovement(PlayerState, double deltaMs)`: update x/y from velocity
    - Constants: MOVE_SPEED, JUMP_VELOCITY, GRAVITY, MAX_FALL_SPEED
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 4.2, 4.3_

  - [ ] 3.6 Create CollisionEngine
    - `resolveFloorCollision(PlayerState, List<Platform>)`: clamp player to platform top, set onGround
    - `checkSwitches(GameState, List<SwitchArea>)`: update switches map based on player positions
    - `checkDoor(GameState, List<DoorArea>)`: set doorOpen when all switches pressed
    - `checkGoal(GameState, GoalArea)`: set atGoal for players in goal area (only when doorOpen)
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 4.4, 4.6_

  - [ ] 3.7 Create ScoreEngine
    - `updateScore(GameState)`: calculate score based on cooperationCount, remaining time
    - `calculateFinalScore(GameState)`: final score on game clear
    - `calculatePartialScore(GameState)`: partial score on timeout
    - `calculateIntimacyPoints(int score, long clearTimeMs)`: convert score to intimacy points
    - Package: `com.ilgiyebo.domain.game.engine`
    - _Requirements: 5.2, 5.4, 6.3_

  - [ ]* 3.8 Write property tests for PhysicsEngine and CollisionEngine
    - **Property P2: 게임 상태 일관성** - remainingTimeMs is monotonically decreasing across ticks
    - **Property P1: 동시성 안전성** - concurrent inputBuffer writes don't lose data
    - **Validates: Requirements 4.2, 4.3, 4.4, 12.2**

- [ ] 4. Service layer
  - [ ] 4.1 Create GameRoomStore interface and InMemoryGameRoomStore implementation
    - Interface methods: `put(UUID, GameRoom)`, `get(UUID)`, `remove(UUID)`, `getActiveRooms()`
    - `InMemoryGameRoomStore`: backed by ConcurrentHashMap<UUID, GameRoom>
    - `getActiveRooms()`: returns rooms where status is PLAYING or PAUSED
    - Package: `com.ilgiyebo.domain.game.service`
    - _Requirements: 8.5, 12.1_

  - [ ] 4.2 Create GameSessionService interface and implementation
    - Interface: `createSession(UUID matchId, UUID requesterId)`, `getSession(UUID gameSessionId, UUID requesterId)`, `startGame(UUID gameSessionId)`, `completeGame(UUID gameSessionId, int score, long clearTimeMs, String finalStateJson)`, `failGame(UUID gameSessionId, GameFailReason reason, int partialScore, String finalStateJson)`, `expireSession(UUID gameSessionId)`
    - `createSession`: validate Match exists + ACTIVE status + requester is participant + idempotent (return existing WAITING/PLAYING session)
    - `completeGame`: set COMPLETED, calculate intimacyPoints, save finalState, publish GameCompletedEvent
    - `failGame`: set FAILED + failReason, save partialScore + finalState, publish GameCompletedEvent(cleared=false)
    - Use `ApplicationEventPublisher` for domain event publishing
    - Package: `com.ilgiyebo.domain.game.service`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.3, 5.4, 6.2, 8.3, 8.4, 11.1, 11.2, 11.3_

  - [ ] 4.3 Create GameRoomService interface and implementation
    - Interface: `registerParticipant(UUID sessionId, UUID userId)`, `setReady(UUID sessionId, UUID userId)`, `bufferInput(UUID sessionId, UUID userId, PlayerInputData input)`, `handleDisconnect(UUID sessionId, UUID userId)`, `handleReconnect(UUID sessionId, UUID userId)`, `requestRestart(UUID sessionId, UUID userId)`
    - `registerParticipant`: validate userId is userA/userB from Match, create GameRoom if not exists, add to readyState
    - `setReady`: set readyState true, if both ready → start game (activate room in GameLoopService, update session status to PLAYING, broadcast GAME_STARTED)
    - `bufferInput`: validate session is PLAYING, put input in ConcurrentHashMap
    - `handleDisconnect`: CAS status PLAYING→PAUSED, set disconnectedUser/disconnectedAt, broadcast PLAYER_DISCONNECTED
    - `handleReconnect`: CAS status PAUSED→PLAYING, clear disconnectedUser, send current state, broadcast PLAYER_RECONNECTED
    - `requestRestart`: add to restartRequests, if both agreed → reset GameState, CAS status to PLAYING, broadcast GAME_STARTED
    - Package: `com.ilgiyebo.domain.game.service`
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 3.1, 3.4, 6.4, 7.1, 7.2, 7.3, 7.4_

  - [ ]* 4.4 Write property tests for GameSessionService
    - **Property P3: 참여자 검증** - non-participant userId always results in rejection
    - **Property P4: 데이터 저장 정책** - completeGame/failGame always persists finalState exactly once
    - **Validates: Requirements 1.3, 2.5, 8.3, 8.4**

- [ ] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. GameLoopService implementation
  - [ ] 6.1 Create GameLoopService with @Scheduled tick
    - `@Scheduled(fixedRate = 50)` for 20Hz tick rate
    - `tick()`: get current time, iterate `roomStore.getActiveRooms()`, process rooms with status PLAYING
    - Calculate real deltaMs per room (now - room.lastTickTime), clamp to MAX_DELTA_MS (100ms)
    - Handle PAUSED rooms: check reconnect timeout (30s), if expired → CAS PAUSED→FINISHED, call failGame(DISCONNECTED)
    - Package: `com.ilgiyebo.domain.game.service`
    - _Requirements: 4.1, 12.2, 12.3_

  - [ ] 6.2 Implement processRoom logic
    - Step 1: Read inputBuffer (ConcurrentHashMap forEach), apply input via PhysicsEngine
    - Step 2: Apply gravity + movement via PhysicsEngine
    - Step 3: Resolve collisions via CollisionEngine (floor, switches, door, goal)
    - Step 4: Update remainingTimeMs and elapsedTimeMs
    - Step 5: Update score via ScoreEngine
    - Step 6: Check game end conditions with CAS (PLAYING→FINISHED)
    - Step 7: Broadcast StateUpdateEvent snapshot via SimpMessagingTemplate
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.1, 6.1_

  - [ ] 6.3 Implement handleGameCleared and handleGameOver
    - `handleGameCleared`: call gameSessionService.completeGame(), broadcast GameClearedEvent with GameResultDto, remove room from store
    - `handleGameOver`: call gameSessionService.failGame(TIMEOUT), broadcast GameOverEvent with GameOverResultDto, remove room from store
    - Use CAS to prevent duplicate completion
    - _Requirements: 5.1, 5.2, 5.3, 6.1, 6.2, 6.3_

  - [ ]* 6.4 Write property tests for GameLoopService
    - **Property P2: 게임 상태 일관성** - GAME_CLEARED occurs only when all players atGoal=true
    - **Property P5: 독립성** - exception in one room does not affect other rooms' tick processing
    - **Validates: Requirements 4.6, 6.1, 12.2**

- [ ] 7. WebSocket configuration
  - [ ] 7.1 Add /ws/game endpoint to existing WebSocketConfig
    - Modify `backend/src/main/java/com/ilgiyebo/domain/chat/config/WebSocketConfig.java`
    - Add `registry.addEndpoint("/ws/game").setAllowedOriginPatterns(...).withSockJS()` in `registerStompEndpoints`
    - Existing StompChannelInterceptor, /topic, /app, /queue/errors prefixes are shared automatically
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 8. Controllers (REST + STOMP)
  - [ ] 8.1 Create GameSessionController (REST)
    - `@RestController`, `@RequestMapping("/api/v1")`
    - `POST /matches/{matchId}/game-sessions`: extract userId from SecurityContext, call gameSessionService.createSession()
    - `GET /game-sessions/{gameSessionId}`: extract userId, call gameSessionService.getSession()
    - Package: `com.ilgiyebo.domain.game.controller`
    - _Requirements: 1.1, 11.1, 11.2, 11.3_

  - [ ] 8.2 Create GameController (STOMP @MessageMapping)
    - `@Controller`
    - `@MessageMapping("/game/{sessionId}/action")`: receive GameActionMessage, extract userId from Principal
    - Route by message type:
      - "READY" → gameRoomService.registerParticipant() + setReady()
      - "PLAYER_INPUT" → validate input structure, gameRoomService.bufferInput()
      - "RESTART_REQUEST" → gameRoomService.requestRestart()
    - Use `@SendToUser("/queue/errors")` for error responses (GameErrorEvent)
    - Validate userId from STOMP Principal (never trust payload)
    - Package: `com.ilgiyebo.domain.game.controller`
    - _Requirements: 2.2, 3.1, 3.2, 3.3, 3.4, 6.4, 10.1, 10.3, 15.5_

- [ ] 9. Event listener for WebSocket connection tracking
  - [ ] 9.1 Create GameEventListener
    - Listen for `SessionSubscribeEvent` and `SessionDisconnectEvent`
    - On SUBSCRIBE to `/topic/game/{sessionId}`: track connection in GameRoom's connectedUsers, broadcast ROOM_STATE (JOIN notification)
    - On DISCONNECT: detect which sessionId the user was in, call gameRoomService.handleDisconnect(), broadcast LEAVE notification
    - Extract userId from StompHeaderAccessor Principal
    - Package: `com.ilgiyebo.domain.game.controller` (or dedicated listener package)
    - _Requirements: 2.1, 2.6, 7.1, 7.2_

- [ ] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Scheduler for expired session cleanup
  - [ ] 11.1 Create GameSessionScheduler
    - `@Component` with `@Scheduled` methods
    - Task 1: Find PLAYING sessions exceeding max game duration → failGame(TIMEOUT)
    - Task 2: Find GameRooms with all users disconnected → remove from store, failGame(DISCONNECTED)
    - Task 3: Find WAITING sessions older than threshold → expireSession() (set EXPIRED)
    - Run every 60 seconds
    - Package: `com.ilgiyebo.domain.game.scheduler`
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] 12. Domain event integration (Interaction domain)
  - [ ] 12.1 Create InteractionGameEventHandler
    - `@Component` in `com.ilgiyebo.domain.interaction.handler` package
    - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for GameCompletedEvent
    - On cleared=true: call interactionService.completeGame(matchId, gameType)
    - On cleared=false: log failure, no action in MVP
    - Error handling: catch + log (GameSession already saved, no rollback needed)
    - _Requirements: 5.4, 5.5_

  - [ ] 12.2 Extend InteractionService with completeGame method
    - Add `void completeGame(UUID matchId, String gameType)` to InteractionService interface
    - Implement in InteractionServiceImpl: find interaction, validate currentStage >= 3, advance to stage 4 with IN_PROGRESS status
    - _Requirements: 5.5_

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Game_Front_Test client setup and implementation
  - [ ] 14.1 Initialize game-front-test project
    - Create `game-front-test/` directory at project root
    - Initialize with Vite + TypeScript template
    - Install dependencies: phaser, @stomp/stompjs, sockjs-client
    - Configure vite.config.ts with dev server proxy to backend (localhost:8080)
    - Create tsconfig.json with strict mode
    - _Requirements: 13.5_

  - [ ] 14.2 Create network layer (StompClient + GameProtocol)
    - `src/network/StompClient.ts`: wrapper around @stomp/stompjs Client with SockJS factory
    - Connect to `/ws/game` with JWT Authorization header
    - Subscribe to `/topic/game/{sessionId}`
    - Publish to `/app/game/{sessionId}/action`
    - `src/network/GameProtocol.ts`: TypeScript interfaces for all message types (GameActionMessage, GameEvent variants, GameStateSnapshot, PlayerStateDto)
    - _Requirements: 13.5, 10.1, 10.2_

  - [ ] 14.3 Create input handlers
    - `src/input/KeyboardInput.ts`: arrow keys (left/right) + spacebar (jump) tracking
    - `src/input/TouchInput.ts`: on-screen touch buttons for mobile (left, right, jump)
    - Both expose boolean state: `{ left, right, jump }`
    - _Requirements: 13.4, 13.7_

  - [ ] 14.4 Create Phaser scenes (Boot, Lobby, Game, Result)
    - `src/scenes/BootScene.ts`: load minimal assets (colored rectangles for players, platforms, switches)
    - `src/scenes/LobbyScene.ts`: input sessionId + JWT token, create session via REST API, connect STOMP, send READY, wait for GAME_STARTED
    - `src/scenes/GameScene.ts`: render players/platforms/switches/door/goal from STATE_UPDATE, send PLAYER_INPUT on input change (boolean snapshot, only when changed)
    - `src/scenes/ResultScene.ts`: display score, clearTime, intimacyPoints from GAME_CLEARED/GAME_OVER
    - _Requirements: 13.5, 13.6, 13.7_

  - [ ] 14.5 Configure Phaser with Scale.FIT and responsive layout
    - Set `scale.mode: Phaser.Scale.FIT`, `autoCenter: CENTER_BOTH`, logical resolution 1280x720
    - `src/ui/OrientationGuard.ts`: detect portrait orientation on mobile, show "가로로 돌려주세요" overlay
    - Support PC, tablet, mobile landscape
    - _Requirements: 13.1, 13.2, 13.3_

  - [ ] 14.6 Create main.ts entry point and config
    - `src/main.ts`: instantiate Phaser.Game with config
    - `src/config.ts`: server URL, WebSocket endpoint, game constants
    - `index.html`: minimal HTML with game container
    - _Requirements: 13.5_

- [ ] 15. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The design uses Java 17 + Spring Boot 3.3.5 for backend and TypeScript + Phaser.js for the test client
- Flyway migration is NOT used — tables are auto-created by `spring.jpa.hibernate.ddl-auto: update`
- Indexes are defined via `@Table(indexes = {...})` annotation on the entity
- The existing WebSocketConfig is modified (not replaced) to add /ws/game endpoint
- GameCompletedEvent uses @TransactionalEventListener(AFTER_COMMIT) to ensure DB consistency before Interaction domain processing
