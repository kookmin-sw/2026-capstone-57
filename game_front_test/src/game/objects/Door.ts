import Phaser from 'phaser';
import { PALETTE } from '../types/gameTypes';

/**
 * Door - 열림/닫힘 시각적 구분과 별빛 파티클 효과를 가진 게임 오브젝트.
 */
export class Door extends Phaser.GameObjects.Rectangle {
  public doorId: string;
  public isOpen: boolean;
  private gateEffect: Phaser.GameObjects.Particles.ParticleEmitter | null = null;

  constructor(scene: Phaser.Scene, x: number, y: number, doorId: string) {
    super(scene, x, y, 32, 64, PALETTE.darkGray);

    this.doorId = doorId;
    this.isOpen = false;

    scene.add.existing(this);
  }

  /**
   * 열림/닫힘 상태 업데이트
   */
  setOpen(open: boolean): void {
    if (this.isOpen === open) return;
    this.isOpen = open;

    if (open) {
      this.setFillStyle(PALETTE.starlightYellow, 0.6);
      this.showGateEffect();
    } else {
      this.setFillStyle(PALETTE.darkGray, 1);
      this.hideGateEffect();
    }
  }

  private showGateEffect(): void {
    if (this.gateEffect) return;

    // star_particle 텍스처가 BootScene에서 생성됨
    if (this.scene.textures.exists('star_particle')) {
      this.gateEffect = this.scene.add.particles(this.x, this.y, 'star_particle', {
        speed: { min: 15, max: 50 },
        lifespan: 1200,
        scale: { start: 0.8, end: 0 },
        alpha: { start: 0.9, end: 0 },
        frequency: 120,
        blendMode: 'ADD',
        emitting: true,
      });
    }
  }

  private hideGateEffect(): void {
    if (this.gateEffect) {
      this.gateEffect.destroy();
      this.gateEffect = null;
    }
  }

  destroy(fromScene?: boolean): void {
    this.hideGateEffect();
    super.destroy(fromScene);
  }
}
