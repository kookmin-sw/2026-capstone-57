# Requirements Document

## Introduction

2인 협동 퍼즐 플랫포머 게임 "별빛 길 열기"를 온라인 멀티플레이어로 전환하기 위한 네트워킹 통합 요구사항이다. 현재 로컬 2인 플레이(Arrow keys + WASD)를 각 클라이언트가 하나의 플레이어만 제어하는 온라인 모드로 변경한다.

핵심 아키텍처: 클라이언트 측 물리 유지, 서버는 릴레이 + 이벤트 판정 역할. 각 클라이언트는 자신의 캐릭터에 대해 Arcade Physics를 실행하고, 상대방의 위치/상태는 서버를 통해 수신한다. 서버는 게임 이벤트(코인 수집, 스위치, 문 열림, 클리어)를 검증하고 브로드캐스트한다.

## Glossary

- **Client**: Phaser 3 기반 게임 클라이언트 (390×844 해상도, TypeScript + Vite)
- **Server**: Spring Boot 백엔드 (WebSocket STOMP 엔드포인트 제공)
- **NetworkService**: WebSocket/STOMP 연결 관리 및 메시지 송수신을 담당하는 클라이언트 서비스 모듈
- **RelayService**: 서버에서 클라이언트 간 위치/이벤트를 중계하는 서비스 모듈
- **GameSession**: 하나의 게임 플레이 인스턴스를 나타내는 서버 엔티티
- **Player_Position_Message**: 클라이언트가 서버로 전송하는 자신의 위치/상태 데이터
- **Game_Event_Message**: 게임 내 이벤트(코인 수집, 스위치 등)를 나타내는 메시지
- **Partner**: 같은 세션에 참여한 상대방 플레이어
- **Level_Scene**: 기존 Phaser Level.ts 씬 (게임 로직 전체 포함)
- **STOMP**: Simple Text Oriented Messaging Protocol (WebSocket 위에서 동작)
- **JWT**: JSON Web Token (인증 토큰)

## Requirements

### Requirement 1: WebSocket 연결 및 인증

**User Story:** As a player, I want to connect to the game server with my credentials, so that I can participate in an authenticated online game session.

#### Acceptance Criteria

1. WHEN the game session starts, THE NetworkService SHALL establish a STOMP-over-SockJS connection to the `/ws/game` endpoint
2. WHEN connecting, THE NetworkService SHALL include the JWT token in the STOMP CONNECT frame headers
3. IF the WebSocket connection fails, THEN THE NetworkService SHALL retry with exponential backoff (1s, 2s, 4s, 8s, maximum 30s)
4. IF the JWT token is invalid or expired, THEN THE NetworkService SHALL emit an authentication error event to the Client
5. WHEN the connection is established, THE NetworkService SHALL subscribe to `/topic/game/{gameSessionId}` for server messages
6. THE NetworkService SHALL expose a connection state (DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR) observable by the Client

### Requirement 2: 세션 라이프사이클 관리

**User Story:** As a player, I want to join a game lobby and wait for my partner, so that we can start the game together when both are ready.

#### Acceptance Criteria

1. WHEN the Client enters the game, THE Client SHALL call `GET /api/v1/game-sessions/{gameSessionId}` to retrieve session information
2. WHEN the WebSocket connection is established, THE Client SHALL send a READY message to `/app/game/{sessionId}/action`
3. WHEN a ROOM_STATE message is received showing both players ready, THE Client SHALL transition from waiting state to playing state
4. WHEN a GAME_STARTED message is received, THE Level_Scene SHALL begin the game with both players active
5. WHEN a GAME_CLEARED message is received, THE Client SHALL display the result screen with score and clear time
6. WHEN a GAME_OVER message is received, THE Client SHALL display the failure result screen with the reason

### Requirement 3: 자기 플레이어 위치 전송

**User Story:** As a player, I want my character's position to be sent to the server, so that my partner can see my movements in real-time.

#### Acceptance Criteria

