# Implementation Plan: coop-game-networking

## Overview

온라인 2인 협동 퍼즐 플랫포머 네트워킹 구현. 클라이언트(TypeScript/Phaser)와 서버(Java/Spring Boot) 양쪽에 릴레이 기반 멀티플레이어 아키텍처를 구축한다. 클라이언트 측 물리를 유지하면서 서버가 위치 중계 및 게임 이벤트 검증을 담당한다.

## Tasks

- [x] 1. Client infrastructure - 타입 정의 및 환경 설정
  - [x] 1.1 Create network message type definitions
    - Create `phaser_game/src/types/network.ts` with all STOMP message interfaces
    - Define `GameActionMessage`, `PositionUpdatePayload`, `GameEventPayload`
    - Define all `ServerMessageType` and `ServerMessagePayloadMap` interfaces
    - Define `ConnectionState` type and `PlayerAnimation` type
    - _Requirements: 3.2, 4.1, 5.1, 6.1_
  - [x] 1.2 Configure environment variables and Vite proxy
    - Create `phaser_game/.env.development` with `VITE_WS_URL=http://localhost:8080/ws/game` and `VITE_API_BASE_URL=http://localhost:8080/api`
    - Create `phaser_game/.env.production` with production URL placeholders
    - Update `phaser_game/types/process.env.ts` to add VITE_ environment variable types
    - Update `phaser_game/vite/config.dev.mjs` to add proxy for `/api` → `http://localhost:8081` and `/ws` → `http://localhost:8081` (ws: true)
    - _Requirements: 9.1, 9.2, 9.5_
  - [x] 1.3 Install client dependencies
    - Add `@stomp/stompjs` (^7.x), `sockjs-client` (^1.6.x) to dependencies
    - Add `@types/sockjs-client` (^1.5.x) to devDependencies
    - _Requirements: 1.1_

- [x] 2. Client infrastructure - NetworkService 구현
  - [x] 2.1 Implement NetworkService class
    - Create `phaser_game/src/services/NetworkService.ts`
    - Implement STOMP-over-SockJS connection to `/ws/game` endpoint
    - Include JWT token in STOMP CONNECT frame headers
    - Subscribe to `/topic/game/{gameSessionId}` on connection
    - Implement event emitter pattern for server message dispatch (on/off methods)
    - Implement `sendReady()`, `sendPositionUpdate()`, `sendGameEvent()` methods
    - Expose `connectionState` and `isConnected` properties
    - _Requirements: 1.1, 1.2, 1.5, 1.6, 2.2, 3.4_
  - [x] 2.2 Implement reconnection with exponential backoff
    - Implement automatic reconnection on disconnect (1s, 2s, 4s, 8s, max 30s)
    - Manage connection state transitions: DISCONNECTED→CONNECTING→CONNECTED→RECONNECTING→ERROR
    - Emit authentication error event on JWT failure
    - _Requirements: 1.3, 1.4, 1.6, 8.1_
  - [ ]* 2.3 Write property test for exponential backoff delay
    - **Property 1: Exponential backoff delay correctness**
    - Test that `computeRetryDelay(n)` equals `min(2^n * 1000, 30000)` for all n ≥ 0
    - Use fast-check with `fc.nat({max: 20})` generator
    - **Validates: Requirements 1.3, 8.1**
  - [ ]* 2.4 Write property test for connection state machine
    - **Property 2: Connection state machine valid transitions**
    - Test that only valid state transitions occur for any sequence of connection events
    - Use fast-check with random event sequence generator
    - **Validates: Requirements 1.6**

