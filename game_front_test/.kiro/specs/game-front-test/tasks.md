# Implementation Plan: game-front-test (별빛 길 열기)

## Overview

Phaser.js + TypeScript 기반 STOMP 실시간 협동 게임 테스트 클라이언트를 구현합니다. 서버 권위적 아키텍처를 따르며, 클라이언트는 입력 전송과 STATE_UPDATE 보간 렌더링만 담당합니다. Vite 빌드, NetworkSystem 싱글톤, 씬 기반 상태 머신, 100ms 보간 지연 전략을 적용합니다.

## Tasks

- [x] 1. Project scaffolding and configuration
  - [x] 1.1 Initialize Vite + TypeScript project with Phaser.js dependencies
    - Create `package.json` with dependencies: phaser, @stomp/stompjs, sockjs-client, and devDependencies: typescript, vite, vitest, fast-check, @types/sockjs-client
    - Create `tsconfig.json` with strict mode enabled
    - Create `vite.config.ts` with default settings
    - Create `index.html` with game container div
    - Create directory structure: `src/game/scenes`, `src/game/objects`, `src/game/systems`, `src/game/types`, `src/game/config`, `src/socket`, `assets/sprites`, `assets/audio`, `assets/shaders`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 16.1, 16.6_

  - [x] 1.2 Create Phaser game configuration and entry point
    - Create `src/game/config/gameConfig.ts` with 1280x720 resolution, Scale.FIT, CENTER_BOTH, Arcade physics
    - Create `src/main.ts` with URL parameter parsing (token, gameSessionId, userId) and Phaser.Game instantiation
    - Create `src/game/PhaserGame.ts` as Phaser.Game wrapper applying config
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.1, 4.2_

- [x] 2. Type definitions and shared constants
  - [x] 2.1 Define all TypeScript interfaces in gameTypes.ts
    - Create `src/game/types/gameTypes.ts` with all server→client message interfaces (GameStatePayload, PlayerStateDto, SwitchStateDto, RoomStateMessage, GameStartedMessage, GameClearedMessage, GameOverMessage, PlayerDisconnectedMessage, PlayerReconnectedMessage, RestartRequestedMessage, GameErrorMessage)
    - Define client→server message interfaces (ReadyMessage, PlayerInputMessage, RestartRequestMessage)
    - Define InputState, GameParams, ConnectionStatus, ReconnectionState, NetworkSystemConfig, Snapshot, CoopSceneData, ResultSceneData interfaces
    - Define PALETTE color constants
    - _Requirements: 16.2, 6.2, 6.3, 6.4_