1. WHILE the game is in playing state, THE Client SHALL send Player_Position_Message to the Server at a fixed interval of 50ms (20Hz)
2. THE Player_Position_Message SHALL contain x position, y position, velocity x, velocity y, current animation state, and facing direction
3. WHEN the local player's position has not changed since the last transmission, THE Client SHALL skip sending the Player_Position_Message for that interval
4. THE Client SHALL use the STOMP destination `/app/game/{sessionId}/action` with message type `POSITION_UPDATE` for position transmission
5. THE Client SHALL continue running Arcade Physics locally for the controlled player without server validation of movement

### Requirement 4: 상대방 위치 수신 및 렌더링

**User Story:** As a player, I want to see my partner's character moving smoothly on my screen, so that we can coordinate our puzzle-solving.

#### Acceptance Criteria

1. WHEN a partner position update is received from the Server, THE Client SHALL update the partner sprite's position to the received coordinates
2. THE Client SHALL apply linear interpolation (lerp) to the partner sprite's position to smooth movement between received updates
3. WHEN a partner position update includes animation state, THE Client SHALL play the corresponding animation on the partner sprite
4. WHEN a partner position update includes facing direction, THE Client SHALL set the partner sprite's flip state accordingly
5. THE Client SHALL render the partner sprite without local physics simulation (position-only update, no collider on partner)
6. IF no partner position update is received for more than 2 seconds, THEN THE Client SHALL display a visual indicator on the partner sprite (semi-transparent effect)

### Requirement 5: 게임 이벤트 동기화 - 코인 수집

**User Story:** As a player, I want coin collection to be synchronized between both players, so that we share a consistent score.

#### Acceptance Criteria

1. WHEN the local player overlaps a coin, THE Client SHALL send a Game_Event_Message with type `COIN_COLLECTED` and the coin identifier to the Server
2. WHEN the Server receives a COIN_COLLECTED event, THE RelayService SHALL validate that the coin has not already been collected
3. WHEN the Server confirms a coin collection, THE RelayService SHALL broadcast a `COIN_CONFIRMED` event to both clients with the coin identifier
4. WHEN a COIN_CONFIRMED event is received, THE Client SHALL remove the coin sprite and increment the shared score display
5. THE Client SHALL immediately hide the coin on local overlap (optimistic update) and restore it only if the Server rejects the collection
6. IF the Server rejects a coin collection (already collected by partner), THEN THE Client SHALL not increment the score for that coin

### Requirement 6: 게임 이벤트 동기화 - 스위치 및 플랫폼

**User Story:** As a player, I want switch states to be synchronized, so that platforms move correctly for both players.

#### Acceptance Criteria

1. WHEN the local player overlaps a switch, THE Client SHALL send a Game_Event_Message with type `SWITCH_PRESSED` and the switch identifier to the Server
2. WHEN the local player leaves a switch, THE Client SHALL send a Game_Event_Message with type `SWITCH_RELEASED` and the switch identifier to the Server
3. WHEN the Server receives a switch event, THE RelayService SHALL broadcast the switch state change to both clients
4. WHEN a switch state update is received from the Server, THE Client SHALL update the switch visual and move the associated platform accordingly
5. THE Client SHALL apply local switch logic for the controlled player immediately (optimistic) and reconcile with server state on broadcast receipt

### Requirement 7: 게임 이벤트 동기화 - 문 열림 및 클리어

**User Story:** As a player, I want the door opening and game clear conditions to be validated by the server, so that the game result is authoritative.

#### Acceptance Criteria

1. WHEN all coins are collected (confirmed by Server), THE Server SHALL broadcast a `DOOR_OPENED` event to both clients
2. WHEN a DOOR_OPENED event is received, THE Client SHALL change the door sprite to the open state
3. WHEN both players are at the door position, THE Client SHALL send a `CLEAR_REQUEST` event to the Server
4. WHEN the Server receives a CLEAR_REQUEST and validates both players are near the door, THE Server SHALL broadcast a GAME_CLEARED event
5. THE Client SHALL NOT determine game clear locally; only the Server's GAME_CLEARED event triggers the clear sequence

### Requirement 8: 연결 끊김 및 재연결 처리

**User Story:** As a player, I want the game to handle disconnections gracefully, so that a temporary network issue does not lose our progress.

#### Acceptance Criteria

