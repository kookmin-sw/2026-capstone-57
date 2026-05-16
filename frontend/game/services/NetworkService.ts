import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  ConnectionState,
  GameActionMessage,
  GameEventPayload,
  PositionUpdatePayload,
  ServerMessage,
  ServerMessagePayloadMap,
  ServerMessageType,
} from '../types/network';

// --- Exported utility for property testing ---

/**
 * Computes the retry delay for a given attempt number using exponential backoff.
 * delay = min(2^attempt * 1000, 30000) ms
 */
export function computeRetryDelay(attempt: number): number {
  return Math.min(Math.pow(2, attempt) * 1000, 30000);
}

// --- Event listener types ---

type ServerMessageListener<T extends ServerMessageType> = (
  payload: ServerMessagePayloadMap[T]
) => void;

type ListenerEntry = {
  event: ServerMessageType;
  callback: ServerMessageListener<ServerMessageType>;
};

// --- NetworkService ---

export class NetworkService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private listeners: ListenerEntry[] = [];
  private _connectionState: ConnectionState = 'DISCONNECTED';
  private _gameSessionId: string = '';
  private _token: string = '';
  private _wsUrl: string = '';
  private reconnectAttempt: number = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private maxReconnectTimeMs: number = 30000;
  private reconnectStartTime: number = 0;
  private intentionalDisconnect: boolean = false;

  // --- Public state ---

  get connectionState(): ConnectionState {
    return this._connectionState;
  }

  get isConnected(): boolean {
    return this._connectionState === 'CONNECTED';
  }

  // --- Connection management ---

  /**
   * Establish a STOMP-over-SockJS connection to the game WebSocket endpoint.
   */
  connect(wsUrl: string, token: string, gameSessionId: string): Promise<void> {
    this._wsUrl = wsUrl;
    this._token = token;
    this._gameSessionId = gameSessionId;
    this.intentionalDisconnect = false;

    return this.doConnect();
  }

  /**
   * Gracefully disconnect from the server.
   */
  disconnect(): void {
    this.intentionalDisconnect = true;
    this.clearReconnectTimer();

    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.setConnectionState('DISCONNECTED');
  }

  // --- Message sending ---

  /**
   * Send READY message to indicate this player is ready.
   */
  sendReady(): void {
    this.sendAction({ type: 'READY' });
  }

  /**
   * Send position update to the server.
   */
  sendPositionUpdate(position: PositionUpdatePayload): void {
    this.sendAction({ type: 'POSITION_UPDATE', payload: position });
  }

  /**
   * Send a game event (COIN_COLLECTED, SWITCH_PRESSED, SWITCH_RELEASED, CLEAR_REQUEST).
   */
  sendGameEvent(type: 'COIN_COLLECTED' | 'SWITCH_PRESSED' | 'SWITCH_RELEASED' | 'CLEAR_REQUEST', payload?: GameEventPayload): void {
    this.sendAction({ type, payload });
  }

  // --- Event emitter ---

  /**
   * Subscribe to a specific server message type.
   */
  on<T extends ServerMessageType>(
    event: T,
    callback: ServerMessageListener<T>
  ): void {
    this.listeners.push({
      event,
      callback: callback as ServerMessageListener<ServerMessageType>,
    });
  }

  /**
   * Unsubscribe from a specific server message type.
   */
  off<T extends ServerMessageType>(
    event: T,
    callback: ServerMessageListener<T>
  ): void {
    this.listeners = this.listeners.filter(
      (entry) => !(entry.event === event && entry.callback === (callback as ServerMessageListener<ServerMessageType>))
    );
  }

  // --- Private implementation ---

  private doConnect(): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      this.setConnectionState(
        this._connectionState === 'RECONNECTING' ? 'RECONNECTING' : 'CONNECTING'
      );

      this.client = new Client({
        webSocketFactory: () => new SockJS(this._wsUrl),
        connectHeaders: {
          Authorization: `Bearer ${this._token}`,
        },
        // Disable built-in reconnect; we handle it manually
        reconnectDelay: 0,
        onConnect: (_frame: IFrame) => {
          this.reconnectAttempt = 0;
          this.setConnectionState('CONNECTED');
          this.subscribeToTopic();
          resolve();
        },
        onStompError: (frame: IFrame) => {
          // Check for authentication errors
          const message = frame.headers?.['message'] || '';
          if (
            message.includes('401') ||
            message.includes('Unauthorized') ||
            message.includes('Authentication')
          ) {
            this.setConnectionState('ERROR');
            this.emit('AUTH_ERROR' as ServerMessageType, { message } as never);
            reject(new Error('Authentication failed'));
            return;
          }
          this.handleDisconnect();
          reject(new Error(`STOMP error: ${message}`));
        },
        onWebSocketClose: (_event: CloseEvent) => {
          if (!this.intentionalDisconnect && this._connectionState === 'CONNECTED') {
            this.handleDisconnect();
          }
        },
        onWebSocketError: (_event: Event) => {
          if (this._connectionState === 'CONNECTING') {
            reject(new Error('WebSocket connection failed'));
          }
          this.handleDisconnect();
        },
      });

      this.client.activate();
    });
  }

  private subscribeToTopic(): void {
    if (!this.client || !this.client.connected) return;

    this.subscription = this.client.subscribe(
      `/topic/game/${this._gameSessionId}`,
      (message: IMessage) => {
        this.handleMessage(message);
      }
    );
  }

  private handleMessage(message: IMessage): void {
    try {
      const parsed: ServerMessage = JSON.parse(message.body);
      if (parsed && parsed.type) {
        this.emit(parsed.type, parsed as never);
      }
    } catch (e) {
      console.error('[NetworkService] Failed to parse message:', e);
    }
  }

  private emit<T extends ServerMessageType>(
    event: T,
    payload: ServerMessagePayloadMap[T]
  ): void {
    for (const entry of this.listeners) {
      if (entry.event === event) {
        try {
          entry.callback(payload);
        } catch (e) {
          console.error(`[NetworkService] Listener error for ${event}:`, e);
        }
      }
    }
  }

  private sendAction(message: GameActionMessage): void {
    if (!this.client || !this.client.connected) {
      console.warn('[NetworkService] Cannot send message: not connected');
      return;
    }

    this.client.publish({
      destination: `/app/game/${this._gameSessionId}/action`,
      body: JSON.stringify(message),
    });
  }

  private handleDisconnect(): void {
    if (this.intentionalDisconnect) return;

    this.setConnectionState('RECONNECTING');
    this.reconnectStartTime = this.reconnectStartTime || Date.now();
    this.scheduleReconnect();
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer();

    const elapsed = Date.now() - this.reconnectStartTime;
    if (elapsed >= this.maxReconnectTimeMs) {
      this.setConnectionState('ERROR');
      this.reconnectStartTime = 0;
      return;
    }

    const delay = computeRetryDelay(this.reconnectAttempt);
    this.reconnectAttempt++;

    this.reconnectTimer = setTimeout(() => {
      this.attemptReconnect();
    }, delay);
  }

  private async attemptReconnect(): Promise<void> {
    try {
      await this.doConnect();
      this.reconnectStartTime = 0;
    } catch {
      // doConnect failed, handleDisconnect will be called via callbacks
      // If state is still RECONNECTING, schedule next attempt
      if (this._connectionState === 'RECONNECTING') {
        this.scheduleReconnect();
      }
    }
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private setConnectionState(state: ConnectionState): void {
    if (this._connectionState !== state) {
      this._connectionState = state;
    }
  }
}
