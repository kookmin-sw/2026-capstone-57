# 요구사항 문서

## 소개

"별빛 길 열기" (Starlight Path Opening)는 STOMP 기반 실시간 협동 게임 서버(backend/domain.game)를 위한 Phaser 테스트 클라이언트입니다. 프로젝트명 "일기예보" (Weather Forecast)의 일부로, 단순 테스트 페이지가 아닌 향후 프로덕션 프론트엔드로 마이그레이션 가능한 구조로 설계됩니다.

기술 스택: Vite, TypeScript, Phaser.js, @stomp/stompjs, sockjs-client
게임 타입: COOP_SWITCH (협동 스위치 퍼즐)
비주얼: 캠퍼스 별빛, 구름, 하늘, 따뜻한 파스텔 톤 (70% 캐주얼 협동, 30% 일기예보 미학)

## 씬 전환 흐름도

```
BootScene -> LobbyScene -> CoopScene -> ResultScene
                ^                          |
                |  (나가기/재시작 실패)      |
                +--------------------------+
```

- BootScene: 리소스 초기화 후 자동 전환
- LobbyScene: STOMP 연결 + READY -> GAME_STARTED 수신 시 전환
- CoopScene: GAME_CLEARED 또는 GAME_OVER 수신 시 전환
- ResultScene: 재시작(RESTART_REQUEST) 또는 나가기(LobbyScene 복귀)

## 용어 사전

- **Client**: Phaser.js 기반 브라우저 게임 클라이언트 애플리케이션
- **STOMP_Client**: @stomp/stompjs 라이브러리를 사용하여 WebSocket/SockJS 위에서 STOMP 프로토콜 통신을 수행하는 모듈
- **NetworkSystem**: STOMP 연결 관리, JWT 인증, 메시지 발행/구독, 재연결을 담당하는 시스템
- **InputSystem**: 키보드 및 터치 입력을 처리하고 InputState 변경 시 PLAYER_INPUT을 발행하는 시스템
- **OrientationSystem**: 디바이스 방향을 감지하고 세로 모드 시 회전 오버레이를 표시하는 시스템
- **BootScene**: 리소스 초기화 및 shape 기반 그래픽을 로드하는 Phaser 씬
- **LobbyScene**: STOMP 연결 상태 표시, JWT 확인, READY 전송, 대기 상태를 관리하는 Phaser 씬
- **CoopScene**: 게임 플레이, STATE_UPDATE 렌더링, 입력 처리를 담당하는 Phaser 씬
- **ResultScene**: 결과(점수, 시간, 친밀도) 표시 및 재시작/나가기 버튼을 제공하는 Phaser 씬
- **Player**: 사용자별 색상으로 구분되는 사각형/스프라이트 게임 오브젝트
- **Switch**: 눌림 상태에 따라 발광 효과가 적용되는 게임 오브젝트
- **Door**: 열림/닫힘 시각적 구분과 별빛 게이트 효과를 가진 게임 오브젝트
- **InputState**: 플레이어 입력 상태를 나타내는 객체 (left, right, jump boolean 값)
- **GameSessionId**: URL 쿼리 파라미터로 전달되는 게임 세션 식별자
- **JWT**: URL 쿼리 파라미터(?token=...)로 전달되어 STOMP CONNECT 헤더에 사용되는 인증 토큰
- **STATE_UPDATE**: 서버에서 수신하는 게임 상태 업데이트 이벤트 (플레이어 위치, 스위치, 도어, 점수 포함)
- **ServerTickRate**: 서버가 STATE_UPDATE를 전송하는 주기 (기본 20Hz 가정)
- **Interpolation**: 최근 두 서버 스냅샷 사이를 보간하여 부드러운 렌더링을 제공하는 기법


## 메시지 스키마 정의

### 서버 -> 클라이언트 메시지 스키마

