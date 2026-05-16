# Requirements Document: 실시간 협동 게임 도메인 (domain.game)

## Introduction

`일기예보` 대학생 소셜 매칭 서비스의 3단계 상호작용인 실시간 협동 게임 도메인을 정의한다. 매칭된 두 사용자가 PICO PARK 스타일의 2인 협동 퍼즐 플랫포머 게임을 통해 자연스럽게 친밀감을 형성하는 단계이다. MVP 게임 타입은 COOP_SWITCH("별빛 길 열기")이며, 서버 authoritative 방식으로 동작한다.

## Glossary

- **Game_Server**: 게임 로직을 처리하고 상태를 관리하는 백엔드 서버 컴포넌트 (com.ilgiyebo.domain.game)
- **Game_Session**: DB에 영구 저장되는 게임 세션 엔티티. matchId에 종속되며 게임의 생성, 완료, 결과를 기록한다. status enum: WAITING, PLAYING, COMPLETED, FAILED, EXPIRED. TIMEOUT과 DISCONNECTED는 FAILED의 reason 필드로 구분한다
- **Game_Room**: 실시간 런타임 방 객체. 메모리 또는 Redis에서 관리되며 참가자 상태, GameState, inputBuffer 등을 보유한다
- **Game_State**: 게임 진행 중 현재 상태 (플레이어 위치, 스위치 상태, 문 열림 여부, 남은 시간, 점수 등)
- **Game_Loop**: 20~30 tick/sec로 동작하는 서버 측 게임 루프. 단일 GameLoopService가 활성 Game_Room 목록을 순회하며 각 방의 Game_State를 갱신하는 구조이다. 입력 처리, 물리 계산, 충돌 판정, 상태 브로드캐스트를 수행한다
- **Player_Input**: 클라이언트가 서버로 전송하는 사용자 입력. boolean snapshot 방식으로 {"type": "PLAYER_INPUT", "input": {"left": boolean, "right": boolean, "jump": boolean}} 형태를 사용한다
- **State_Update**: 서버가 클라이언트에게 브로드캐스트하는 계산된 게임 상태
- **Match_Entity**: 매칭된 두 사용자(userA, userB)를 나타내는 기존 엔티티. 게임 참여자 검증의 기준이 된다
- **STOMP_Principal**: STOMP CONNECT 시 JWT에서 추출된 userId 기반 인증 정보
- **Game_Front_Test**: Phaser.js 기반 테스트 클라이언트. STOMP 클라이언트(@stomp/stompjs)를 사용하여 /ws/game 엔드포인트에 연결하고, /app/game/{sessionId}/action으로 publish, /topic/game/{sessionId}를 subscribe하여 백엔드 게임 서버를 검증한다. SockJS fallback을 사용한다
- **Intimacy_Points**: 게임 완료 시 계산되어 지급되는 친밀도 점수
- **COOP_SWITCH**: MVP 게임 타입. 두 사용자가 각각 발판을 밟아 길을 열고 함께 도착 지점에 도달하는 협동 퍼즐

## Requirements

### Requirement 1: 게임 세션 생성

**User Story:** As a 매칭된 사용자, I want to 매칭 상대와 게임 세션을 생성하고 싶다, so that 3단계 협동 게임을 시작할 수 있다.

#### Acceptance Criteria

1. WHEN a POST /api/v1/matches/{matchId}/game-sessions 요청이 수신되면, THE Game_Server SHALL matchId에 해당하는 Match_Entity의 존재 여부를 확인하고 Game_Session을 생성하여 반환한다
2. WHEN Game_Session 생성 요청 시 Match_Entity의 status가 ACTIVE가 아니면, THE Game_Server SHALL 요청을 거부하고 에러 응답을 반환한다
3. WHEN Game_Session 생성 요청 시 요청자의 userId가 Match_Entity의 userA 또는 userB가 아니면, THE Game_Server SHALL 참여자 검증 실패 에러를 반환한다
4. WHEN 동일 matchId에 대해 이미 WAITING 또는 PLAYING 상태의 Game_Session이 존재하면, THE Game_Server SHALL 새 세션을 생성하지 않고 기존 세션을 반환한다 (멱등성 보장)
5. WHEN Game_Session이 생성되면, THE Game_Server SHALL 해당 세션의 상태를 WAITING으로 설정하고 DB에 저장한다

