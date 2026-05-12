import Phaser from 'phaser';
import { PALETTE } from '../types/gameTypes';

/**
 * BootScene - 리소스 초기화 및 shape 기반 그래픽 텍스처를 생성합니다.
 * 완료 후 LobbyScene으로 자동 전환합니다.
 */
export class BootScene extends Phaser.Scene {
  constructor() {
    super({ key: 'BootScene' });
  }

  preload(): void {
    // shape 기반이므로 외부 에셋 로드 없음
  }

  create(): void {
    this.showLoadingIndicator();
    this.generateTextures();
    this.scene.start('LobbyScene');
  }

  private showLoadingIndicator(): void {
    const { width, height } = this.scale;

    this.cameras.main.setBackgroundColor(PALETTE.navy);

    this.add
      .text(width / 2, height / 2, '로딩 중...', {
        fontSize: '24px',
        color: '#FFF4B8',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      })
      .setOrigin(0.5);
  }

  private generateTextures(): void {
    // Player 텍스처 (32x48 사각형)
    const playerGfx = this.make.graphics({ x: 0, y: 0 });
    playerGfx.fillStyle(PALETTE.skyBlue, 1);
    playerGfx.fillRoundedRect(0, 0, 32, 48, 4);
    playerGfx.generateTexture('player_default', 32, 48);
    playerGfx.destroy();

    // Switch 텍스처 (40x16 사각형)
    const switchGfx = this.make.graphics({ x: 0, y: 0 });
    switchGfx.fillStyle(PALETTE.lavender, 1);
    switchGfx.fillRoundedRect(0, 0, 40, 16, 3);
    switchGfx.generateTexture('switch_default', 40, 16);
    switchGfx.destroy();

    // Switch pressed 텍스처
    const switchPressedGfx = this.make.graphics({ x: 0, y: 0 });
    switchPressedGfx.fillStyle(PALETTE.starlightYellow, 1);
    switchPressedGfx.fillRoundedRect(0, 0, 40, 12, 3);
    switchPressedGfx.generateTexture('switch_pressed', 40, 12);
    switchPressedGfx.destroy();

    // Door 텍스처 (32x64 사각형)
    const doorGfx = this.make.graphics({ x: 0, y: 0 });
    doorGfx.fillStyle(PALETTE.darkGray, 1);
    doorGfx.fillRoundedRect(0, 0, 32, 64, 4);
    doorGfx.generateTexture('door_closed', 32, 64);
    doorGfx.destroy();

    // Door open 텍스처
    const doorOpenGfx = this.make.graphics({ x: 0, y: 0 });
    doorOpenGfx.fillStyle(PALETTE.starlightYellow, 0.6);
    doorOpenGfx.fillRoundedRect(0, 0, 32, 64, 4);
    doorOpenGfx.generateTexture('door_open', 32, 64);
    doorOpenGfx.destroy();

    // 별빛 파티클 텍스처 (8x8 원)
    const particleGfx = this.make.graphics({ x: 0, y: 0 });
    particleGfx.fillStyle(PALETTE.starlightYellow, 1);
    particleGfx.fillCircle(4, 4, 4);
    particleGfx.generateTexture('star_particle', 8, 8);
    particleGfx.destroy();
  }
}