```typescript
interface PlayerState {
  userId: string;
  x: number;
  y: number;
  velocityX: number;
  velocityY: number;
  isGrounded: boolean;
}

interface SwitchState {
  id: string;
  x: number;
  y: number;
  pressed: boolean;
  activatedBy: string | null;
}

interface DoorState {
  id: string;
  x: number;
  y: number;
  open: boolean;
}

interface StateUpdateMessage {
  type: "STATE_UPDATE";
  gameSessionId: string;
  mapId: string;
  players: PlayerState[];
  switches: SwitchState[];
  doors: DoorState[];
  score: number;
  serverTimestamp: number; // epoch milliseconds
}

interface RoomStateMessage {
  type: "ROOM_STATE";
  players: { userId: string; ready: boolean }[];
  gameSessionId: string;
}

interface GameStartedMessage {
  type: "GAME_STARTED";
  gameSessionId: string;
  players: string[];
}

interface GameClearedMessage {
  type: "GAME_CLEARED";
  gameSessionId: string;
  score: number;
  timeElapsed: number;
  intimacy: number;
}

interface GameOverMessage {
  type: "GAME_OVER";
  gameSessionId: string;
  reason: string;
  score: number;
  timeElapsed: number;
}

interface PlayerDisconnectedMessage {
  type: "PLAYER_DISCONNECTED";
  gameSessionId: string;
  userId: string;
}

interface GameErrorMessage {
  type: "GAME_ERROR";
  gameSessionId: string;
  code: string;
  message: string;
}
```

### 클라이언트 -> 서버 메시지 스키마

```typescript
interface ReadyMessage {
  type: "READY";
}

interface PlayerInputMessage {
  type: "PLAYER_INPUT";
  input: {
    left: boolean;
    right: boolean;
    jump: boolean;
  };
}

interface RestartRequestMessage {
  type: "RESTART_REQUEST";
}
```

### 메시지 스키마 공통 규칙

1. 모든 서버 → 클라이언트 메시지는 `type` 필드를 포함한다.
2. 모든 게임 진행 관련 서버 메시지는 `gameSessionId`를 포함한다.
3. `serverTimestamp`는 epoch milliseconds 기준 숫자로 전달한다.
4. Client는 JWT에서 확인되는 사용자 식별을 신뢰하며, URL의 `userId`는 인증/권한 판단에 사용하지 않는다.
5. Client는 알 수 없는 필드가 포함된 메시지를 수신해도 필수 필드가 유효하면 처리한다.

## 요구사항

### 요구사항 1: 프로젝트 초기화 및 빌드

**사용자 스토리:** 개발자로서, Vite + TypeScript + Phaser.js 프로젝트를 `npm install && npm run dev`로 즉시 실행할 수 있기를 원합니다. 이를 통해 빠르게 개발 환경을 구축할 수 있습니다.

#### 인수 조건

1. WHEN `npm install`이 실행되면, THE Client SHALL 모든 의존성을 에러 없이 설치한다
2. WHEN `npm run dev`가 실행되면, THE Client SHALL localhost:5173에서 Vite 개발 서버를 시작한다
3. THE Client SHALL TypeScript strict 모드를 사용하여 타입 안전성을 보장한다
4. THE Client SHALL Phaser.js, @stomp/stompjs, sockjs-client를 의존성으로 포함한다

### 요구사항 2: Phaser 캔버스 설정

**사용자 스토리:** 플레이어로서, 다양한 디바이스에서 적절한 크기의 게임 화면을 볼 수 있기를 원합니다. 이를 통해 PC, 태블릿, 모바일에서 모두 플레이할 수 있습니다.

#### 인수 조건

1. THE Client SHALL 1280x720 기본 해상도로 Phaser 캔버스를 렌더링한다
2. THE Client SHALL Phaser Scale.FIT 모드를 적용하여 캔버스를 뷰포트에 맞춘다
3. THE Client SHALL CENTER_BOTH를 적용하여 캔버스를 수평/수직 중앙에 배치한다
4. WHEN 브라우저 창 크기가 변경되면, THE Client SHALL 종횡비를 유지하며 캔버스 스케일을 재계산한다

### 요구사항 3: 반응형 및 방향 감지

**사용자 스토리:** 모바일 플레이어로서, 세로 모드에서 게임을 실행했을 때 가로로 회전하라는 안내를 받고 싶습니다. 이를 통해 최적의 게임 경험을 할 수 있습니다.

#### 인수 조건

1. THE OrientationSystem SHALL 모바일 디바이스에서 화면 방향을 감지한다
2. WHEN 디바이스가 세로 모드일 때, THE OrientationSystem SHALL 한국어 회전 안내가 포함된 전체 화면 오버레이를 표시한다
3. WHEN 디바이스가 가로 모드로 돌아오면, THE OrientationSystem SHALL 회전 오버레이를 숨긴다
4. WHEN 방향이 변경되면, THE OrientationSystem SHALL Phaser 스케일 새로고침을 트리거한다


### 요구사항 4: JWT 인증 및 URL 파라미터 처리

