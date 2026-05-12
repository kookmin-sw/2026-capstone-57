import Phaser from 'phaser';
import { NetworkSystem } from '../systems/NetworkSystem';
import {
  PALETTE,
  ConnectionStatus,
  RoomStateMessage,
  GameStartedMessage,
  GameParams,
  CoopSceneData,
} from '../types/gameTypes';

const STATUS_TEXT_MAP: Record<ConnectionStatus, string> = {
  connecting: '연결 중...',
  connected: '연결됨',
  disconnected: '연결 해제',
  error: '에러',
};

/**
 * LobbyScene - STOMP 연결 상태 표시, READY 전송, 대기 UI를 관리합니다.
 */
export class LobbyScene extends Phaser.Scene {
  private statusText!: Phaser.GameObjects.Text;
  private readyButton!: Phaser.GameObjects.Text;
  private waitingText!: Phaser.GameObjects.Text;
  private playerListText!: Phaser.GameObjects.Text;
  private errorText!: Phaser.GameObjects.Text;

  private gameParams!: GameParams;
  private readySent = false;

  constructor() {
    super({ key: 'LobbyScene' });
  }

  init(): void {
    // URL 파라미터는 main.ts에서 registry에 저장됨
    this.gameParams = this.registry.get('gameParams') as GameParams;
  }

  create(): void {
    const { width, height } = this.scale;
    this.cameras.main.setBackgroundColor(PALETTE.navy);
    this.readySent = false;

    // 타이틀
    this.add
      .text(width / 2, 80, '별빛 길 열기', {
        fontSize: '32px',
        color: '#FFF4B8',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      })
      .setOrigin(0.5);

    // 연결 상태 표시
    this.statusText = this.add
      .text(width / 2, 140, '', {
        fontSize: '18px',
        color: '#B8A9C9',
      })
      .setOrigin(0.5);

    // 에러 메시지
    this.errorText = this.add
      .text(width / 2, 180, '', {
        fontSize: '16px',
        color: '#FFB7C5',
      })
      .setOrigin(0.5);

    // 플레이어 목록
    this.playerListText = this.add
      .text(width / 2, height / 2 - 40, '', {
        fontSize: '16px',
        color: '#FFFFFF',
        align: 'center',
      })
      .setOrigin(0.5);

    // READY 버튼
    this.readyButton = this.add
      .text(width / 2, height / 2 + 60, '준비 완료', {
        fontSize: '22px',
        color: '#1B2838',
        backgroundColor: '#B8A9C9',
        padding: { x: 32, y: 12 },
      })
      .setOrigin(0.5)
      .setInteractive({ useHandCursor: true })
      .on('pointerdown', this.onReadyPress, this);

    // 대기 텍스트
    this.waitingText = this.add
      .text(width / 2, height / 2 + 130, '', {
        fontSize: '16px',
        color: '#B8A9C9',
      })
      .setOrigin(0.5);

    // 인증 검증
    if (!this.gameParams?.token) {
      this.showError('인증 토큰이 없습니다. 올바른 링크로 접속해주세요.');
      this.readyButton.disableInteractive().setAlpha(0.4);
      return;
    }

    if (!this.gameParams?.gameSessionId) {
      this.showError('게임 세션 정보가 없습니다. 올바른 링크로 접속해주세요.');
      this.readyButton.disableInteractive().setAlpha(0.4);
      return;
    }

    // 연결 시작
    this.readyButton.disableInteractive().setAlpha(0.4);
    this.setupNetwork();
  }

  shutdown(): void {
    // 씬 전환 시 콜백 정리 불필요 (NetworkSystem은 싱글톤으로 유지)
  }

  // ---------------------------------------------------------------------------
  // Private
  // ---------------------------------------------------------------------------

  private setupNetwork(): void {
    const network = NetworkSystem.getInstance();

    network.setCallbacks({
      onStatusChange: (status) => this.handleStatusChange(status),
      onRoomState: (msg) => this.handleRoomState(msg),
      onGameStarted: (msg) => this.handleGameStarted(msg),
    });

    network.connect({
      wsEndpoint: '/ws/game',
      token: this.gameParams.token,
      gameSessionId: this.gameParams.gameSessionId,
    });
  }

  private handleStatusChange(status: ConnectionStatus): void {
    this.statusText.setText(STATUS_TEXT_MAP[status]);

    if (status === 'connected' && !this.readySent) {
      this.readyButton.setInteractive({ useHandCursor: true }).setAlpha(1);
    } else if (status === 'error') {
      this.readyButton.disableInteractive().setAlpha(0.4);
      this.showError('서버에 연결할 수 없습니다. 페이지를 새로고침해주세요.');
    } else if (status === 'disconnected') {
      this.readyButton.disableInteractive().setAlpha(0.4);
    }
  }

  private handleRoomState(msg: RoomStateMessage): void {
    const lines = msg.players.map(
      (p) => `${p.userId} - ${p.ready ? '준비 완료 ✓' : '대기 중...'}`,
    );
    this.playerListText.setText(lines.join('\n'));
  }

  private handleGameStarted(msg: GameStartedMessage): void {
    const data: CoopSceneData = {
      gameSessionId: msg.gameSessionId,
      players: msg.players,
    };
    this.scene.start('CoopScene', data);
  }

  private onReadyPress(): void {
    if (this.readySent) return;

    NetworkSystem.getInstance().publishReady();
    this.readySent = true;
    this.readyButton.disableInteractive().setAlpha(0.4);
    this.waitingText.setText('다른 플레이어를 기다리는 중...');
  }

  private showError(message: string): void {
    this.errorText.setText(message);
  }
}
