import Phaser from 'phaser';
import { BootScene } from '../scenes/BootScene';
import { LobbyScene } from '../scenes/LobbyScene';
import { CoopScene } from '../scenes/CoopScene';
import { ResultScene } from '../scenes/ResultScene';

export const gameConfig: Phaser.Types.Core.GameConfig = {
  type: Phaser.AUTO,
  width: 1280,
  height: 720,
  parent: 'game-container',
  scale: {
    mode: Phaser.Scale.FIT,
    autoCenter: Phaser.Scale.CENTER_BOTH,
  },
  physics: {
    default: 'arcade',
    arcade: {
      gravity: { x: 0, y: 0 },
    },
  },
  scene: [BootScene, LobbyScene, CoopScene, ResultScene],
};
