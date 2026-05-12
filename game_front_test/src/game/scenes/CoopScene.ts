import Phaser from 'phaser';
import { NetworkSystem } from '../systems/NetworkSystem';
import { InputSystem } from '../systems/InputSystem';
import {
  PALETTE,
  ConnectionStatus,
  GameStatePayload,
  GameClearedMessage,
  GameOverMessage,
  PlayerDisconnectedMessage,
  PlayerStateDto,
  SwitchStateDto,
  Snapshot,
  CoopSceneData,
  ResultSceneData,
} from '../types/gameTypes';

// =============================================================================
// InterpolationBuffer
// =============================================================================

export class InterpolationBuffer {
  private buffer: Snapshot[] = [];
  private readonly INTERPOLATION_DELAY_MS = 100;
  private readonly MAX_BUFFER_SIZE = 10;

  push(snapshot: Snapshot): void {
    this.buffer.push(snapshot);
    if (this.buffer.length > this.MAX_BUFFER_SIZE) {
      this.buffer.shift();
    }
  }

  getInterpolatedState(currentTime: number): { before: Snapshot; after: Snapshot; t: number } | null {
    const renderTime = currentTime - this.INTERPOLATION_DELAY_MS;

    if (this.buffer.length < 2) {
      return this.buffer.length === 1
        ? { before: this.buffer[0], after: this.buffer[0], t: 0 }
        : null;
    }

    // 렌더링 시점을 감싸는 두 스냅샷 찾기
    let before: Snapshot | null = null;
    let after: Snapshot | null = null;

    for (let i = 0; i < this.buffer.length - 1; i++) {
      if (this.buffer[i].timestamp <= renderTime && this.buffer[i + 1].timestamp >= renderTime) {
        before = this.buffer[i];
        after = this.buffer[i + 1];
        break;
      }
    }

    // renderTime이 모든 스냅샷보다 뒤에 있으면 마지막 두 개 사용
    if (!before || !after) {
      if (renderTime >= this.buffer[this.buffer.length - 1].timestamp) {
        before = this.buffer[this.buffer.length - 2];
        after = this.buffer[this.buffer.length - 1];
      } else {
        // renderTime이 모든 스냅샷보다 앞에 있으면 첫 번째 사용
        return { before: this.buffer[0], after: this.buffer[0], t: 0 };
      }
    }

    const duration = after.timestamp - before.timestamp;
    const t = duration > 0 ? Math.min(1, Math.max(0, (renderTime - before.timestamp) / duration)) : 0;

    return { before, after, t };
  }

  clear(): void {
    this.buffer = [];
  }
}

// =============================================================================
// Utility
// =============================================================================

function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

/** userId 기반 결정적 색상 할당 */
function userIdToColor(userId: string): number {
  const colors = [0x87ceeb, 0xffb7c5, 0xb8a9c9, 0xfff4b8, 0x98d8c8, 0xf7dc6f, 0xbb8fce];
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = (hash * 31 + userId.charCodeAt(i)) | 0;
  }
  return colors[Math.abs(hash) % colors.length];
}

// =============================================================================
// CoopScene
// =============================================================================

/**
 * CoopScene - 게임 플레이 씬.
 * STATE_UPDATE를 보간 렌더링하고, 입력을 처리합니다.
 */
export class CoopScene extends Phaser.Scene {
  private interpolationBuffer = new InterpolationBuffer();
  private inputSystem = new InputSystem();

  private players: Map<string, Phaser.GameObjects.Rectangle> = new Map();
  private switches: Map<string, Phaser.GameObjects.Rectangle> = new Map();
  private door: Phaser.GameObjects.Rectangle | null = null;
  private doorParticles: Phaser.GameObjects.Particles.ParticleEmitter | null = null;

  private scoreText!: Phaser.GameObjects.Text;
  private disconnectOverlay: Phaser.GameObjects.Rectangle | null = null;
  private disconnectText: Phaser.GameObjects.Text | null = null;
  private notificationText!: Phaser.GameObjects.Text;

  private sceneData!: CoopSceneData;

  constructor() {
    super({ key: 'CoopScene' });
  }

  init(data: CoopSceneData): void {
    this.sceneData = data;
  }

