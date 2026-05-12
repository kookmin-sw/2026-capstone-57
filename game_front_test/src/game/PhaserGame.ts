import Phaser from 'phaser';
import { gameConfig } from './config/gameConfig';

export class PhaserGame extends Phaser.Game {
  constructor(config?: Partial<Phaser.Types.Core.GameConfig>) {
    const mergedConfig: Phaser.Types.Core.GameConfig = {
      ...gameConfig,
      ...config,
    };
    super(mergedConfig);
  }
}