- [ ] 3. Core systems implementation
  - [ ] 3.1 Implement StompClient wrapper
    - Create `src/socket/stompClient.ts` with @stomp/stompjs Client configuration
    - Implement SockJS webSocketFactory for `/ws/game` endpoint
    - Configure connectHeaders with `Authorization: Bearer {token}` (STOMP CONNECT frame, NOT HTTP headers)
    - Expose activate/deactivate/publish methods
    - _Requirements: 5.1, 5.2, 16.3_

  - [ ] 3.2 Implement NetworkSystem singleton
    - Create `src/game/systems/NetworkSystem.ts` as singleton with connect/disconnect/destroy/reset methods
    - Implement `/topic/game/{gameSessionId}` subscription and message routing by type field
    - Implement `/app/game/{gameSessionId}/action` publish for READY, PLAYER_INPUT, RESTART_REQUEST
    - Implement exponential backoff reconnection (initial 1s, max 30s, max 5 attempts)
    - Implement connection status event emission (connecting, connected, disconnected, error)
    - Implement auto-resubscribe on reconnection success
    - Handle unknown message types with console.warn (no throw)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 7.8, 17.5, 17.6_

  - [ ] 3.3 Implement InputSystem
    - Create `src/game/systems/InputSystem.ts` with keyboard mapping (Arrow Left/A → left, Arrow Right/D → right, Space/W/Arrow Up → jump)
    - Implement InputState diff detection (compare with previous state, publish only on change)
    - Implement mobile touch button creation (48x48px minimum, rounded, pastel style)
    - Implement activate/deactivate lifecycle tied to CoopScene
    - Publish PLAYER_INPUT via NetworkSystem only when InputState changes
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ] 3.4 Implement OrientationSystem
    - Create `src/game/systems/OrientationSystem.ts` with portrait/landscape detection
    - Implement full-screen HTML overlay with Korean rotation message for portrait mode
    - Implement overlay removal on landscape return
    - Trigger `game.scale.refresh()` on orientation change
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Checkpoint - Verify core systems compile and structure is correct
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Scene implementations
  - [ ] 5.1 Implement BootScene
    - Create `src/game/scenes/BootScene.ts` with shape-based graphics resource initialization
    - Generate rectangle textures for Player, Switch, Door using Phaser Graphics
    - Display loading indicator during initialization
    - Auto-transition to LobbyScene on completion
    - _Requirements: 10.1, 10.2, 10.3_

  - [ ] 5.2 Implement LobbyScene
    - Create `src/game/scenes/LobbyScene.ts` with STOMP connection status display (Korean: 연결 중, 연결됨, 연결 해제, 에러)
    - Implement READY button (enabled only when connected, disabled after press to prevent duplicate)
    - Display waiting state after READY sent ("다른 플레이어를 기다리는 중...")
    - Handle ROOM_STATE to show player list with ready status
    - Handle GAME_STARTED to transition to CoopScene with CoopSceneData
    - Display auth error if token missing/invalid
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 4.3, 4.4, 17.3_

  - [ ] 5.3 Implement CoopScene with interpolation buffer
    - Create `src/game/scenes/CoopScene.ts` with InterpolationBuffer (100ms delay, max 10 snapshots)
    - Implement snapshot push on STATE_UPDATE (use serverTimeMs, fallback to Date.now())
    - Implement lerp-based interpolation in update loop (renderTime = now - 100ms)
    - Manage Player, Switch, Door game objects based on interpolated state
    - Display current score from STATE_UPDATE
    - Activate InputSystem on scene enter, deactivate on exit
    - Handle GAME_CLEARED → ResultScene transition with ResultSceneData (type: 'cleared')
    - Handle GAME_OVER → ResultScene transition with ResultSceneData (type: 'over')
    - Handle PLAYER_DISCONNECTED with notification display
    - Handle connection loss: show overlay, disable input, resume on reconnect
    - Render fixed map for COOP_SWITCH (default mapId)
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [ ] 5.4 Implement ResultScene
    - Create `src/game/scenes/ResultScene.ts` with score, time, intimacy display
    - Implement restart button (sends RESTART_REQUEST, disables after press, shows "상대방의 재시작을 기다리는 중")
    - Implement exit button (disconnect STOMP, return to LobbyScene)
    - Handle RESTART_REQUESTED event (show opponent requested restart)
    - Handle GAME_STARTED event (transition to CoopScene for restart)
    - Apply success theme (starlight effect) for GAME_CLEARED
    - Apply failure display with reason for GAME_OVER
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7_

- [ ] 6. Game objects implementation
  - [ ] 6.1 Implement Player game object
    - Create `src/game/objects/Player.ts` extending Phaser.GameObjects.Rectangle
    - Implement userId-based deterministic color assignment (hash function)
    - Implement interpolation fields (targetX/Y, previousX/Y, interpolationAlpha)
    - Implement smooth position update via lerp between snapshots
    - _Requirements: 14.1, 14.2, 12.2_

  - [ ] 6.2 Implement Switch game object
    - Create `src/game/objects/Switch.ts` extending Phaser.GameObjects.Rectangle
    - Implement pressed state toggle with glow effect (Phaser.FX.Glow)
    - Show activatedBy visual indicator
    - _Requirements: 14.3, 12.3_

  - [ ] 6.3 Implement Door game object
    - Create `src/game/objects/Door.ts` extending Phaser.GameObjects.Rectangle
    - Implement open/closed visual distinction (color/opacity change)
    - Implement starlight particle effect on door open (ParticleEmitter)
    - _Requirements: 14.4, 14.5, 12.4_

