import { NetworkService } from './NetworkService';
import {
  CoinEventPayload,
  DoorOpenedPayload,
  GameClearedPayload,
  GameOverPayload,
  PartnerPositionPayload,
  PlayerAnimation,
  PlayerEventPayload,
  PositionUpdatePayload,
  SwitchStatePayload,
} from '../types/network';

// --- Exported utilities for property testing ---

/**
 * Determines whether a position update should be sent based on state difference.
 * Returns true if at least one field differs between prev and curr.
 */
export function shouldSendPosition(
  prev: PositionUpdatePayload | null,
  curr: PositionUpdatePayload
): boolean {
  if (!prev) return true;
  return (
    prev.x !== curr.x ||
    prev.y !== curr.y ||
    prev.velocityX !== curr.velocityX ||
    prev.velocityY !== curr.velocityY ||
    prev.animation !== curr.animation ||
    prev.flipX !== curr.flipX
  );
}

/**
 * Builds a PositionUpdatePayload from a player sprite's current state.
 */
export function buildPositionMessage(
  sprite: Phaser.Physics.Arcade.Sprite
): PositionUpdatePayload {
  const body = sprite.body as Phaser.Physics.Arcade.Body;
  let animation: PlayerAnimation = 'idle';
  if (!body.blocked.down) {
    animation = 'jump';
  } else if (body.velocity.x !== 0) {
    animation = 'walk';
  }
  return {
    x: sprite.x,
    y: sprite.y,
    velocityX: body.velocity.x,
    velocityY: body.velocity.y,
    animation,
    flipX: sprite.flipX,
    timestamp: Date.now(),
  };
}

/**
 * Linear interpolation between two values.
 */
export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

// --- Configuration interface ---

export interface NetworkGameConfig {
  networkService: NetworkService;
  gameSessionId: string;
  myUserId: string;
  playerAssignment: {
    player1UserId: string;
    player2UserId: string;
  };
  partnerSprite: Phaser.Physics.Arcade.Sprite;
  localPlayerSprite: Phaser.Physics.Arcade.Sprite;
  coins: Phaser.Physics.Arcade.StaticGroup;
  switches: {
    switch1: Phaser.Physics.Arcade.Sprite;
    switch2: Phaser.Physics.Arcade.Sprite;
  };
  platforms: {
    platform1: Phaser.Physics.Arcade.Sprite;
    platform2: Phaser.Physics.Arcade.Sprite;
    platform1BaseY: number;
    platform2BaseY: number;
  };
  door: Phaser.Physics.Arcade.Sprite;
  scoreText: Phaser.GameObjects.Text;
  totalCoins: number;
}

// --- Internal types ---

interface PendingCoin {
  coinId: string;
  sprite: Phaser.Physics.Arcade.Image;
  scoreBeforeCollect: number;
}

// --- NetworkGameManager ---

export class NetworkGameManager {
  private scene!: Phaser.Scene;
  private config!: NetworkGameConfig;
  private networkService!: NetworkService;
  private initialized = false;

  // Position sending
  private positionSendInterval = 50; // ms
  private lastPositionSendTime = 0;
  private lastSentPosition: PositionUpdatePayload | null = null;

  // Partner interpolation
  private partnerTargetX = 0;
  private partnerTargetY = 0;
  private partnerCurrentAnimation: PlayerAnimation = 'idle';
  private partnerFlipX = false;
  private lerpFactor = 0.3;

  // Coin optimistic update tracking
  private pendingCoins: Map<string, PendingCoin> = new Map();
  private score = 0;

  // Partner connection state
  private _partnerConnected = true;

  // Door state
  private _doorOpen = false;

  // --- Public state ---

  get isOnlineMode(): boolean {
    return this.initialized;
  }

  get myPlayerRole(): 'player1' | 'player2' {
    if (!this.config) return 'player1';
    return this.config.myUserId === this.config.playerAssignment.player1UserId
      ? 'player1'
      : 'player2';
  }

  get partnerConnected(): boolean {
    return this._partnerConnected;
  }

  // --- Lifecycle ---

  initialize(scene: Phaser.Scene, config: NetworkGameConfig): void {
    this.scene = scene;
    this.config = config;
    this.networkService = config.networkService;
    this.initialized = true;

    // Initialize partner target to current partner sprite position
    this.partnerTargetX = config.partnerSprite.x;
    this.partnerTargetY = config.partnerSprite.y;

    this.registerListeners();
  }

  destroy(): void {
    if (!this.initialized) return;
    this.unregisterListeners();
    this.pendingCoins.clear();
    this.initialized = false;
  }

