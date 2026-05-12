import { PhaserGame } from './game/PhaserGame';
import { parseGameParams } from './utils/parseGameParams';

export { parseGameParams } from './utils/parseGameParams';
export type { ParseResult } from './utils/parseGameParams';
export type { GameParams } from './game/types/gameTypes';

export function showError(message: string): void {
  const container = document.getElementById('game-container');
  if (container) {
    container.innerHTML = `
      <div style="
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        color: #FFFFFF;
        font-family: sans-serif;
        font-size: 1.2rem;
        text-align: center;
        padding: 2rem;
        background-color: #1B2838;
      ">
        <p>${message}</p>
      </div>
    `;
  }
}

function boot(): void {
  const result = parseGameParams();
  if (!result.params) {
    showError(result.error!);
    return;
  }

  const game = new PhaserGame();

  // Store game params on the game registry for later use by scenes/systems
  game.registry.set('gameParams', result.params);
}

boot();