- [ ] 7. Visual styling and UI polish
  - [ ] 7.1 Apply campus night sky theme and pastel palette
    - Apply PALETTE constants across all scenes (navy background, starlight yellow accents, lavender UI)
    - Render campus night sky background with stars in CoopScene
    - Style all buttons as rounded, pastel-colored, touch-friendly (48x48px minimum)
    - Ensure all UI text is in Korean
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [ ] 8. Checkpoint - Full integration verification
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. MVP tests (priority 1)
  - [ ] 9.1 Write unit tests for URL parameter parsing
    - Test token extraction (present, missing, empty)
    - Test gameSessionId extraction (present, missing, empty)
    - Test userId extraction (present, missing - optional)
    - Test combined parameter scenarios
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 9.2 Write property test for PLAYER_INPUT message format (Property 6)
    - **Property 6: PLAYER_INPUT message format correctness**
    - Generate arbitrary InputState with `fc.record({left: fc.boolean(), right: fc.boolean(), jump: fc.boolean()})`
    - Verify published message has exact structure `{ "type": "PLAYER_INPUT", "input": { left, right, jump } }` with no userId field
    - **Validates: Requirements 6.3**

  - [ ]* 9.3 Write property test for InputState diff publishing (Property 2)
    - **Property 2: InputState diff-based publishing**
    - Generate sequence of InputState values with `fc.array(fc.record({left: fc.boolean(), right: fc.boolean(), jump: fc.boolean()}))`
    - Verify publish count equals number of state transitions (consecutive identical states produce no publish)
    - **Validates: Requirements 8.6, 8.7, 9.6**

  - [ ] 9.4 Write unit test for unknown message type handling
    - Test that unknown type messages log console.warn
    - Test that unknown type messages do not throw exceptions
    - Test that known type messages are routed correctly
    - _Requirements: 7.8_

- [ ] 10. Additional property tests (priority 2)
  - [ ]* 10.1 Write property test for interpolation correctness (Property 1)
    - **Property 1: Interpolation correctness (lerp between snapshots)**
    - Generate arbitrary timestamps and positions with `fc.float()`
    - Verify lerp result equals `position_at_t1 + (position_at_t2 - position_at_t1) * ((t - t1) / (t2 - t1))`
    - **Validates: Requirements 12.1, 14.2**

  - [ ]* 10.2 Write property test for exponential backoff (Property 4)
    - **Property 4: Exponential backoff delay calculation**
    - Generate attempt numbers with `fc.integer({min: 0, max: 4})`
    - Verify delay equals `min(1000 * 2^n, 30000)`
    - Verify no reconnection after attempt 5
    - **Validates: Requirements 5.4, 5.6**

  - [ ]* 10.3 Write property test for deterministic color assignment (Property 5)
    - **Property 5: Deterministic userId color assignment**
    - Generate arbitrary userId strings with `fc.string()`
    - Verify same userId always produces same color (determinism)
    - **Validates: Requirements 12.2, 14.1**

  - [ ]* 10.4 Write property test for unknown message graceful handling (Property 7)
    - **Property 7: Unknown message type graceful handling**
    - Generate arbitrary type strings filtered to exclude known types
    - Verify no exception thrown and console.warn called
    - **Validates: Requirements 7.8**

  - [ ]* 10.5 Write property test for READY idempotence (Property 8)
    - **Property 8: READY message idempotence**
    - Generate arbitrary press counts with `fc.integer({min: 1, max: 100})`
    - Verify exactly one READY message published regardless of press count
    - **Validates: Requirements 17.3**

- [ ] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The design specifies TypeScript with Vite, Phaser.js, @stomp/stompjs, vitest, and fast-check
- Server-authoritative: client NEVER determines game outcomes, only renders STATE_UPDATE
- STOMP connectHeaders for JWT (not HTTP headers)
- Record<string, PlayerStateDto> for players (not array)
- 100ms interpolation delay with serverTimeMs (Date.now() fallback)
- InputState diff-based publishing only (no duplicate sends)
- NetworkSystem singleton with disconnect()/destroy()/reset() for future production migration