### Requirement 2: 게임 방 참가 및 준비

**User Story:** As a 게임 세션에 참여하는 사용자, I want to 게임 방에 입장하고 준비 상태를 표시하고 싶다, so that 상대방과 함께 게임을 시작할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 /topic/game/{sessionId}를 구독하면, THE Game_Server SHALL 해당 사용자의 연결 상태를 추적하고 상대방에게 JOIN 알림을 브로드캐스트한다 (participant 등록은 수행하지 않는다)
2. WHEN 사용자가 STOMP를 통해 READY 메시지를 /app/game/{sessionId}/action으로 전송하면, THE Game_Server SHALL 해당 사용자를 Game_Room의 participant로 등록하고 참여자 검증(Match_Entity의 userA/userB 여부)을 수행한 뒤 ready 상태를 true로 설정한다
3. WHEN Game_Room의 두 참가자 모두 ready 상태가 true가 되면, THE Game_Server SHALL 게임을 시작하고 GAME_STARTED 이벤트를 브로드캐스트한다
4. WHILE 사용자가 STOMP CONNECT를 수행하는 동안, THE Game_Server SHALL JWT 토큰에서 userId를 추출하여 STOMP_Principal로 설정한다 (기존 StompChannelInterceptor 재사용)
5. WHEN STOMP 메시지의 Principal에서 추출한 userId가 Match_Entity의 userA 또는 userB가 아니면, THE Game_Server SHALL 해당 메시지를 거부하고 에러를 반환한다
6. WHEN 사용자가 /topic/game/{sessionId} 구독을 해제하거나 연결이 끊어지면, THE Game_Server SHALL 상대방에게 LEAVE 알림을 전송한다

### Requirement 3: 실시간 게임 입력 처리

**User Story:** As a 게임 중인 사용자, I want to 좌/우 이동과 점프 입력을 보내고 싶다, so that 게임 내 캐릭터를 조작할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 PLAYER_INPUT 메시지를 /app/game/{sessionId}/action으로 전송하면, THE Game_Server SHALL 입력을 검증하고 Game_Room의 inputBuffer에 저장한다
2. THE Game_Server SHALL Player_Input의 userId를 클라이언트 payload에서 신뢰하지 않고 STOMP_Principal에서 추출한 userId를 사용한다
3. WHEN Player_Input의 input 객체가 {"left": boolean, "right": boolean, "jump": boolean} 구조를 만족하지 않으면, THE Game_Server SHALL 해당 입력을 무시한다
4. WHILE Game_Session의 상태가 PLAYING이 아닌 동안, THE Game_Server SHALL Player_Input을 무시한다

### Requirement 4: 서버 측 게임 루프

**User Story:** As a 시스템 운영자, I want to 서버가 게임 상태를 권위적으로 계산하고 싶다, so that 클라이언트 조작을 방지하고 공정한 게임을 보장할 수 있다.

#### Acceptance Criteria

1. WHILE Game_Session의 상태가 PLAYING인 동안, THE Game_Loop SHALL 단일 GameLoopService가 활성 Game_Room 목록을 순회하며 20~30 tick/sec 주기로 각 방의 Game_State를 갱신한다
2. THE Game_Loop SHALL 매 tick마다 inputBuffer의 Player_Input을 기반으로 각 플레이어의 위치를 계산한다
3. THE Game_Loop SHALL 매 tick마다 중력을 적용하고 바닥 충돌을 처리한다
4. THE Game_Loop SHALL 매 tick마다 플레이어와 스위치 영역의 충돌을 체크하고 doorOpen 상태를 판정한다
5. THE Game_Loop SHALL 매 tick마다 remainingTime을 감소시킨다
6. WHEN 두 플레이어가 모두 도착 지점에 도달하면, THE Game_Loop SHALL 게임 클리어를 판정하고 점수를 계산한다
7. THE Game_Server SHALL 계산된 Game_State를 State_Update 이벤트로 /topic/game/{sessionId}에 브로드캐스트한다