**사용자 스토리:** 개발자로서, URL 쿼리 파라미터로 JWT 토큰과 게임 세션 ID를 전달하여 인증된 STOMP 연결을 수립하고 싶습니다. 이를 통해 서버와 안전하게 통신할 수 있습니다.

#### 인수 조건

1. WHEN Client가 로드되면, THE Client SHALL URL에서 `token` 쿼리 파라미터를 추출한다
2. WHEN Client가 로드되면, THE Client SHALL URL에서 `gameSessionId` 쿼리 파라미터를 추출한다
3. IF `token` 파라미터가 누락되면, THEN THE Client SHALL 한국어로 인증 실패 에러 메시지를 표시한다
4. IF `gameSessionId` 파라미터가 누락되면, THEN THE Client SHALL 한국어로 세션 누락 에러 메시지를 표시한다
5. THE Client SHALL `userId` 쿼리 파라미터를 UI 표시 및 디버깅 목적으로만 사용한다
6. THE Client SHALL `userId` 쿼리 파라미터를 인증, 소유권 확인, 또는 메시지 페이로드 식별에 사용하지 않는다 (실제 사용자 식별은 서버 측 JWT 기반으로 수행된다)

### 요구사항 5: STOMP 연결 관리

**사용자 스토리:** 플레이어로서, 게임 서버에 안정적으로 연결되어 실시간 게임 데이터를 주고받고 싶습니다. 이를 통해 끊김 없는 협동 게임을 즐길 수 있습니다.

#### 인수 조건

1. WHEN 서버에 연결할 때, THE NetworkSystem SHALL SockJS 폴백을 포함하여 `/ws/game` 엔드포인트에 WebSocket 연결을 수립한다
2. WHEN STOMP로 연결할 때, THE NetworkSystem SHALL STOMP CONNECT 헤더에 `Authorization: Bearer {token}`을 포함한다
3. WHEN STOMP 연결이 수립되면, THE NetworkSystem SHALL `/topic/game/{gameSessionId}`를 구독한다
4. IF STOMP 연결이 실패하면, THEN THE NetworkSystem SHALL 지수 백오프로 재연결을 시도한다 (초기 1초, 최대 30초, 최대 5회)
5. WHEN STOMP 연결 상태가 변경되면, THE NetworkSystem SHALL UI 표시를 위한 상태 이벤트를 발행한다 (connecting, connected, disconnected, error)
6. IF 재연결 최대 횟수를 초과하면, THEN THE NetworkSystem SHALL 한국어로 연결 불가 에러를 표시하고 재연결을 중단한다

### 요구사항 6: STOMP 메시지 발행

**사용자 스토리:** 플레이어로서, 게임 내 행동(준비, 입력, 재시작 요청)을 서버에 전달하고 싶습니다. 이를 통해 서버가 게임 상태를 업데이트할 수 있습니다.

#### 인수 조건

1. WHEN 플레이어가 메시지를 보낼 때, THE NetworkSystem SHALL `/app/game/{gameSessionId}/action`으로 발행한다
2. WHEN 플레이어가 READY를 보낼 때, THE NetworkSystem SHALL 페이로드에 userId 없이 `{ "type": "READY" }`를 발행한다
3. WHEN InputState가 변경되면, THE NetworkSystem SHALL 페이로드에 userId 없이 `{ "type": "PLAYER_INPUT", "input": { "left": bool, "right": bool, "jump": bool } }`를 발행한다
4. WHEN 플레이어가 재시작을 요청하면, THE NetworkSystem SHALL 페이로드에 userId 없이 `{ "type": "RESTART_REQUEST" }`를 발행한다

### 요구사항 7: STOMP 메시지 수신 처리

**사용자 스토리:** 플레이어로서, 서버에서 보내는 게임 이벤트(상태 업데이트, 게임 시작/종료 등)를 수신하여 게임 화면에 반영하고 싶습니다. 이를 통해 실시간으로 게임 진행 상황을 확인할 수 있습니다.

#### 인수 조건