  update(time: number, _delta: number): void {
    if (!this.initialized) return;

    // Send position at 50ms interval (only when changed)
    if (time - this.lastPositionSendTime >= this.positionSendInterval) {
      this.sendPositionIfChanged();
      this.lastPositionSendTime = time;
    }

    // Interpolate partner sprite toward target position
    this.interpolatePartner();
  }

  // --- Coin collection (called from Level scene) ---

  /**
   * Called when the local player overlaps a coin.
   * Performs optimistic update and sends COIN_COLLECTED to server.
   * Returns true if the coin was handled (online mode), false otherwise.
   */
  handleCoinCollected(coinSprite: Phaser.Physics.Arcade.Image, coinId: string): boolean {
    if (!this.initialized) return false;

    // Optimistic update: hide coin, increment score
    coinSprite.setVisible(false);
    if (coinSprite.body) {
      coinSprite.body.enable = false;
    }
    this.score += 1;
    this.config.scoreText.setText(`달: ${this.score}`);

    // Track pending coin for potential rollback
    this.pendingCoins.set(coinId, {
      coinId,
      sprite: coinSprite,
      scoreBeforeCollect: this.score - 1,
    });

    // Send to server
    this.networkService.sendGameEvent('COIN_COLLECTED', { targetId: coinId });

    return true;
  }

  // --- Switch events (called from Level scene) ---

  /**
   * Called when a switch is pressed by the local player.
   */
  handleSwitchPressed(switchId: string): void {
    if (!this.initialized) return;
    this.networkService.sendGameEvent('SWITCH_PRESSED', { targetId: switchId });
  }

  /**
   * Called when a switch is released by the local player.
   */
  handleSwitchReleased(switchId: string): void {
    if (!this.initialized) return;
    this.networkService.sendGameEvent('SWITCH_RELEASED', { targetId: switchId });
  }

  // --- Clear request ---

  /**
   * Called when both players are near the door (local check).
   */
  sendClearRequest(): void {
    if (!this.initialized) return;
    this.networkService.sendGameEvent('CLEAR_REQUEST', { targetId: 'door' });
  }

  // --- Private: Position sending ---

  private sendPositionIfChanged(): void {
    const currentPosition = buildPositionMessage(this.config.localPlayerSprite);

    if (shouldSendPosition(this.lastSentPosition, currentPosition)) {
      this.networkService.sendPositionUpdate(currentPosition);
      this.lastSentPosition = currentPosition;
    }
  }

  // --- Private: Partner interpolation ---

  private interpolatePartner(): void {
    const partner = this.config.partnerSprite;

    // Lerp position
    partner.x = lerp(partner.x, this.partnerTargetX, this.lerpFactor);
    partner.y = lerp(partner.y, this.partnerTargetY, this.lerpFactor);

    // Apply animation
    const animPrefix = this.myPlayerRole === 'player1' ? 'pink' : 'blue';
    const animKey = `${animPrefix}-${this.partnerCurrentAnimation}`;
    if (partner.anims.currentAnim?.key !== animKey) {
      partner.play(animKey);
    }

    // Apply flip
    partner.setFlipX(this.partnerFlipX);
  }

  // --- Private: Event listeners ---

  private onPartnerPosition = (payload: PartnerPositionPayload): void => {
    // Ignore own position messages (broadcast includes sender)
    if (payload.senderId === this.config.myUserId) return;

    this.partnerTargetX = payload.x;
    this.partnerTargetY = payload.y;
    this.partnerCurrentAnimation = payload.animation;
    this.partnerFlipX = payload.flipX;
  };

  private onCoinConfirmed = (payload: CoinEventPayload): void => {
    const coinId = payload.coinId;

    // Remove from pending (optimistic update confirmed)
    if (this.pendingCoins.has(coinId)) {
      // Our own coin was confirmed - destroy the sprite
      const pending = this.pendingCoins.get(coinId)!;
      pending.sprite.destroy();
      this.pendingCoins.delete(coinId);
    } else {
      // Partner collected a coin - find and remove it
      const coinSprite = this.findCoinById(coinId);
      if (coinSprite) {
        coinSprite.destroy();
      }
      // Update score from server's authoritative count
      this.score = payload.totalCollected;
      this.config.scoreText.setText(`달: ${this.score}`);
    }
  };

  private onCoinRejected = (payload: CoinEventPayload): void => {
    const coinId = payload.coinId;

    // Rollback optimistic update
    if (this.pendingCoins.has(coinId)) {
      const pending = this.pendingCoins.get(coinId)!;
      // Restore coin visibility
      pending.sprite.setVisible(true);
      if (pending.sprite.body) {
        pending.sprite.body.enable = true;
      }
      // Rollback score
      this.score = pending.scoreBeforeCollect;
      this.config.scoreText.setText(`달: ${this.score}`);
      this.pendingCoins.delete(coinId);
    }
  };

