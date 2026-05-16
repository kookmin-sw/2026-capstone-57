import Phaser from 'phaser';
import { NetworkService } from '../services/NetworkService';
import {
  ConnectionState,
  GameStartedPayload,
  RoomStatePayload,
} from '../types/network';

/**
 * Lobby Scene
 *
 * Handles WebSocket connection setup, READY signaling, and waiting for
 * the game to start. Displays placeholder text-based UI for connection
 * status and player readiness.
 */
export default class Lobby extends Phaser.Scene {
  private networkService!: NetworkService;
  private sessionId: string | null = null;
  private token: string | null = null;

  // UI elements
  private statusText!: Phaser.GameObjects.Text;
  private waitingText!: Phaser.GameObjects.Text;
  private player1StatusText!: Phaser.GameObjects.Text;
  private player2StatusText!: Phaser.GameObjects.Text;
  private errorText!: Phaser.GameObjects.Text;
  private retryButton!: Phaser.GameObjects.Text;

  // State
  private connecting = false;
  private retryCount = 0;
  private maxRetries = 3;

  constructor() {
    super('Lobby');
  }

  create(): void {
    const params = new URLSearchParams(window.location.search);
    this.sessionId = params.get('sessionId');
    this.token = params.get('token');

    this.createUI();

    if (!this.sessionId) {
      this.showError('세션 연결 실패');
      return;
    }

    this.startConnection();
  }

  // --- UI Creation ---

  private createUI(): void {
    const centerX = 195;

    // Connection status
    this.statusText = this.add.text(centerX, 300, '연결 중...', {
      fontSize: '18px',
      color: '#ffffff',
      align: 'center',
    });
    this.statusText.setOrigin(0.5);

    // Waiting indicator
    this.waitingText = this.add.text(centerX, 350, '', {
      fontSize: '14px',
      color: '#cccccc',
      align: 'center',
    });
    this.waitingText.setOrigin(0.5);

    // Player ready status
    this.player1StatusText = this.add.text(centerX, 420, '', {
      fontSize: '14px',
      color: '#88ccff',
      align: 'center',
    });
    this.player1StatusText.setOrigin(0.5);

    this.player2StatusText = this.add.text(centerX, 450, '', {
      fontSize: '14px',
      color: '#ffaacc',
      align: 'center',
    });
    this.player2StatusText.setOrigin(0.5);

    // Error text (hidden initially)
    this.errorText = this.add.text(centerX, 380, '', {
      fontSize: '16px',
      color: '#ff6666',
      align: 'center',
    });
    this.errorText.setOrigin(0.5);
    this.errorText.setVisible(false);

    // Retry button (hidden initially)
    this.retryButton = this.add.text(centerX, 440, '다시 시도', {
      fontSize: '16px',
      color: '#ffffff',
      backgroundColor: '#444444',
      padding: { x: 16, y: 8 },
      align: 'center',
    });
    this.retryButton.setOrigin(0.5);
    this.retryButton.setVisible(false);
    this.retryButton.setInteractive({ useHandCursor: true });
    this.retryButton.on('pointerdown', () => {
      this.retryCount = 0;
      this.hideError();
      this.startConnection();
    });
  }

  // --- Connection Flow ---

  private async startConnection(): Promise<void> {
    if (this.connecting) return;
    this.connecting = true;

    this.statusText.setText('세션 정보 조회 중...');

    try {
      // Step 1: Fetch session info from REST API
      const sessionInfo = await this.fetchSessionInfo(this.sessionId!);
      if (!sessionInfo) {
        this.handleConnectionError('세션 정보를 가져올 수 없습니다');
        return;
      }

      this.statusText.setText('서버에 연결 중...');

      // Step 2: Connect via WebSocket
      this.networkService = new NetworkService();
      this.registerNetworkListeners();

      const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws/game';
      const authToken = this.token || '';

      await this.networkService.connect(wsUrl, authToken, this.sessionId!);

      // Step 3: Send READY after connection established
      this.networkService.sendReady();

      this.statusText.setText('연결 완료');
      this.waitingText.setText('상대방을 기다리고 있어요...');
      this.connecting = false;
    } catch (error) {
      const message = error instanceof Error ? error.message : '연결에 실패했습니다';
      this.handleConnectionError(message);
    }
  }

