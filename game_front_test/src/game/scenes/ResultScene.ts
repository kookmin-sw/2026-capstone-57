import Phaser from 'phaser';
import { NetworkSystem } from '../systems/NetworkSystem';
import {
  PALETTE,
  ResultSceneData,
  GameStartedMessage,
  RestartRequestedMessage,
  CoopSceneData,
} from '../types/gameTypes';

/**
 * ResultScene - 게임 결과 표시, 재시작/나가기 버튼을 제공합니다.
 */
export class ResultScene extends Phaser.Scene {
  private resultData!: ResultSceneData;
  private restartButton!: Phaser.GameObjects.Text;
  private restartSent = false;

  constructor() {
    super({ key: 'ResultScene' });
  }

  init(data: ResultSceneData): void {
    this.resultData = data;
  }

  create(): void {
    const { width, height } = this.scale;
    this.cameras.main.setBackgroundColor(PALETTE.navy);
    this.restartSent = false;

    // 성공/실패 테마
    if (this.resultData.type === 'cleared') {
      this.renderClearedTheme(width, height);
    } else {
      this.renderOverTheme(width, height);
    }

    // 결과 정보
    const centerY = height / 2 - 40;

    this.add
      .text(width / 2, centerY - 60, this.resultData.type === 'cleared' ? '클리어!' : '게임 오버', {
        fontSize: '36px',
        color: this.resultData.type === 'cleared' ? '#FFF4B8' : '#FFB7C5',
      })
      .setOrigin(0.5);

    // 점수
    this.add
      .text(width / 2, centerY, `점수: ${this.resultData.score}`, {
        fontSize: '22px',
        color: '#FFFFFF',
      })
      .setOrigin(0.5);

    // 시간
    const seconds = Math.floor(this.resultData.timeElapsed / 1000);
    this.add
      .text(width / 2, centerY + 35, `시간: ${seconds}초`, {
        fontSize: '18px',
        color: '#B8A9C9',
      })
      .setOrigin(0.5);

    // 친밀도 (클리어 시에만)
    if (this.resultData.type === 'cleared' && this.resultData.intimacy !== undefined) {
      this.add
        .text(width / 2, centerY + 65, `친밀도: +${this.resultData.intimacy}`, {
          fontSize: '18px',
          color: '#FFF4B8',
        })
        .setOrigin(0.5);
    }

    // 실패 사유 (게임 오버 시에만)
    if (this.resultData.type === 'over' && this.resultData.reason) {
      this.add
        .text(width / 2, centerY + 65, `사유: ${this.resultData.reason}`, {
          fontSize: '16px',
          color: '#FFB7C5',
        })
        .setOrigin(0.5);
    }

    // 재시작 버튼
    this.restartButton = this.add
      .text(width / 2 - 80, height / 2 + 100, '재시작', {
        fontSize: '20px',
        color: '#1B2838',
        backgroundColor: '#B8A9C9',
        padding: { x: 24, y: 10 },
      })
      .setOrigin(0.5)
      .setInteractive({ useHandCursor: true })
      .on('pointerdown', this.onRestartPress, this);

    // 나가기 버튼
    this.add
      .text(width / 2 + 80, height / 2 + 100, '나가기', {
        fontSize: '20px',
        color: '#1B2838',
        backgroundColor: '#FFB7C5',
        padding: { x: 24, y: 10 },
      })
      .setOrigin(0.5)
      .setInteractive({ useHandCursor: true })
      .on('pointerdown', this.onExitPress, this);

    // NetworkSystem 콜백
    this.setupNetworkCallbacks();
  }

  // ---------------------------------------------------------------------------
  // Network
  // ---------------------------------------------------------------------------

  private setupNetworkCallbacks(): void {
    const network = NetworkSystem.getInstance();

    network.setCallbacks({
      onGameStarted: (msg: GameStartedMessage) => this.handleGameStarted(msg),
      onRestartRequested: (_msg: RestartRequestedMessage) => this.handleRestartRequested(),
    });
  }

  private handleGameStarted(msg: GameStartedMessage): void {
    const data: CoopSceneData = {
      gameSessionId: msg.gameSessionId,
      players: msg.players,
    };
    this.scene.start('CoopScene', data);
  }

  private handleRestartRequested(): void {
    // 상대방이 재시작을 요청함
    this.add
      .text(this.scale.width / 2, this.scale.height / 2 + 150, '상대방이 재시작을 요청했습니다', {
        fontSize: '16px',
        color: '#FFF4B8',
      })
      .setOrigin(0.5);
  }

  // ---------------------------------------------------------------------------
  // Button Handlers
  // ---------------------------------------------------------------------------

  private onRestartPress(): void {
    if (this.restartSent) return;

    NetworkSystem.getInstance().publishRestartRequest();
    this.restartSent = true;
    this.restartButton.disableInteractive().setAlpha(0.4);

    this.add
      .text(this.scale.width / 2, this.scale.height / 2 + 150, '상대방의 재시작을 기다리는 중...', {
        fontSize: '16px',
        color: '#B8A9C9',
      })
      .setOrigin(0.5);
  }

  private onExitPress(): void {
    NetworkSystem.getInstance().disconnect();
    this.scene.start('LobbyScene');
  }

  // ---------------------------------------------------------------------------
  // Visual Themes
  // ---------------------------------------------------------------------------

  private renderClearedTheme(width: number, height: number): void {
    // 별빛 효과
    for (let i = 0; i < 40; i++) {
      const x = Phaser.Math.Between(0, width);
      const y = Phaser.Math.Between(0, height);
      const size = Phaser.Math.Between(1, 4);
      const alpha = Phaser.Math.FloatBetween(0.4, 1);
      const star = this.add.circle(x, y, size, PALETTE.starlightYellow, alpha);

      // 반짝임 애니메이션
      this.tweens.add({
        targets: star,
        alpha: { from: alpha, to: 0.1 },
        duration: Phaser.Math.Between(800, 2000),
        yoyo: true,
        repeat: -1,
      });
    }
  }

  private renderOverTheme(_width: number, _height: number): void {
    // 실패 시 어두운 분위기 (배경색만으로 충분)
  }
}
