import Phaser from 'phaser';

/**
 * userId 기반 결정적 색상 할당.
 * 같은 userId는 항상 같은 색상을 반환합니다.
 */
export function userIdToColor(userId: string): number {
  const colors = [0x87ceeb, 0xffb7c5, 0xb8a9c9, 0xfff4b8, 0x98d8c8, 0xf7dc6f, 0xbb8fce];
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = (hash * 31 + userId.charCodeAt(i)) | 0;
  }
  return colors[Math.abs(hash) % colors.length];
}

/**
 * Player - 사용자별 색상으로 구분되는 게임 오브젝트.
 * 서버 스냅샷 간 선형 보간으로 부드러운 위치 이동을 제공합니다.
 */
export class Player extends Phaser.GameObjects.Rectangle {
  public userId: string;
  public targetX: number;
  public targetY: number;
  public previousX: number;
  public previousY: number;
  public interpolationAlpha: number;
  public playerColor: number;

  constructor(scene: Phaser.Scene, x: number, y: number, userId: string) {
    const color = userIdToColor(userId);
    super(scene, x, y, 32, 48, color);

    this.userId = userId;
    this.playerColor = color;
    this.targetX = x;
    this.targetY = y;
    this.previousX = x;
    this.previousY = y;
    this.interpolationAlpha = 0;

    scene.add.existing(this);
  }

  /**
   * 새 서버 스냅샷 수신 시 보간 대상 업데이트
   */
  setTarget(x: number, y: number): void {
    this.previousX = this.x;
    this.previousY = this.y;
    this.targetX = x;
    this.targetY = y;
    this.interpolationAlpha = 0;
  }

  /**
   * lerp 기반 위치 보간 (매 프레임 호출)
   */
  interpolate(t: number): void {
    this.x = this.previousX + (this.targetX - this.previousX) * t;
    this.y = this.previousY + (this.targetY - this.previousY) * t;
  }

  /**
   * 즉시 위치 설정 (보간 없이)
   */
  setPositionImmediate(x: number, y: number): void {
    this.setPosition(x, y);
    this.previousX = x;
    this.previousY = y;
    this.targetX = x;
    this.targetY = y;
  }
}