1. WHEN ROOM_STATE 메시지를 수신하면, THE Client SHALL 현재 방 정보(플레이어 목록, 준비 상태)로 로비 화면을 업데이트한다
2. WHEN GAME_STARTED 메시지를 수신하면, THE Client SHALL LobbyScene에서 CoopScene으로 전환한다
3. WHEN STATE_UPDATE 메시지를 수신하면, THE Client SHALL 메시지 스키마에 정의된 모든 게임 오브젝트의 위치와 상태를 업데이트한다
4. WHEN GAME_CLEARED 메시지를 수신하면, THE Client SHALL 점수, 시간, 친밀도 데이터와 함께 ResultScene으로 전환한다
5. WHEN GAME_OVER 메시지를 수신하면, THE Client SHALL 사유, 점수, 시간 데이터와 함께 ResultScene으로 전환한다
6. WHEN PLAYER_DISCONNECTED 메시지를 수신하면, THE Client SHALL 해당 플레이어의 userId와 함께 연결 해제 알림을 표시한다
7. WHEN GAME_ERROR 메시지를 수신하면, THE Client SHALL 에러 코드와 메시지를 한국어로 플레이어에게 표시한다
8. IF 알 수 없는 메시지 타입을 수신하면, THEN THE Client SHALL 콘솔에 경고를 로깅하고 무시한다


### 요구사항 8: 키보드 입력 처리

**사용자 스토리:** PC 플레이어로서, 키보드(화살표/WASD/스페이스)로 캐릭터를 조작하고 싶습니다. 이를 통해 직관적으로 게임을 플레이할 수 있습니다.

#### 인수 조건

1. THE InputSystem SHALL 왼쪽 화살표와 A 키를 `left` 입력에 매핑한다
2. THE InputSystem SHALL 오른쪽 화살표와 D 키를 `right` 입력에 매핑한다
3. THE InputSystem SHALL 스페이스바, W 키, 위쪽 화살표를 `jump` 입력에 매핑한다
4. WHEN 매핑된 키가 눌리면, THE InputSystem SHALL 해당 InputState 필드를 true로 설정한다
5. WHEN 매핑된 키가 해제되면, THE InputSystem SHALL 해당 InputState 필드를 false로 설정한다
6. WHEN InputState가 이전 값에서 변경되면, THE InputSystem SHALL PLAYER_INPUT 발행을 트리거한다
7. THE InputSystem SHALL 동일한 InputState를 연속으로 발행하지 않는다

### 요구사항 9: 모바일 터치 입력 처리

**사용자 스토리:** 모바일 플레이어로서, 화면의 터치 버튼으로 캐릭터를 조작하고 싶습니다. 이를 통해 모바일에서도 편하게 게임을 플레이할 수 있습니다.

#### 인수 조건

1. THE InputSystem SHALL 모바일 디바이스에서 터치 컨트롤 버튼을 표시한다
2. THE InputSystem SHALL 왼쪽, 오른쪽, 점프 터치 버튼을 제공한다
3. THE InputSystem SHALL 터치 버튼을 최소 48x48px 크기의 둥글고 터치 친화적인 요소로 렌더링한다
4. WHEN 터치 버튼이 눌리면, THE InputSystem SHALL 해당 InputState 필드를 true로 설정한다
5. WHEN 터치 버튼이 해제되면, THE InputSystem SHALL 해당 InputState 필드를 false로 설정한다
6. WHEN 터치를 통해 InputState가 이전 값에서 변경되면, THE InputSystem SHALL PLAYER_INPUT 발행을 트리거한다

### 요구사항 10: BootScene 구현

**사용자 스토리:** 플레이어로서, 게임 로딩 시 필요한 리소스가 초기화되기를 원합니다. 이를 통해 이후 씬에서 그래픽 요소를 사용할 수 있습니다.

#### 인수 조건

1. WHEN BootScene이 시작되면, THE BootScene SHALL shape 기반 그래픽 리소스를 초기화한다
2. WHEN 리소스 초기화가 완료되면, THE BootScene SHALL LobbyScene으로 전환한다
3. THE BootScene SHALL 초기화 중 로딩 인디케이터를 표시한다

### 요구사항 11: LobbyScene 구현

**사용자 스토리:** 플레이어로서, 로비에서 STOMP 연결 상태를 확인하고 준비 완료를 알릴 수 있기를 원합니다. 이를 통해 다른 플레이어와 함께 게임을 시작할 수 있습니다.

#### 인수 조건