### Requirement 5: 게임 클리어 및 점수 계산

**User Story:** As a 게임을 완료한 사용자, I want to 클리어 시간과 협동 성공 여부에 따른 점수를 받고 싶다, so that 성취감을 느끼고 친밀도를 쌓을 수 있다.

#### Acceptance Criteria

1. WHEN 두 플레이어가 모두 도착 지점에 도달하면, THE Game_Server SHALL GAME_CLEARED 이벤트를 브로드캐스트한다
2. WHEN 게임이 클리어되면, THE Game_Server SHALL 클리어 시간, 남은 시간, 협동 성공 횟수를 기준으로 점수를 계산한다
3. WHEN 게임이 클리어되면, THE Game_Server SHALL Game_Session의 상태를 COMPLETED로 변경하고 최종 점수를 DB에 저장한다
4. WHEN 게임이 클리어되면, THE Game_Server SHALL Intimacy_Points를 계산하여 매칭된 두 사용자에게 지급한다
5. WHEN 게임이 클리어되면, THE Game_Server SHALL 게임 완료 결과(점수, 클리어 시간, 성공 여부)를 Interaction 도메인에 전달하거나 연동 가능한 응답 형태로 제공하여 4단계 미션 해금 판단을 위임한다

### Requirement 6: 게임 시간 초과 및 실패 처리

**User Story:** As a 게임 중인 사용자, I want to 제한 시간이 초과되면 게임이 종료되길 원한다, so that 무한히 진행되지 않고 재도전할 수 있다.

#### Acceptance Criteria

1. WHEN remainingTime이 0 이하가 되면, THE Game_Loop SHALL 게임을 종료하고 GAME_OVER 이벤트를 브로드캐스트한다
2. WHEN 게임이 시간 초과로 종료되면, THE Game_Server SHALL Game_Session의 상태를 FAILED로 변경하고 reason을 TIMEOUT으로 기록하여 DB에 저장한다
3. WHEN 게임이 시간 초과로 종료되면, THE Game_Server SHALL 부분 점수를 계산하여 저장한다 (진행도 기반)
4. WHEN 사용자가 RESTART_REQUEST 메시지를 전송하면, THE Game_Server SHALL 두 사용자 모두 동의 시 Game_State를 초기화하고 게임을 재시작한다

### Requirement 7: 연결 끊김 처리

**User Story:** As a 게임 중인 사용자, I want to 상대방의 연결이 끊어졌을 때 알림을 받고 싶다, so that 상황을 인지하고 대기하거나 종료할 수 있다.

#### Acceptance Criteria

1. WHEN 게임 중 한 참가자의 WebSocket 연결이 끊어지면, THE Game_Server SHALL PLAYER_DISCONNECTED 이벤트를 상대방에게 전송한다
2. WHEN 참가자의 연결이 끊어지면, THE Game_Server SHALL Game_Loop를 일시 정지하고 일정 시간 동안 재연결을 대기한다
3. IF 끊어진 참가자가 대기 시간 내에 재연결하지 않으면, THEN THE Game_Server SHALL 게임을 중단하고 Game_Session의 상태를 FAILED로 변경하며 reason을 DISCONNECTED로 기록한다
4. WHEN 끊어진 참가자가 대기 시간 내에 재연결하면, THE Game_Server SHALL Game_Loop를 재개하고 현재 Game_State를 재연결된 클라이언트에 전송한다

### Requirement 8: 데이터 저장 정책

**User Story:** As a 시스템 운영자, I want to 게임 데이터를 효율적으로 저장하고 싶다, so that 성능을 유지하면서 필요한 결과를 보존할 수 있다.

#### Acceptance Criteria