- [x] 3. Server relay - RelayService 구현
  - [x] 3.1 Create RelayGameState and PositionUpdateData models
    - Create `backend/.../game/engine/RelayGameState.java` with collected coins set, switch states, door open status, total coins, timing, last positions
    - Create `backend/.../game/dto/request/PositionUpdateData.java` record
    - _Requirements: 11.4_
  - [x] 3.2 Create server response event DTOs
    - Create `PartnerPositionEvent.java`, `CoinConfirmedEvent.java`, `CoinRejectedEvent.java`
    - Create `SwitchStateEvent.java`, `DoorOpenedEvent.java`, `GameClearedEvent.java`, `GameOverEvent.java`
    - Create `PlayerDisconnectedEvent.java`, `PlayerReconnectedEvent.java`
    - _Requirements: 4.1, 5.3, 6.3, 7.1, 8.4, 8.5_
  - [x] 3.3 Implement RelayService interface and RelayServiceImpl
    - Create `backend/.../game/service/RelayService.java` interface
    - Create `backend/.../game/service/RelayServiceImpl.java`
    - Implement `relayPosition()`: forward position data to partner without modification
    - Implement `handleCoinCollected()`: validate coin not already collected, broadcast COIN_CONFIRMED or COIN_REJECTED
    - Implement `handleSwitchEvent()`: update switch state, broadcast SWITCH_STATE
    - Implement door opening logic: broadcast DOOR_OPENED when all coins collected
    - Implement `handleClearRequest()`: validate both players near door, broadcast GAME_CLEARED
    - Discard POSITION_UPDATE when session not in PLAYING state
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  - [x] 3.4 Update GameController for relay message types
    - Modify `GameActionMessage.java` to add `payload` field (Map or JsonNode)
    - Add cases for `POSITION_UPDATE`, `COIN_COLLECTED`, `SWITCH_PRESSED`, `SWITCH_RELEASED`, `CLEAR_REQUEST` in GameController
    - Route new message types to RelayService
    - _Requirements: 3.4, 5.1, 6.1, 6.2, 7.3_
  - [ ]* 3.5 Write property test for server authoritative state consistency
    - **Property 6: Server authoritative state consistency**
    - Test that coins can only be collected once, door opens iff all coins collected, GAME_CLEARED iff door open AND both players near door
    - Use jqwik with random event sequence generator
    - **Validates: Requirements 5.2, 7.1, 7.4, 11.3, 11.4**
  - [ ]* 3.6 Write property test for position relay identity
    - **Property 7: Position relay identity**
    - Test that relayed position data is identical to original payload
    - Use jqwik with random PositionUpdateData generator
    - **Validates: Requirements 11.1**
  - [ ]* 3.7 Write property test for session lifecycle state machine
    - **Property 8: Session lifecycle state machine validity**
    - Test valid state transitions and POSITION_UPDATE discard in non-PLAYING states
    - Use jqwik with random session event sequence generator
    - **Validates: Requirements 11.6, 11.7**

- [ ] 4. Checkpoint - Server relay tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Client scenes - NetworkGameManager 구현
  - [x] 5.1 Implement NetworkGameManager class
    - Create `phaser_game/src/services/NetworkGameManager.ts`
    - Implement `initialize()` with NetworkGameConfig, `destroy()`, `update(time, delta)`
    - Send position updates at 50ms interval (only when position changed)
    - Apply linear interpolation (lerp) to partner sprite position on PARTNER_POSITION received
    - Handle COIN_CONFIRMED/COIN_REJECTED: optimistic update + rollback on rejection
    - Handle SWITCH_STATE: update switch visual and platform position
    - Handle DOOR_OPENED: change door sprite to open state
    - Handle GAME_CLEARED: trigger clear sequence
    - Handle PLAYER_DISCONNECTED/RECONNECTED: alpha effect on partner sprite
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.4, 5.5, 5.6, 6.4, 6.5, 7.2, 7.5, 8.4, 8.5, 10.4_
  - [ ]* 5.2 Write property test for position message completeness
    - **Property 3: Position message completeness**
    - Test that serialized PositionUpdatePayload contains all six fields matching input state
    - Use fast-check with `fc.record({x: fc.float({min:0, max:390}), y: fc.float({min:0, max:844}), ...})`
    - **Validates: Requirements 3.2**
  - [ ]* 5.3 Write property test for delta-only position transmission
    - **Property 4: Delta-only position transmission**
    - Test that position message is sent iff at least one field differs between consecutive states
    - Use fast-check with two random PlayerState generators
    - **Validates: Requirements 3.3**
  - [ ]* 5.4 Write property test for linear interpolation bounds
    - **Property 5: Linear interpolation bounds**
    - Test that interpolated position stays within bounds of the two input positions for any t in [0,1]
    - Use fast-check with `fc.float({min:0, max:1})` and two random coordinates
    - **Validates: Requirements 4.2**

