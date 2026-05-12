import Phaser from 'phaser';
import { InputState } from '../types/gameTypes';
import { NetworkSystem } from './NetworkSystem';

/**
 * InputSystem - 키보드/터치 입력을 InputState로 변환하고,
 * 변경 시에만 NetworkSystem을 통해 PLAYER_INPUT을 발행합니다.
 *
 * 키보드 매핑:
 *   Arrow Left / A → left
 *   Arrow Right / D → right
 *   Space / W / Arrow Up → jump
 *
 * CoopScene 생명주기에 바인딩: activate() / deactivate()
 */
export class InputSystem {
  private scene: Phaser.Scene | null = null;
  private keys: {
    left1?: Phaser.Input.Keyboard.Key;
    left2?: Phaser.Input.Keyboard.Key;
    right1?: Phaser.Input.Keyboard.Key;
    right2?: Phaser.Input.Keyboard.Key;
    jump1?: Phaser.Input.Keyboard.Key;
    jump2?: Phaser.Input.Keyboard.Key;
    jump3?: Phaser.Input.Keyboard.Key;
  } = {};

  private currentState: InputState = { left: false, right: false, jump: false };
  private previousState: InputState = { left: false, right: false, jump: false };
  private active = false;

  // 모바일 터치 버튼
  private touchButtons: Phaser.GameObjects.GameObject[] = [];
  private touchState: InputState = { left: false, right: false, jump: false };

  activate(scene: Phaser.Scene): void {
    this.scene = scene;
    this.active = true;
    this.currentState = { left: false, right: false, jump: false };
    this.previousState = { left: false, right: false, jump: false };
    this.touchState = { left: false, right: false, jump: false };

    this.setupKeyboard();

    if (this.isMobile()) {
      this.createTouchButtons();
    }
  }

  deactivate(): void {
    this.active = false;
    this.destroyTouchButtons();
    this.keys = {};
    this.scene = null;
  }

  /**
   * update()는 씬의 update 루프에서 호출됩니다.
   * 키보드 + 터치 상태를 합산하고, diff가 있으면 발행합니다.
   */
  update(): void {
    if (!this.active) return;

    const keyboardState = this.readKeyboardState();

    // 키보드 OR 터치 중 하나라도 true면 true
    this.currentState = {
      left: keyboardState.left || this.touchState.left,
      right: keyboardState.right || this.touchState.right,
      jump: keyboardState.jump || this.touchState.jump,
    };

    if (this.hasChanged()) {
      NetworkSystem.getInstance().publishInput({ ...this.currentState });
      this.previousState = { ...this.currentState };
    }
  }

  getCurrentState(): InputState {
    return { ...this.currentState };
  }

  isActive(): boolean {
    return this.active;
  }

  // ---------------------------------------------------------------------------
  // Private - Keyboard
  // ---------------------------------------------------------------------------

  private setupKeyboard(): void {
    if (!this.scene?.input.keyboard) return;

    const kb = this.scene.input.keyboard;
    this.keys = {
      left1: kb.addKey(Phaser.Input.Keyboard.KeyCodes.LEFT),
      left2: kb.addKey(Phaser.Input.Keyboard.KeyCodes.A),
      right1: kb.addKey(Phaser.Input.Keyboard.KeyCodes.RIGHT),
      right2: kb.addKey(Phaser.Input.Keyboard.KeyCodes.D),
      jump1: kb.addKey(Phaser.Input.Keyboard.KeyCodes.SPACE),
      jump2: kb.addKey(Phaser.Input.Keyboard.KeyCodes.W),
      jump3: kb.addKey(Phaser.Input.Keyboard.KeyCodes.UP),
    };
  }

  private readKeyboardState(): InputState {
    return {
      left: !!(this.keys.left1?.isDown || this.keys.left2?.isDown),
      right: !!(this.keys.right1?.isDown || this.keys.right2?.isDown),
      jump: !!(this.keys.jump1?.isDown || this.keys.jump2?.isDown || this.keys.jump3?.isDown),
    };
  }

  // ---------------------------------------------------------------------------
  // Private - Touch Buttons (모바일)
  // ---------------------------------------------------------------------------

  private createTouchButtons(): void {
    if (!this.scene) return;

    const { width, height } = this.scene.scale;
    const btnSize = 64; // 48px 최소 요구사항 초과
    const padding = 20;
    const y = height - btnSize - padding;

    // 왼쪽 버튼
    const leftBtn = this.createButton(
      padding + btnSize / 2,
      y,
      btnSize,
      '◀',
      () => { this.touchState.left = true; this.update(); },
      () => { this.touchState.left = false; this.update(); },
    );

    // 오른쪽 버튼
    const rightBtn = this.createButton(
      padding + btnSize * 2,
      y,
      btnSize,
      '▶',
      () => { this.touchState.right = true; this.update(); },
      () => { this.touchState.right = false; this.update(); },
    );

    // 점프 버튼
    const jumpBtn = this.createButton(
      width - padding - btnSize / 2,
      y,
      btnSize,
      '▲',
      () => { this.touchState.jump = true; this.update(); },
      () => { this.touchState.jump = false; this.update(); },
    );

    this.touchButtons.push(leftBtn, rightBtn, jumpBtn);
  }

  private createButton(
    x: number,
    y: number,
    size: number,
    label: string,
    onDown: () => void,
    onUp: () => void,
  ): Phaser.GameObjects.Container {
    if (!this.scene) throw new Error('Scene not available');

    const bg = this.scene.add.circle(0, 0, size / 2, 0xb8a9c9, 0.7);
    const text = this.scene.add.text(0, 0, label, {
      fontSize: '24px',
      color: '#ffffff',
    }).setOrigin(0.5);

    const container = this.scene.add.container(x, y, [bg, text]);
    container.setSize(size, size);
    container.setInteractive();
    container.setDepth(1000);

    container.on('pointerdown', onDown);
    container.on('pointerup', onUp);
    container.on('pointerout', onUp);

    return container;
  }

  private destroyTouchButtons(): void {
    for (const btn of this.touchButtons) {
      btn.destroy();
    }
    this.touchButtons = [];
  }

  // ---------------------------------------------------------------------------
  // Private - Helpers
  // ---------------------------------------------------------------------------

  /** InputState가 이전과 다른지 비교 */
  private hasChanged(): boolean {
    return (
      this.currentState.left !== this.previousState.left ||
      this.currentState.right !== this.previousState.right ||
      this.currentState.jump !== this.previousState.jump
    );
  }

  private isMobile(): boolean {
    return /Android|iPhone|iPad|iPod|webOS|BlackBerry|IEMobile|Opera Mini/i.test(
      navigator.userAgent,
    );
  }
}