1. WHEN the WebSocket connection is lost during gameplay, THE NetworkService SHALL attempt automatic reconnection with exponential backoff
2. WHEN reconnection is in progress, THE Client SHALL display a "재연결 중..." overlay and freeze the game state
3. WHEN reconnection succeeds, THE Client SHALL send a READY message to resume and request current game state from the Server
4. WHEN a PLAYER_DISCONNECTED event is received (partner disconnected), THE Client SHALL display "상대방 연결 끊김" notification and apply semi-transparent effect to the partner sprite
5. WHEN a PLAYER_RECONNECTED event is received, THE Client SHALL remove the disconnection notification and restore normal partner rendering
6. IF reconnection fails after 30 seconds, THEN THE Client SHALL display a "연결 실패" message and provide a button to return to the lobby

### Requirement 9: API URL 환경변수 관리

**User Story:** As a developer, I want API URLs to be configurable via environment variables, so that the game client can be deployed to different environments without code changes.

#### Acceptance Criteria

1. THE Client SHALL read the WebSocket endpoint URL from the environment variable `VITE_WS_URL`
2. THE Client SHALL read the REST API base URL from the environment variable `VITE_API_BASE_URL`
3. THE Client SHALL read the game session ID from the URL query parameter `sessionId`
4. THE Client SHALL read the JWT token from the URL query parameter `token` or from a shared storage mechanism
5. IF a required environment variable is not set, THEN THE Client SHALL use a default value suitable for local development (e.g., `http://localhost:8080`)

### Requirement 10: Level.ts 최소 변경 원칙

**User Story:** As a developer, I want networking code to be separated from the existing Level.ts, so that Phaser Editor compatibility is maintained and the codebase remains modular.

#### Acceptance Criteria

1. THE Client SHALL implement all networking logic in separate service files (not inside Level.ts editorCreate or compiled code sections)
2. THE Client SHALL add networking integration to Level.ts only within `/* START-USER-CODE */` blocks
3. THE Client SHALL not modify any code between `/* START OF COMPILED CODE */` and `/* END OF COMPILED CODE */` markers except within designated user code blocks
4. THE Client SHALL implement a NetworkGameManager class that Level.ts instantiates to handle all online multiplayer coordination
5. WHEN running in online mode, THE Level_Scene SHALL disable local input for the partner player (no WASD or second set of controls)
6. WHEN running in offline/local mode (no sessionId parameter), THE Level_Scene SHALL retain the existing local 2-player behavior unchanged

### Requirement 11: 서버 릴레이 모드 전환

**User Story:** As a developer, I want the server to operate in relay mode for this game, so that client-side physics is preserved and server complexity is reduced.

#### Acceptance Criteria

1. WHEN a POSITION_UPDATE message is received, THE RelayService SHALL forward the position data to the other player in the session without modification
2. THE Server SHALL NOT run PhysicsEngine or CollisionEngine for position validation in relay mode
3. THE Server SHALL validate game events (COIN_COLLECTED, SWITCH_PRESSED, SWITCH_RELEASED, CLEAR_REQUEST) against the current game state
4. THE Server SHALL maintain authoritative state for: collected coins set, switch states, door open status, and game clear condition
5. WHEN a game event is validated, THE Server SHALL update the authoritative game state and broadcast the confirmed event to both clients
6. THE Server SHALL continue managing session lifecycle (WAITING, PLAYING, PAUSED, FINISHED) and time limits
7. IF a POSITION_UPDATE message is received while the session is not in PLAYING state, THEN THE Server SHALL discard the message silently

### Requirement 12: 게임 세션 파라미터 전달

**User Story:** As a player, I want to enter the game with the correct session context, so that I am connected to the right partner and game room.

#### Acceptance Criteria

1. WHEN the game page loads, THE Client SHALL extract `gameSessionId` and `token` from URL query parameters
2. WHEN the Client has a valid gameSessionId and token, THE Client SHALL skip the tutorial overlay and proceed to connection
3. IF gameSessionId or token is missing from URL parameters, THEN THE Client SHALL operate in local offline mode (existing behavior)
4. THE Client SHALL determine which player role (player1 or player2) it controls based on the session information received from the Server (ROOM_STATE or GAME_STARTED message containing player assignment)
5. WHEN assigned as player1, THE Client SHALL use arrow keys and mobile buttons for input; WHEN assigned as player2, THE Client SHALL use arrow keys and mobile buttons for input (both use same controls since each device controls one character)
