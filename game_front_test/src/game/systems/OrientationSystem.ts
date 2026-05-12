import Phaser from 'phaser';

/**
 * OrientationSystem - 디바이스 방향을 감지하고,
 * 세로 모드(portrait) 시 전체 화면 HTML 오버레이를 표시합니다.
 *
 * Phaser 캔버스 위에 absolute positioned div를 사용하여
 * 게임 렌더링과 독립적으로 동작합니다.
 */
export class OrientationSystem {
  private game: Phaser.Game | null = null;
  private overlay: HTMLDivElement | null = null;
  private mediaQuery: MediaQueryList | null = null;
  private boundHandler: ((e: MediaQueryListEvent) => void) | null = null;

  activate(game: Phaser.Game): void {
    this.game = game;
    this.createOverlay();

    this.mediaQuery = window.matchMedia('(orientation: portrait)');
    this.boundHandler = this.handleOrientationChange.bind(this);
    this.mediaQuery.addEventListener('change', this.boundHandler);

    // 초기 상태 확인
    if (this.mediaQuery.matches) {
      this.showOverlay();
    }
  }

  deactivate(): void {
    if (this.mediaQuery && this.boundHandler) {
      this.mediaQuery.removeEventListener('change', this.boundHandler);
    }
    this.removeOverlay();
    this.mediaQuery = null;
    this.boundHandler = null;
    this.game = null;
  }

  // ---------------------------------------------------------------------------
  // Private
  // ---------------------------------------------------------------------------

  private handleOrientationChange(e: MediaQueryListEvent): void {
    if (e.matches) {
      this.showOverlay();
    } else {
      this.hideOverlay();
      this.game?.scale.refresh();
    }
  }

  private createOverlay(): void {
    if (this.overlay) return;

    const div = document.createElement('div');
    div.id = 'orientation-overlay';
    div.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(27, 40, 56, 0.95);
      display: none;
      justify-content: center;
      align-items: center;
      flex-direction: column;
      z-index: 99999;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    `;

    div.innerHTML = `
      <div style="font-size: 48px; margin-bottom: 24px;">📱🔄</div>
      <p style="color: #FFF4B8; font-size: 20px; text-align: center; margin: 0; padding: 0 24px; line-height: 1.6;">
        화면을 가로로 회전해주세요
      </p>
      <p style="color: #B8A9C9; font-size: 14px; margin-top: 12px;">
        게임은 가로 모드에서만 플레이할 수 있습니다
      </p>
    `;

    document.body.appendChild(div);
    this.overlay = div;
  }

  private showOverlay(): void {
    if (this.overlay) {
      this.overlay.style.display = 'flex';
    }
  }

  private hideOverlay(): void {
    if (this.overlay) {
      this.overlay.style.display = 'none';
    }
  }

  private removeOverlay(): void {
    if (this.overlay) {
      this.overlay.remove();
      this.overlay = null;
    }
  }
}
