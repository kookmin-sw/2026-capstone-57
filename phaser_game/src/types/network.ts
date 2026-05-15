// ============================================================
// Network Message Type Definitions for Cooperative Game Networking
// STOMP over SockJS protocol types
// ============================================================

// --- Connection State ---

export type ConnectionState =
  | 'DISCONNECTED'
  | 'CONNECTING'
  | 'CONNECTED'
  | 'RECONNECTING'
  | 'ERROR';

// --- Player Animation ---

export type PlayerAnimation = 'idle' | 'walk' | 'jump';

// --- Client → Server Messages ---

export type ClientMessageType =
  | 'READY'
  | 'POSITION_UPDATE'
  | 'COIN_COLLECTED'
  | 'SWITCH_PRESSED'
  | 'SWITCH_RELEASED'
  | 'CLEAR_REQUEST';

export interface PositionUpdatePayload {
  x: number;           // 0~390
  y: number;           // 0~844
  velocityX: number;
  velocityY: number;
  animation: PlayerAnimation;
  flipX: boolean;
}

export interface GameEventPayload {
  targetId: string;    // coinId or switchId
}

export interface GameActionMessage {
  type: ClientMessageType;
  payload?: PositionUpdatePayload | GameEventPayload;
}

// --- Server → Client Messages ---

export type ServerMessageType =
  | 'ROOM_STATE'
  | 'GAME_STARTED'
  | 'PARTNER_POSITION'
  | 'COIN_CONFIRMED'
  | 'COIN_REJECTED'
  | 'SWITCH_STATE'
  | 'DOOR_OPENED'
  | 'GAME_CLEARED'
  | 'GAME_OVER'
  | 'PLAYER_DISCONNECTED'
  | 'PLAYER_RECONNECTED';

// --- Server Message Payloads ---

export interface RoomStatePayload {
  type: 'ROOM_STATE';
  players: Record<string, boolean>;  // userId → ready status
  playerAssignment?: {
    player1UserId: string;
    player2UserId: string;
  };
}

export interface GameStartedPayload {
  type: 'GAME_STARTED';
  gameSessionId: string;
  playerAssignment: {
    player1UserId: string;
    player2UserId: string;
  };
  totalCoins: number;
  timeLimitMs: number;
}

export interface PartnerPositionPayload {
  type: 'PARTNER_POSITION';
  x: number;
  y: number;
  velocityX: number;
  velocityY: number;
  animation: PlayerAnimation;
  flipX: boolean;
  timestamp: number;
}

export interface CoinEventPayload {
  type: 'COIN_CONFIRMED' | 'COIN_REJECTED';
  coinId: string;
  collectedBy: string;
  totalCollected: number;
}

export interface SwitchStatePayload {
  type: 'SWITCH_STATE';
  switchId: string;
  pressed: boolean;
  pressedBy: string;
}

export interface DoorOpenedPayload {
  type: 'DOOR_OPENED';
}

export interface GameClearedPayload {
  type: 'GAME_CLEARED';
  score: number;
  clearTimeMs: number;
  intimacyPoints: number;
}

export interface GameOverPayload {
  type: 'GAME_OVER';
  reason: 'TIMEOUT' | 'DISCONNECTED';
  score: number;
  elapsedTimeMs: number;
}

export interface PlayerEventPayload {
  type: 'PLAYER_DISCONNECTED' | 'PLAYER_RECONNECTED';
  playerId: string;
}

// --- Server Message Payload Map ---

export interface ServerMessagePayloadMap {
  ROOM_STATE: RoomStatePayload;
  GAME_STARTED: GameStartedPayload;
  PARTNER_POSITION: PartnerPositionPayload;
  COIN_CONFIRMED: CoinEventPayload;
  COIN_REJECTED: CoinEventPayload;
  SWITCH_STATE: SwitchStatePayload;
  DOOR_OPENED: DoorOpenedPayload;
  GAME_CLEARED: GameClearedPayload;
  GAME_OVER: GameOverPayload;
  PLAYER_DISCONNECTED: PlayerEventPayload;
  PLAYER_RECONNECTED: PlayerEventPayload;
}

// --- Utility type for typed server message ---

export type ServerMessage<T extends ServerMessageType = ServerMessageType> = {
  type: T;
} & ServerMessagePayloadMap[T];
