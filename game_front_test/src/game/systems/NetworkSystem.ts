import { createStompClient, Client, IFrame, IMessage, StompSubscription } from '../../socket/stompClient';
import {
  NetworkSystemConfig,
  ConnectionStatus,
  ReconnectionState,
  GameStatePayload,
  RoomStateMessage,
  GameStartedMessage,
  GameClearedMessage,
  GameOverMessage,
  PlayerDisconnectedMessage,
  PlayerReconnectedMessage,
  RestartRequestedMessage,
  GameErrorMessage,
  InputState,
  KNOWN_SERVER_MESSAGE_TYPES,
  KnownServerMessageType,
} from '../types/gameTypes';

type MessageHandler<T> = (msg: T) => void;

interface NetworkSystemCallbacks {
  onStatusChange?: MessageHandler<ConnectionStatus>;
  onRoomState?: MessageHandler<RoomStateMessage>;
  onGameStarted?: MessageHandler<GameStartedMessage>;
  onStateUpdate?: MessageHandler<GameStatePayload>;
  onGameCleared?: MessageHandler<GameClearedMessage>;
  onGameOver?: MessageHandler<GameOverMessage>;
  onPlayerDisconnected?: MessageHandler<PlayerDisconnectedMessage>;
  onPlayerReconnected?: MessageHandler<PlayerReconnectedMessage>;
  onRestartRequested?: MessageHandler<RestartRequestedMessage>;
  onGameError?: MessageHandler<GameErrorMessage>;
}

/**
 * NetworkSystem - STOMP 연결 생명주기를 관리하는 싱글톤.
 *
 * 책임:
 * - SockJS/STOMP 연결 수립 및 JWT 인증
 * - /topic/game/{gameSessionId} 구독 및 메시지 type 기반 라우팅
 * - /app/game/{gameSessionId}/action 발행 (READY, PLAYER_INPUT, RESTART_REQUEST)
 * - 지수 백오프 재연결 (초기 1s, 최대 30s, 최대 5회)
 * - 연결 상태 이벤트 발행
 * - 재연결 성공 시 자동 재구독
 */
export class NetworkSystem {
  private static instance: NetworkSystem | null = null;

  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private config: NetworkSystemConfig | null = null;
  private callbacks: NetworkSystemCallbacks = {};

  private status: ConnectionStatus = 'disconnected';
  private reconnectionState: ReconnectionState = {
    attempt: 0,
    maxAttempts: 5,
    baseDelay: 1000,
    maxDelay: 30000,
    currentDelay: 1000,
  };
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readySent = false;

  private constructor() {}