1. THE LobbyScene SHALL 현재 STOMP 연결 상태를 한국어로 표시한다 (연결 중, 연결됨, 연결 해제, 에러)
2. WHEN STOMP 연결이 수립되면, THE LobbyScene SHALL 준비 버튼을 활성화한다
3. WHEN 준비 버튼이 눌리면, THE LobbyScene SHALL NetworkSystem을 통해 READY 메시지를 전송한다
4. WHEN READY 메시지가 전송되면, THE LobbyScene SHALL 다른 플레이어를 기다리는 대기 상태를 표시한다
5. IF JWT 토큰이 유효하지 않거나 누락되면, THEN THE LobbyScene SHALL 한국어로 인증 에러 메시지를 표시하고 준비 버튼을 비활성화한다
6. WHEN ROOM_STATE를 수신하면, THE LobbyScene SHALL 접속한 플레이어 목록과 각 플레이어의 준비 상태를 표시한다

### 요구사항 12: CoopScene 게임 렌더링

**사용자 스토리:** 플레이어로서, 서버에서 받은 STATE_UPDATE를 기반으로 플레이어, 스위치, 도어, 점수를 실시간으로 볼 수 있기를 원합니다. 이를 통해 협동 게임을 시각적으로 즐길 수 있습니다.

#### 인수 조건

1. WHEN STATE_UPDATE를 수신하면, THE CoopScene SHALL 최근 두 서버 스냅샷 사이를 보간하여 Player 오브젝트를 렌더링한다
2. THE CoopScene SHALL 사용자 식별자에 따라 각 Player를 고유한 색상으로 렌더링한다
3. WHEN STATE_UPDATE를 수신하면, THE CoopScene SHALL 눌림 상태일 때 발광 효과가 적용된 Switch 오브젝트를 렌더링한다
4. WHEN STATE_UPDATE를 수신하면, THE CoopScene SHALL 열림/닫힘 시각적 구분과 별빛 게이트 효과가 적용된 Door 오브젝트를 렌더링한다
5. THE CoopScene SHALL STATE_UPDATE 데이터에서 현재 점수를 표시한다
6. THE CoopScene SHALL 게임 플레이 중 키보드 및 터치 입력을 수용한다
7. THE CoopScene SHALL 클라이언트 렌더링을 60fps로 수행하되, 서버 스냅샷 간 보간으로 부드러운 움직임을 제공한다
8. THE CoopScene SHALL 최신 serverTimestamp보다 100ms 지연된 시점을 기준으로 보간 렌더링을 수행한다
9. THE CoopScene SHALL COOP_SWITCH 기본 mapId를 사용하여 고정 맵을 렌더링한다


### 요구사항 13: ResultScene 구현

**사용자 스토리:** 플레이어로서, 게임 종료 후 결과(점수, 시간, 친밀도)를 확인하고 재시작하거나 나갈 수 있기를 원합니다. 이를 통해 게임 성과를 확인하고 다음 행동을 결정할 수 있습니다.

#### 인수 조건

1. WHEN ResultScene이 시작되면, THE ResultScene SHALL 점수, 시간, 친밀도 값을 포함한 게임 결과를 표시한다
2. THE ResultScene SHALL 재시작 버튼을 표시한다
3. THE ResultScene SHALL 나가기 버튼을 표시한다
4. WHEN 재시작 버튼이 눌리면, THE ResultScene SHALL NetworkSystem을 통해 RESTART_REQUEST 메시지를 전송한다
5. WHEN 나가기 버튼이 눌리면, THE ResultScene SHALL STOMP 연결을 해제하고 LobbyScene으로 돌아간다
6. WHEN GAME_CLEARED로 도달한 경우, THE ResultScene SHALL 성공 테마(별빛 효과)로 결과를 표시한다
7. WHEN GAME_OVER로 도달한 경우, THE ResultScene SHALL 실패 사유와 함께 결과를 표시한다

### 요구사항 14: 게임 오브젝트 시각 표현

**사용자 스토리:** 플레이어로서, 게임 오브젝트(플레이어, 스위치, 도어)를 시각적으로 명확하게 구분할 수 있기를 원합니다. 이를 통해 게임 상황을 직관적으로 파악할 수 있습니다.

#### 인수 조건

1. THE Client SHALL Player 오브젝트를 사용자별 색상 코딩이 적용된 색상 사각형 또는 스프라이트로 렌더링한다
2. THE Client SHALL 최근 두 서버 스냅샷 사이를 보간하여 Player 오브젝트에 부드러운 위치 이동을 적용한다
3. THE Client SHALL 눌림 상태일 때 시각적 발광 효과가 적용된 Switch 오브젝트를 렌더링한다
4. THE Client SHALL 열림과 닫힘이 시각적으로 구분되는 Door 오브젝트를 렌더링한다
5. WHEN Door가 열리면, THE Client SHALL 별빛 게이트 시각 효과를 표시한다