  private onSwitchState = (payload: SwitchStatePayload): void => {
    const { switchId, pressed } = payload;
    const switchSprite = switchId === 'switch1'
      ? this.config.switches.switch1
      : this.config.switches.switch2;

    // Update switch visual (frame 7 = pressed, frame 6 = released)
    switchSprite.setFrame(pressed ? 7 : 6);

    // Update platform positions based on switch states
    this.updatePlatformsFromSwitchState(switchId, pressed);
  };

  private onDoorOpened = (_payload: DoorOpenedPayload): void => {
    if (this._doorOpen) return;
    this._doorOpen = true;

    // Change door sprite to open state
    this.config.door.setTexture('door');

    // Notify the scene that door is open (for checkClear logic)
    this.scene.events.emit('network-door-opened');
  };

  private onGameCleared = (payload: GameClearedPayload): void => {
    // Pause physics and show clear UI
    this.scene.physics.pause();

    // Transition to Result scene with clear data
    this.scene.scene.start('Result', {
      type: 'cleared',
      score: payload.score,
      clearTimeMs: payload.clearTimeMs,
      intimacyPoints: payload.intimacyPoints,
    });
  };

  private onGameOver = (payload: GameOverPayload): void => {
    // Pause physics
    this.scene.physics.pause();

    // Transition to Result scene with game over data
    this.scene.scene.start('Result', {
      type: 'gameover',
      reason: payload.reason,
      score: payload.score,
      elapsedTimeMs: payload.elapsedTimeMs,
    });
  };

  private onPlayerDisconnected = (_payload: PlayerEventPayload): void => {
    this._partnerConnected = false;
    // Apply alpha effect to partner sprite
    this.config.partnerSprite.setAlpha(0.5);
  };

  private onPlayerReconnected = (_payload: PlayerEventPayload): void => {
    this._partnerConnected = true;
    // Restore partner sprite alpha
    this.config.partnerSprite.setAlpha(1.0);
  };

  private registerListeners(): void {
    this.networkService.on('PARTNER_POSITION', this.onPartnerPosition);
    this.networkService.on('COIN_CONFIRMED', this.onCoinConfirmed);
    this.networkService.on('COIN_REJECTED', this.onCoinRejected);
    this.networkService.on('SWITCH_STATE', this.onSwitchState);
    this.networkService.on('DOOR_OPENED', this.onDoorOpened);
    this.networkService.on('GAME_CLEARED', this.onGameCleared);
    this.networkService.on('GAME_OVER', this.onGameOver);
    this.networkService.on('PLAYER_DISCONNECTED', this.onPlayerDisconnected);
    this.networkService.on('PLAYER_RECONNECTED', this.onPlayerReconnected);
  }

  private unregisterListeners(): void {
    this.networkService.off('PARTNER_POSITION', this.onPartnerPosition);
    this.networkService.off('COIN_CONFIRMED', this.onCoinConfirmed);
    this.networkService.off('COIN_REJECTED', this.onCoinRejected);
    this.networkService.off('SWITCH_STATE', this.onSwitchState);
    this.networkService.off('DOOR_OPENED', this.onDoorOpened);
    this.networkService.off('GAME_CLEARED', this.onGameCleared);
    this.networkService.off('GAME_OVER', this.onGameOver);
    this.networkService.off('PLAYER_DISCONNECTED', this.onPlayerDisconnected);
    this.networkService.off('PLAYER_RECONNECTED', this.onPlayerReconnected);
  }

  // --- Private: Helpers ---

  private findCoinById(coinId: string): Phaser.Physics.Arcade.Image | null {
    const children = this.config.coins.getChildren() as Phaser.Physics.Arcade.Image[];
    // coinId is the index-based identifier (e.g., "coin_0", "coin_1", ...)
    for (const child of children) {
      if (child.getData('coinId') === coinId) {
        return child;
      }
    }
    return null;
  }

  private updatePlatformsFromSwitchState(switchId: string, pressed: boolean): void {
    const { platform1, platform2, platform1BaseY, platform2BaseY } = this.config.platforms;

    if (switchId === 'switch1') {
      const targetY = pressed ? platform1BaseY + 90 : platform1BaseY;
      platform1.y = targetY;
      (platform1.body as Phaser.Physics.Arcade.StaticBody).updateFromGameObject();

      // switch1 also affects platform2
      const platform2TargetY = pressed ? platform2BaseY + 90 : platform2BaseY;
      platform2.y = platform2TargetY;
      (platform2.body as Phaser.Physics.Arcade.StaticBody).updateFromGameObject();
    } else if (switchId === 'switch2') {
      const targetY = pressed ? platform2BaseY + 90 : platform2BaseY;
      platform2.y = targetY;
      (platform2.body as Phaser.Physics.Arcade.StaticBody).updateFromGameObject();
    }
  }
}