1. THE Game_Server SHALL Player_Input을 매번 DB에 저장하지 않고 메모리 내 inputBuffer에서만 처리한다
2. THE Game_Server SHALL State_Update를 매번 DB에 저장하지 않고 메모리 내 Game_State로만 관리한다
3. WHEN Game_Session이 생성되면, THE Game_Server SHALL 세션 정보를 DB에 저장한다
4. WHEN 게임이 완료 또는 중단되면, THE Game_Server SHALL 최종 결과(점수, 상태, 완료 시간)를 DB에 저장한다
5. WHILE 게임이 진행 중인 동안, THE Game_Server SHALL Game_Room의 런타임 상태를 인메모리로 관리하되 Redis 확장이 가능한 인터페이스로 설계한다

### Requirement 9: 스케줄러 기반 정리 작업

**User Story:** As a 시스템 운영자, I want to 비정상 상태의 게임 세션이 자동으로 정리되길 원한다, so that 서버 리소스가 낭비되지 않는다.

#### Acceptance Criteria

1. THE Game_Server SHALL 주기적으로 제한 시간을 초과한 PLAYING 상태의 Game_Session을 감지하여 자동 종료한다
2. THE Game_Server SHALL 주기적으로 참가자가 모두 연결 해제된 Game_Room을 감지하여 정리한다
3. THE Game_Server SHALL 주기적으로 일정 시간 이상 WAITING 상태로 유지된 Game_Session을 감지하여 EXPIRED로 변경한다

### Requirement 10: STOMP 이벤트 명세

**User Story:** As a 프론트엔드 개발자, I want to 명확한 STOMP 이벤트 명세를 참고하고 싶다, so that Game_Front_Test 클라이언트를 구현할 수 있다.

#### Acceptance Criteria

1. THE Game_Server SHALL 클라이언트→서버 메시지를 /app/game/{sessionId}/action 경로로 수신한다
2. THE Game_Server SHALL 서버→클라이언트 이벤트를 /topic/game/{sessionId} 경로로 브로드캐스트한다
3. THE Game_Server SHALL 클라이언트→서버 메시지 타입으로 READY, PLAYER_INPUT, RESTART_REQUEST를 지원한다
4. THE Game_Server SHALL 서버→클라이언트 이벤트 타입으로 ROOM_STATE, GAME_STARTED, STATE_UPDATE, GAME_CLEARED, GAME_OVER, PLAYER_DISCONNECTED, GAME_ERROR를 지원한다
5. THE Game_Server SHALL 에러 발생 시 /queue/errors 경로로 개별 사용자에게 에러 메시지를 전송한다
6. THE Game_Server SHALL 모든 이벤트 메시지에 type 필드를 포함하여 클라이언트가 메시지 종류를 구분할 수 있도록 한다

### Requirement 11: 게임 세션 조회

**User Story:** As a 매칭된 사용자, I want to 게임 세션의 현재 상태를 조회하고 싶다, so that 게임 진행 상황과 결과를 확인할 수 있다.

#### Acceptance Criteria

1. WHEN GET /api/v1/game-sessions/{gameSessionId} 요청이 수신되면, THE Game_Server SHALL 해당 Game_Session의 정보를 반환한다
2. WHEN 요청자의 userId가 해당 Game_Session의 Match_Entity에 속하지 않으면, THE Game_Server SHALL 참여자 검증 실패 에러를 반환한다
3. WHEN Game_Session의 상태가 COMPLETED이면, THE Game_Server SHALL 최종 점수, 클리어 시간, Intimacy_Points를 포함하여 반환한다

### Requirement 12: 동시 게임 세션 독립성

**User Story:** As a 시스템 운영자, I want to 여러 게임 세션이 동시에 독립적으로 실행되길 원한다, so that 한 게임의 상태가 다른 게임에 영향을 주지 않는다.

#### Acceptance Criteria

1. THE Game_Server SHALL 각 Game_Room을 gameSessionId 기준으로 독립적으로 관리한다
2. THE Game_Server SHALL 단일 GameLoopService가 각 Game_Room의 Game_State를 순회 갱신하되, 한 Game_Room의 상태 갱신이 다른 Game_Room에 영향을 주지 않도록 격리한다
3. THE Game_Server SHALL 동시에 실행 중인 Game_Room 수에 관계없이 각 Game_Room의 tick rate를 유지한다