### 요구사항 15: UI/UX 비주얼 스타일

**사용자 스토리:** 플레이어로서, 캠퍼스 별빛/밤하늘 테마의 따뜻한 파스텔 톤 UI를 경험하고 싶습니다. 이를 통해 캐주얼 협동 게임의 분위기를 느낄 수 있습니다.

#### 인수 조건

1. THE Client SHALL 네이비, 하늘색, 연보라, 별빛 노랑 파스텔 톤으로 구성된 색상 팔레트를 사용한다
2. THE Client SHALL CoopScene에서 캠퍼스 밤하늘과 별빛 길 배경을 렌더링한다
3. THE Client SHALL 모든 UI 텍스트를 한국어로 표시한다
4. THE Client SHALL 버튼을 파스텔 색상 스타일의 둥글고 터치 친화적인 요소로 렌더링한다
5. THE Client SHALL 연결, 대기, 에러 상태에 대한 명확한 시각적 인디케이터를 표시한다

### 요구사항 16: 프로젝트 구조 및 코드 품질

**사용자 스토리:** 개발자로서, 향후 프로덕션 프론트엔드로 마이그레이션할 수 있는 구조화된 코드베이스를 원합니다. 이를 통해 테스트 클라이언트에서 프로덕션으로 자연스럽게 전환할 수 있습니다.

#### 인수 조건

1. THE Client SHALL 지정된 디렉토리 구조(game/scenes, game/objects, game/systems, game/types, socket)에 따라 소스 코드를 구성한다
2. THE Client SHALL 모든 STOMP 메시지 타입에 대한 TypeScript 인터페이스를 전용 타입 파일에 정의한다
3. THE Client SHALL 네트워크 로직(stompClient.ts)을 게임 로직(scenes, objects)과 분리한다
4. THE Client SHALL 각 Phaser 씬을 독립적인 클래스 파일로 구현한다
5. THE Client SHALL 각 게임 시스템(NetworkSystem, InputSystem, OrientationSystem)을 독립적인 모듈로 구현한다
6. THE Client SHALL 향후 에셋 확장을 위한 디렉토리 구조를 포함한다 (assets/sprites, assets/audio, assets/shaders)

### 요구사항 17: 에러 처리 및 복구

**사용자 스토리:** 플레이어로서, 네트워크 문제나 서버 에러가 발생해도 적절한 안내를 받고 게임을 계속할 수 있기를 원합니다. 이를 통해 안정적인 게임 경험을 할 수 있습니다.

#### 인수 조건

1. IF JWT 토큰이 만료되면, THEN THE Client SHALL 한국어로 토큰 만료 메시지를 표시하고 재인증을 안내한다
2. IF STOMP 재연결 최대 횟수(5회)를 초과하면, THEN THE Client SHALL 한국어로 연결 불가 메시지를 표시하고 페이지 새로고침을 안내한다
3. IF 중복 READY 메시지를 전송하려 하면, THEN THE Client SHALL 중복 전송을 방지하고 이미 준비 완료 상태임을 표시한다
4. IF 유효하지 않은 gameSessionId로 접속하면, THEN THE Client SHALL 한국어로 세션 없음 에러를 표시한다
5. WHEN 게임 중 STOMP 연결이 끊기면, THE Client SHALL 재연결 시도 중임을 표시하고 게임 입력을 일시 중단한다
6. WHEN 재연결에 성공하면, THE Client SHALL 자동으로 해당 gameSessionId를 재구독하고 게임 입력을 재개한다

### 요구사항 18: 멀티 브라우저 동기화 검증

**사용자 스토리:** 개발자로서, 두 개의 브라우저에서 같은 gameSessionId와 다른 JWT로 접속하여 동기화를 검증하고 싶습니다. 이를 통해 협동 게임의 실시간 동기화가 정상 작동하는지 확인할 수 있습니다.

#### 인수 조건

1. WHEN 두 브라우저가 같은 gameSessionId와 다른 JWT 토큰으로 연결하면, THE Client SHALL CoopScene에서 두 플레이어를 모두 표시한다
2. WHEN 한 플레이어의 위치가 STATE_UPDATE를 통해 변경되면, THE Client SHALL 연결된 두 브라우저 모두에서 변경을 반영한다
3. WHEN 한 플레이어가 Switch를 누르면, THE Client SHALL 연결된 두 브라우저 모두에서 눌림 상태를 표시한다