  create(): void {
    const { width, height } = this.scale;
    this.cameras.main.setBackgroundColor(PALETTE.navy);

    // 배경 별 렌더링
    this.renderStars(width, height);

    // 점수 UI
    this.scoreText = this.add
      .text(width / 2, 30, '점수: 0', {
        fontSize: '20px',
        color: '#FFF4B8',
      })
      .setOrigin(0.5)
      .setDepth(100);

    // 알림 텍스트
    this.notificationText = this.add
      .text(width / 2, 70, '', {
        fontSize: '16px',
        color: '#FFB7C5',
      })
      .setOrigin(0.5)
      .setDepth(100);

    // InputSystem 활성화
    this.inputSystem.activate(this);

    // NetworkSystem 콜백 설정
    this.setupNetworkCallbacks();
  }

  update(_time: number, _delta: number): void {
    this.inputSystem.update();
    this.renderInterpolatedState();
  }

  shutdown(): void {
    this.inputSystem.deactivate();
    this.interpolationBuffer.clear();
    this.players.clear();
    this.switches.clear();
    this.door = null;
    this.doorParticles = null;
    this.disconnectOverlay = null;
    this.disconnectText = null;
  }

  // ---------------------------------------------------------------------------
  // Network Callbacks
  // ---------------------------------------------------------------------------

  private setupNetworkCallbacks(): void {
    const network = NetworkSystem.getInstance();

    network.setCallbacks({
      onStateUpdate: (msg) => this.handleStateUpdate(msg),
      onGameCleared: (msg) => this.handleGameCleared(msg),
      onGameOver: (msg) => this.handleGameOver(msg),
      onPlayerDisconnected: (msg) => this.handlePlayerDisconnected(msg),
      onStatusChange: (status) => this.handleConnectionStatus(status),
    });
  }

  private handleStateUpdate(msg: GameStatePayload): void {
    const snapshot: Snapshot = {
      timestamp: msg.serverTimeMs || Date.now(),
      players: msg.state.players,
      switches: msg.state.switches,
      doorOpen: msg.state.doorOpen,
      remainingTimeMs: msg.state.remainingTimeMs,
      score: msg.state.score,
    };
    this.interpolationBuffer.push(snapshot);
    this.scoreText.setText(`점수: ${msg.state.score}`);
  }

  private handleGameCleared(msg: GameClearedMessage): void {
    this.inputSystem.deactivate();
    const data: ResultSceneData = {
      type: 'cleared',
      score: msg.score,
      timeElapsed: msg.timeElapsed,
      intimacy: msg.intimacy,
    };
    this.scene.start('ResultScene', data);
  }

  private handleGameOver(msg: GameOverMessage): void {
    this.inputSystem.deactivate();
    const data: ResultSceneData = {
      type: 'over',
      score: msg.score,
      timeElapsed: msg.timeElapsed,
      reason: msg.reason,
    };
    this.scene.start('ResultScene', data);
  }

  private handlePlayerDisconnected(msg: PlayerDisconnectedMessage): void {
    this.notificationText.setText(`${msg.userId} 연결 끊김`);
    this.time.delayedCall(3000, () => {
      this.notificationText.setText('');
    });
  }

  private handleConnectionStatus(status: ConnectionStatus): void {
    if (status === 'disconnected' || status === 'error') {
      this.showDisconnectOverlay();
      this.inputSystem.deactivate();
    } else if (status === 'connected') {
      this.hideDisconnectOverlay();
      this.inputSystem.activate(this);
    }
  }

  // ---------------------------------------------------------------------------
  // Interpolation Rendering
  // ---------------------------------------------------------------------------

  private renderInterpolatedState(): void {
    const result = this.interpolationBuffer.getInterpolatedState(Date.now());
    if (!result) return;

    const { before, after, t } = result;

    // Players
    this.renderPlayers(before.players, after.players, t);

    // Switches (use latest state, no lerp needed for boolean)
    this.renderSwitches(after.switches);

    // Door
    this.renderDoor(after.doorOpen);
  }