  static getInstance(): NetworkSystem {
    if (!NetworkSystem.instance) {
      NetworkSystem.instance = new NetworkSystem();
    }
    return NetworkSystem.instance;
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  setCallbacks(callbacks: NetworkSystemCallbacks): void {
    this.callbacks = { ...this.callbacks, ...callbacks };
  }

  connect(config: NetworkSystemConfig): void {
    this.config = config;
    this.readySent = false;
    this.setStatus('connecting');

    this.client = createStompClient({
      wsEndpoint: config.wsEndpoint,
      token: config.token,
      onConnect: this.handleConnect.bind(this),
      onDisconnect: this.handleDisconnect.bind(this),
      onStompError: this.handleStompError.bind(this),
      onWebSocketClose: this.handleWebSocketClose.bind(this),
      onWebSocketError: this.handleWebSocketError.bind(this),
    });

    this.client.activate();
  }

  disconnect(): void {
    this.clearReconnectTimer();
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
    if (this.client?.active) {
      this.client.deactivate();
    }
    this.setStatus('disconnected');
  }

  destroy(): void {
    this.disconnect();
    this.callbacks = {};
    this.config = null;
    this.client = null;
    NetworkSystem.instance = null;
  }

  reset(): void {
    this.disconnect();
    this.reconnectionState.attempt = 0;
    this.reconnectionState.currentDelay = this.reconnectionState.baseDelay;
    this.readySent = false;
  }

  // ---------------------------------------------------------------------------
  // Message Publishing
  // ---------------------------------------------------------------------------

  publishReady(): void {
    if (this.readySent) return;
    this.publish({ type: 'READY' });
    this.readySent = true;
  }

  publishInput(input: InputState): void {
    this.publish({ type: 'PLAYER_INPUT', input });
  }

  publishRestartRequest(): void {
    this.publish({ type: 'RESTART_REQUEST' });
  }

  getStatus(): ConnectionStatus {
    return this.status;
  }

  // ---------------------------------------------------------------------------
  // Private - Connection Handlers
  // ---------------------------------------------------------------------------

  private handleConnect(_frame: IFrame): void {
    this.reconnectionState.attempt = 0;
    this.reconnectionState.currentDelay = this.reconnectionState.baseDelay;
    this.setStatus('connected');
    this.subscribe();
  }

  private handleDisconnect(_frame: IFrame): void {
    this.subscription = null;
    this.setStatus('disconnected');
  }

  private handleStompError(_frame: IFrame): void {
    this.setStatus('error');
    this.attemptReconnect();
  }

  private handleWebSocketClose(_event: CloseEvent): void {
    this.subscription = null;
    if (this.status === 'connected' || this.status === 'connecting') {
      this.setStatus('disconnected');
      this.attemptReconnect();
    }
  }

  private handleWebSocketError(_event: Event): void {
    this.setStatus('error');
  }

  // ---------------------------------------------------------------------------
  // Private - Subscription & Routing
  // ---------------------------------------------------------------------------

  private subscribe(): void {
    if (!this.client || !this.config) return;

    const destination = `/topic/game/${this.config.gameSessionId}`;
    this.subscription = this.client.subscribe(destination, (message: IMessage) => {
      this.routeMessage(message);
    });
  }

  private routeMessage(message: IMessage): void {
    try {
      const body = JSON.parse(message.body);
      const type: string | undefined = body?.type;

      if (!type) {
        console.warn('[NetworkSystem] Received message without type field:', body);
        return;
      }

      if (!KNOWN_SERVER_MESSAGE_TYPES.includes(type as KnownServerMessageType)) {
        console.warn(`[NetworkSystem] Unknown message type: "${type}"`, body);
        return;
      }

      switch (type) {
        case 'STATE_UPDATE':
          this.callbacks.onStateUpdate?.(body as GameStatePayload);
          break;
        case 'ROOM_STATE':
          this.callbacks.onRoomState?.(body as RoomStateMessage);
          break;
        case 'GAME_STARTED':
          this.callbacks.onGameStarted?.(body as GameStartedMessage);
          break;
        case 'GAME_CLEARED':
          this.callbacks.onGameCleared?.(body as GameClearedMessage);
          break;
        case 'GAME_OVER':
          this.callbacks.onGameOver?.(body as GameOverMessage);
          break;
        case 'PLAYER_DISCONNECTED':
          this.callbacks.onPlayerDisconnected?.(body as PlayerDisconnectedMessage);
          break;
        case 'PLAYER_RECONNECTED':
          this.callbacks.onPlayerReconnected?.(body as PlayerReconnectedMessage);
          break;
        case 'RESTART_REQUESTED':
          this.callbacks.onRestartRequested?.(body as RestartRequestedMessage);
          break;
        case 'GAME_ERROR':
          this.callbacks.onGameError?.(body as GameErrorMessage);
          break;
      }
    } catch (e) {
      console.warn('[NetworkSystem] Failed to parse message:', message.body, e);
    }
  }

  // ---------------------------------------------------------------------------
  // Private - Publishing
  // ---------------------------------------------------------------------------

  private publish(body: object): void {
    if (!this.client?.active || !this.config) return;

    const destination = `/app/game/${this.config.gameSessionId}/action`;
    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  // ---------------------------------------------------------------------------
  // Private - Reconnection (Exponential Backoff)
  // ---------------------------------------------------------------------------

  private attemptReconnect(): void {
    if (this.reconnectionState.attempt >= this.reconnectionState.maxAttempts) {
      this.setStatus('error');
      return;
    }

    this.clearReconnectTimer();

    const delay = this.calculateDelay(this.reconnectionState.attempt);
    this.reconnectionState.currentDelay = delay;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectionState.attempt++;
      this.setStatus('connecting');

      if (this.client) {
        this.client.activate();
      }
    }, delay);
  }

  /** min(baseDelay * 2^attempt, maxDelay) */
  calculateDelay(attempt: number): number {
    const delay = this.reconnectionState.baseDelay * Math.pow(2, attempt);
    return Math.min(delay, this.reconnectionState.maxDelay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  // ---------------------------------------------------------------------------
  // Private - Status
  // ---------------------------------------------------------------------------

  private setStatus(status: ConnectionStatus): void {
    if (this.status === status) return;
    this.status = status;
    this.callbacks.onStatusChange?.(status);
  }
}