- [x] 6. Client scenes - Lobby Scene 구현
  - [x] 6.1 Implement Lobby scene
    - Create `phaser_game/src/scenes/Lobby.ts` extending Phaser.Scene
    - Extract `sessionId` and `token` from URL query parameters
    - If sessionId is missing, show "세션 연결 실패" error text (do NOT fall back to local mode from Lobby)
    - Call `GET /api/v1/game-sessions/{gameSessionId}` to retrieve session info
    - Instantiate NetworkService and connect with token and sessionId
    - Send READY message after connection established
    - Display placeholder text-based UI: connection status, waiting indicator (user will add custom assets later)
    - On ROOM_STATE received: show both players' ready status
    - On GAME_STARTED received: transition to Level scene with player role and session config
    - Handle connection errors: display error message with retry button
    - _Requirements: 1.1, 1.5, 2.1, 2.2, 2.3, 2.4, 12.1, 12.2_

- [ ] 7. Client scenes - Level.ts 온라인 모드 통합
  - [ ] 7.1 Modify Preload.ts for session routing
    - In Preload.ts `create()` method, check URL for `sessionId` parameter
    - If sessionId present: route to Lobby scene
    - If sessionId missing AND dev flag/env variable `VITE_LOCAL_MODE=true`: route to Level (local 2-player mode)
    - If sessionId missing AND no dev flag: show "세션 연결 실패" error (production behavior)
    - _Requirements: 12.1, 12.3_
  - [ ] 7.2 Integrate NetworkGameManager into Level.ts
    - In Level.ts `create()` within START-USER-CODE block, check for online mode config passed from Lobby
    - If online mode: instantiate NetworkGameManager with scene references (sprites, coins, switches, door, scoreText)
    - If online mode: disable input for partner player (no WASD controls for player2 if assigned player1, vice versa)
    - If online mode: skip tutorial overlay
    - Call `networkGameManager.update(time, delta)` in Level.ts `update()` method
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 12.4, 12.5_
  - [ ] 7.3 Register Lobby scene in main.ts
    - Import Lobby scene in `phaser_game/src/main.ts`
    - Add Lobby to the Phaser.Game scene array
    - _Requirements: 2.3_

- [ ] 8. Client scenes - Result Scene 구현
  - [ ] 8.1 Implement Result scene
    - Create `phaser_game/src/scenes/Result.ts` extending Phaser.Scene
    - Display score, clear time, intimacy points on GAME_CLEARED
    - Display failure reason on GAME_OVER (TIMEOUT or DISCONNECTED)
    - Provide "돌아가기" button (window.history.back or app navigation)
    - Placeholder text-based UI
    - _Requirements: 2.5, 2.6_

- [ ] 9. Client - 연결 끊김 처리 UI
  - [ ] 9.1 Implement reconnection overlay in Level scene
    - On connection lost: display "재연결 중..." overlay, freeze game (physics pause)
    - On reconnection success: remove overlay, send READY, resume game
    - On reconnection failure (30s timeout): display "연결 실패" message with lobby return button
    - _Requirements: 8.1, 8.2, 8.3, 8.6_

- [ ] 10. Checkpoint - Client integration complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Integration wiring and final verification
  - [ ] 11.1 End-to-end message flow verification
    - Verify full flow: Lobby connect → READY → GAME_STARTED → position exchange → coin collect → door open → clear
    - Ensure Vite proxy correctly routes `/api` and `/ws` to backend on port 8081
    - Verify environment variables are loaded correctly in dev mode
    - _Requirements: 1.1, 2.2, 2.4, 3.4, 5.3, 7.4, 9.1, 9.2_
  - [ ]* 11.2 Write unit tests for NetworkService
    - Test connection/subscription/message sending with mock STOMP client
    - Test reconnection behavior and state transitions
    - _Requirements: 1.1, 1.3, 1.5, 1.6_
  - [ ]* 11.3 Write unit tests for NetworkGameManager
    - Test coin optimistic update + rollback on rejection
    - Test partner position lerp application
    - Test switch state synchronization
    - Test partner disconnect/reconnect visual feedback
    - _Requirements: 4.2, 5.5, 5.6, 6.4, 8.4, 8.5_
  - [ ]* 11.4 Write integration tests for RelayService
    - Test coin double-collection prevention
    - Test door open condition (all coins collected)
    - Test clear validation (both players near door)
    - Test position relay without modification
    - _Requirements: 5.2, 7.1, 7.4, 11.1, 11.3_

- [ ] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Client dev server runs on port 8080, backend on port 8081 (Vite proxy handles routing)
- Lobby scene uses placeholder text-based UI (user will add custom assets later)
- When sessionId is missing from URL, production behavior shows "세션 연결 실패" error; local 2-player mode is dev-only (enabled via `VITE_LOCAL_MODE=true`)