  private async fetchSessionInfo(gameSessionId: string): Promise<GameSessionInfo | null> {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    const url = `${apiBaseUrl}/v1/game-sessions/${gameSessionId}`;

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      if (this.token) {
        headers['Authorization'] = `Bearer ${this.token}`;
      }

      const response = await fetch(url, { headers });

      if (!response.ok) {
        console.error(`[Lobby] Session fetch failed: ${response.status}`);
        return null;
      }

      const data = await response.json();
      return {
        id: data.id,
        matchId: data.matchId,
        gameType: data.gameType,
        status: data.status,
      };
    } catch (error) {
      console.error('[Lobby] Failed to fetch session info:', error);
      return null;
    }
  }

  // --- Network Event Handlers ---

  private registerNetworkListeners(): void {
    this.networkService.on('ROOM_STATE', this.onRoomState);
    this.networkService.on('GAME_STARTED', this.onGameStarted);
  }

  private onRoomState = (payload: RoomStatePayload): void => {
    const players = payload.players;
    const entries = Object.entries(players);

    if (entries.length >= 1) {
      const [id1, ready1] = entries[0];
      this.player1StatusText.setText(
        `플레이어 1 (${id1.substring(0, 6)}...): ${ready1 ? '준비 완료 ✓' : '대기 중...'}`
      );
    }

    if (entries.length >= 2) {
      const [id2, ready2] = entries[1];
      this.player2StatusText.setText(
        `플레이어 2 (${id2.substring(0, 6)}...): ${ready2 ? '준비 완료 ✓' : '대기 중...'}`
      );
    }

    // Update waiting text based on ready states
    const allReady = entries.length >= 2 && entries.every(([, ready]) => ready);
    if (allReady) {
      this.waitingText.setText('게임 시작 준비 완료!');
    } else if (entries.length < 2) {
      this.waitingText.setText('상대방을 기다리고 있어요...');
    } else {
      this.waitingText.setText('상대방이 준비 중...');
    }
  };

  private onGameStarted = (payload: GameStartedPayload): void => {
    // Clean up listeners before transitioning
    this.networkService.off('ROOM_STATE', this.onRoomState);
    this.networkService.off('GAME_STARTED', this.onGameStarted);

    // Determine which player we are based on token/userId
    // The token is used to identify the current user; the server assigns roles
    const myUserId = this.extractUserIdFromToken(this.token || '');

    // Transition to Level scene with online mode config
    this.scene.start('Level', {
      onlineMode: true,
      networkService: this.networkService,
      gameSessionId: payload.gameSessionId,
      myUserId,
      playerAssignment: payload.playerAssignment,
      totalCoins: payload.totalCoins,
      timeLimitMs: payload.timeLimitMs,
    });
  };

  // --- Error Handling ---

  private handleConnectionError(message: string): void {
    this.connecting = false;
    this.retryCount++;

    if (this.retryCount >= this.maxRetries) {
      this.showError(`${message}\n재시도 횟수를 초과했습니다.`);
    } else {
      this.showError(message);
    }
  }

  private showError(message: string): void {
    this.statusText.setText('');
    this.waitingText.setText('');
    this.player1StatusText.setText('');
    this.player2StatusText.setText('');

    this.errorText.setText(message);
    this.errorText.setVisible(true);
    this.retryButton.setVisible(true);
  }

  private hideError(): void {
    this.errorText.setVisible(false);
    this.retryButton.setVisible(false);
    this.statusText.setText('연결 중...');
  }

  // --- Utilities ---

  /**
   * Extract userId from JWT token payload.
   * Falls back to empty string if parsing fails.
   */
  private extractUserIdFromToken(token: string): string {
    try {
      if (!token) return '';
      const parts = token.split('.');
      if (parts.length !== 3) return '';
      const payload = JSON.parse(atob(parts[1]));
      return payload.sub || payload.userId || '';
    } catch {
      return '';
    }
  }

  // --- Cleanup ---

  shutdown(): void {
    if (this.networkService) {
      this.networkService.off('ROOM_STATE', this.onRoomState);
      this.networkService.off('GAME_STARTED', this.onGameStarted);
    }
  }
}

// --- Internal types ---

interface GameSessionInfo {
  id: string;
  matchId: string;
  gameType: string;
  status: string;
}