  private renderPlayers(
    beforePlayers: Record<string, PlayerStateDto>,
    afterPlayers: Record<string, PlayerStateDto>,
    t: number,
  ): void {
    const activeIds = new Set(Object.keys(afterPlayers));

    // 새 플레이어 추가 또는 기존 플레이어 업데이트
    for (const [userId, afterState] of Object.entries(afterPlayers)) {
      const beforeState = beforePlayers[userId] || afterState;
      const x = lerp(beforeState.x, afterState.x, t);
      const y = lerp(beforeState.y, afterState.y, t);

      let playerObj = this.players.get(userId);
      if (!playerObj) {
        const color = userIdToColor(userId);
        playerObj = this.add.rectangle(x, y, 32, 48, color);
        this.players.set(userId, playerObj);
      } else {
        playerObj.setPosition(x, y);
      }
    }

    // 사라진 플레이어 제거
    for (const [userId, obj] of this.players) {
      if (!activeIds.has(userId)) {
        obj.destroy();
        this.players.delete(userId);
      }
    }
  }

  private renderSwitches(switches: Record<string, SwitchStateDto | boolean>): void {
    for (const [id, state] of Object.entries(switches)) {
      const pressed = typeof state === 'boolean' ? state : state.pressed;

      let switchObj = this.switches.get(id);
      if (!switchObj) {
        // 스위치 위치는 서버에서 제공하지 않으므로 고정 배치 (MVP)
        // 실제로는 맵 데이터에서 가져와야 함
        switchObj = this.add.rectangle(0, 0, 40, 16, PALETTE.lavender);
        switchObj.setVisible(false); // 위치 정보 없으면 숨김
        this.switches.set(id, switchObj);
      }

      if (pressed) {
        switchObj.setFillStyle(PALETTE.starlightYellow);
      } else {
        switchObj.setFillStyle(PALETTE.lavender);
      }
    }
  }

  private renderDoor(doorOpen: boolean): void {
    if (!this.door) {
      // 도어 위치도 맵 데이터 기반 (MVP에서는 고정)
      this.door = this.add.rectangle(0, 0, 32, 64, PALETTE.darkGray);
      this.door.setVisible(false);
    }

    if (doorOpen) {
      this.door.setFillStyle(PALETTE.starlightYellow, 0.6);
      this.showDoorParticles();
    } else {
      this.door.setFillStyle(PALETTE.darkGray, 1);
      this.hideDoorParticles();
    }
  }

  // ---------------------------------------------------------------------------
  // Visual Effects
  // ---------------------------------------------------------------------------

  private renderStars(width: number, height: number): void {
    for (let i = 0; i < 60; i++) {
      const x = Phaser.Math.Between(0, width);
      const y = Phaser.Math.Between(0, height);
      const size = Phaser.Math.Between(1, 3);
      const alpha = Phaser.Math.FloatBetween(0.3, 1);
      this.add.circle(x, y, size, PALETTE.starlightYellow, alpha).setDepth(0);
    }
  }

  private showDoorParticles(): void {
    if (this.doorParticles || !this.door) return;

    this.doorParticles = this.add.particles(this.door.x, this.door.y, 'star_particle', {
      speed: { min: 20, max: 60 },
      lifespan: 1000,
      scale: { start: 0.8, end: 0 },
      alpha: { start: 1, end: 0 },
      frequency: 100,
      blendMode: 'ADD',
    });
  }

  private hideDoorParticles(): void {
    if (this.doorParticles) {
      this.doorParticles.destroy();
      this.doorParticles = null;
    }
  }

  private showDisconnectOverlay(): void {
    if (this.disconnectOverlay) return;

    const { width, height } = this.scale;
    this.disconnectOverlay = this.add
      .rectangle(width / 2, height / 2, width, height, 0x000000, 0.7)
      .setDepth(900);

    this.disconnectText = this.add
      .text(width / 2, height / 2, '재연결 시도 중... 잠시만 기다려주세요.', {
        fontSize: '20px',
        color: '#FFF4B8',
      })
      .setOrigin(0.5)
      .setDepth(901);
  }

  private hideDisconnectOverlay(): void {
    this.disconnectOverlay?.destroy();
    this.disconnectText?.destroy();
    this.disconnectOverlay = null;
    this.disconnectText = null;
  }
}