### Requirement 13: Game_Front_Test 클라이언트 요구사항

**User Story:** As a 프론트엔드 개발자, I want to Phaser.js 기반 테스트 클라이언트를 구현하고 싶다, so that 백엔드 게임 서버를 검증하고 모바일 반응형 게임 화면을 테스트할 수 있다.

#### Acceptance Criteria

1. THE Game_Front_Test SHALL Phaser.js Scale.FIT 모드를 사용하고 내부 논리 해상도를 1280x720으로 설정한다
2. THE Game_Front_Test SHALL 고정 800x600 해상도를 사용하지 않고 PC, 태블릿, 모바일 가로 모드를 지원한다
3. WHEN 모바일 세로 모드로 접속하면, THE Game_Front_Test SHALL "가로로 돌려주세요" 안내 화면을 표시한다
4. THE Game_Front_Test SHALL 키보드 입력(방향키, 스페이스바)과 모바일 터치 버튼 입력을 모두 지원한다
5. THE Game_Front_Test SHALL @stomp/stompjs STOMP 클라이언트를 사용하여 /ws/game 엔드포인트에 SockJS fallback으로 연결하고, /app/game/{sessionId}/action으로 publish, /topic/game/{sessionId}를 subscribe하여 백엔드 Game_Server와 실시간 통신한다
6. THE Game_Front_Test SHALL 서버로부터 수신한 State_Update를 기반으로 두 플레이어의 위치를 화면에 렌더링한다
7. THE Game_Front_Test SHALL Player_Input을 boolean snapshot 방식 {"type": "PLAYER_INPUT", "input": {"left": boolean, "right": boolean, "jump": boolean}}으로 전송한다

### Requirement 14: WebSocket 엔드포인트 구성

**User Story:** As a 백엔드 개발자, I want to 게임 전용 WebSocket 엔드포인트를 구성하고 싶다, so that 채팅과 게임의 WebSocket 트래픽을 분리할 수 있다.

#### Acceptance Criteria

1. THE Game_Server SHALL /ws/game 경로로 게임 전용 STOMP WebSocket 엔드포인트를 제공하되, 기존 WebSocketConfig의 MessageBroker 설정(/topic, /app, /queue/errors prefix)을 재사용한다
2. THE Game_Server SHALL 기존 /ws/chat 엔드포인트와 독립적으로 게임 엔드포인트를 운영한다
3. THE Game_Server SHALL 게임 WebSocket 연결 시 SockJS fallback을 지원한다
4. THE Game_Server SHALL 기존 StompChannelInterceptor의 JWT 인증 패턴을 게임 엔드포인트에도 동일하게 적용한다 (동일 인터셉터 재사용)
5. THE Game_Server SHALL 기존 WebSocketConfig에 /ws/game 엔드포인트를 추가 등록하는 방식으로 구성하며, 별도의 WebSocketMessageBrokerConfigurer를 생성하지 않는다

### Requirement 15: 예외 처리

**User Story:** As a 게임 중인 사용자, I want to 오류 발생 시 명확한 에러 메시지를 받고 싶다, so that 문제 상황을 이해하고 적절히 대응할 수 있다.

#### Acceptance Criteria

1. THE Game_Server SHALL enum 기반 GameException을 정의하고 toException() 메서드를 통해 BusinessException을 생성한다
2. WHEN 존재하지 않는 Game_Session에 접근하면, THE Game_Server SHALL GAME_SESSION_NOT_FOUND 에러를 반환한다
3. WHEN 참여 권한이 없는 사용자가 게임에 접근하면, THE Game_Server SHALL NOT_GAME_PARTICIPANT 에러를 반환한다
4. WHEN 이미 완료된 Game_Session에 대해 게임 액션을 시도하면, THE Game_Server SHALL GAME_SESSION_NOT_ACTIVE 에러를 반환한다
5. WHEN STOMP 메시지 처리 중 예외가 발생하면, THE Game_Server SHALL GAME_ERROR 이벤트를 해당 사용자의 /queue/errors로 전송한다
