import Phaser from 'phaser';
import { PALETTE } from '../types/gameTypes';

/**
 * Switch - 눌림 상태에 따라 발광 효과가 적용되는 게임 오브젝트.
 */
export class Switch extends Phaser.GameObjects.Rectangle {
  public switchId: string;
  public pressed: boolean;
  public activatedBy: string | null;
  private glowEffect: Phaser.FX.Glow | null = null;

  constructor(scene: Phaser.Scene, x: number, y: number, switchId: string) {
    super(scene, x, y, 40, 16, PALETTE.lavender);

    this.switchId = switchId;
    this.pressed = false;
    this.activatedBy = null;

    scene.add.existing(this);

    // PostFX pipeline이 지원되면 glow 준비
    if (this.postFX) {
      this.glowEffect = this.postFX.addGlow(PALETTE.starlightYellow, 0, 0, false);
    }
  }

  /**
   * 눌림 상태 업데이트
   */
  setPressed(pressed: boolean, activatedBy?: string | null): void {
    this.pressed = pressed;
    this.activatedBy = activatedBy ?? null;

    if (pressed) {
      this.setFillStyle(PALETTE.starlightYellow);
      if (this.glowEffect) {
        this.glowEffect.outerStrength = 4;
        this.glowEffect.innerStrength = 2;
      }
    } else {
      this.setFillStyle(PALETTE.lavender);
      if (this.glowEffect) {
        this.glowEffect.outerStrength = 0;
        this.glowEffect.innerStrength = 0;
      }
    }
  }
}
