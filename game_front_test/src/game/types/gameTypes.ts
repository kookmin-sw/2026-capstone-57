// =============================================================================
// game-front-test: 별빛 길 열기 (Starlight Path Opening)
// Type Definitions and Shared Constants
// =============================================================================

// -----------------------------------------------------------------------------
// Server → Client Message Interfaces
// -----------------------------------------------------------------------------

/** 백엔드 STATE_UPDATE payload */
export interface GameStatePayload {
  type: 'STATE_UPDATE';
  gameSessionId: string;
  mapId?: string;
  serverTimeMs: number;
  state: {
    players: Record<string, PlayerStateDto>;
    switches: Record<string, SwitchStateDto | boolean>;
    doorOpen: boolean;
    remainingTimeMs: number;
    score: number;
  };
}

export interface PlayerStateDto {
  x: number;
  y: number;
  vx: number;
  vy: number;
}

export interface SwitchStateDto {
  pressed: boolean;
  activatedBy?: string;
}

export interface RoomStateMessage {
  type: 'ROOM_STATE';
  players: { userId: string; ready: boolean }[];
  gameSessionId: string;
}

export interface GameStartedMessage {
  type: 'GAME_STARTED';
  gameSessionId: string;
  players: string[];
}

export interface GameClearedMessage {
  type: 'GAME_CLEARED';
  gameSessionId: string;
  score: number;
  timeElapsed: number;
  intimacy: number;
}

export interface GameOverMessage {
  type: 'GAME_OVER';
  gameSessionId: string;
  reason: string;
  score: number;
  timeElapsed: number;
}

export interface PlayerDisconnectedMessage {
  type: 'PLAYER_DISCONNECTED';
  gameSessionId: string;
  userId: string;
}

export interface PlayerReconnectedMessage {
  type: 'PLAYER_RECONNECTED';
  gameSessionId: string;
  userId: string;
}

export interface RestartRequestedMessage {
  type: 'RESTART_REQUESTED';
  gameSessionId: string;
  userId: string;
}

export interface GameErrorMessage {
  type: 'GAME_ERROR';
  gameSessionId: string;
  code: string;
  message: string;
}

/** Union type for all server → client messages */
export type ServerMessage =
  | GameStatePayload
  | RoomStateMessage
  | GameStartedMessage
  | GameClearedMessage
  | GameOverMessage
  | PlayerDisconnectedMessage
  | PlayerReconnectedMessage
  | RestartRequestedMessage
  | GameErrorMessage;

// -----------------------------------------------------------------------------
// Client → Server Message Interfaces
// -----------------------------------------------------------------------------

export interface ReadyMessage {
  type: 'READY';
}

export interface PlayerInputMessage {
  type: 'PLAYER_INPUT';
  input: {
    left: boolean;
    right: boolean;
    jump: boolean;
  };
}

export interface RestartRequestMessage {
  type: 'RESTART_REQUEST';
}

/** Union type for all client → server messages */
export type ClientMessage = ReadyMessage | PlayerInputMessage | RestartRequestMessage;

// -----------------------------------------------------------------------------
// System Interfaces
// -----------------------------------------------------------------------------

/** Player input state (keyboard/touch) */
export interface InputState {
  left: boolean;
  right: boolean;
  jump: boolean;
}

/** URL query parameters for game initialization */
export interface GameParams {
  token: string;
  gameSessionId: string;
  userId?: string;
}

/** STOMP connection status */
export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

/** Reconnection state tracking */
export interface ReconnectionState {
  attempt: number;
  maxAttempts: number;
  baseDelay: number;
  maxDelay: number;
  currentDelay: number;
}

/** NetworkSystem configuration */
export interface NetworkSystemConfig {
  wsEndpoint: string;
  token: string;
  gameSessionId: string;
}

// -----------------------------------------------------------------------------
// Interpolation Buffer
// -----------------------------------------------------------------------------

/** A single server state snapshot for interpolation */
export interface Snapshot {
  timestamp: number;
  players: Record<string, PlayerStateDto>;
  switches: Record<string, SwitchStateDto | boolean>;
  doorOpen: boolean;
  remainingTimeMs: number;
  score: number;
}

// -----------------------------------------------------------------------------
// Scene Data Transfer Interfaces
// -----------------------------------------------------------------------------

/** LobbyScene → CoopScene */
export interface CoopSceneData {
  gameSessionId: string;
  players: string[];
}

/** CoopScene → ResultScene */
export interface ResultSceneData {
  type: 'cleared' | 'over';
  score: number;
  timeElapsed: number;
  intimacy?: number;
  reason?: string;
}

// -----------------------------------------------------------------------------
// Color Palette Constants
// -----------------------------------------------------------------------------

export const PALETTE = {
  navy: 0x1b2838,
  skyBlue: 0x87ceeb,
  lavender: 0xb8a9c9,
  starlightYellow: 0xfff4b8,
  pastelPink: 0xffb7c5,
  white: 0xffffff,
  darkGray: 0x2d2d2d,
} as const;

// -----------------------------------------------------------------------------
// Known Message Types (for routing/validation)
// -----------------------------------------------------------------------------

export const KNOWN_SERVER_MESSAGE_TYPES = [
  'STATE_UPDATE',
  'ROOM_STATE',
  'GAME_STARTED',
  'GAME_CLEARED',
  'GAME_OVER',
  'PLAYER_DISCONNECTED',
  'PLAYER_RECONNECTED',
  'RESTART_REQUESTED',
  'GAME_ERROR',
] as const;

export type KnownServerMessageType = (typeof KNOWN_SERVER_MESSAGE_TYPES)[number];
