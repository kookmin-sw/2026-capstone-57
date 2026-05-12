import { PhaserGame } from './game/PhaserGame';

export interface GameParams {
  token: string;
  gameSessionId: string;
  userId?: string;
}

export function parseGameParams(): GameParams | null {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');
  const gameSessionId = params.get('gameSessionId');
  const userId = params.get('userId') ?? undefined;

  if (!token) {
    showError('인증 토큰이 없습니다. 올바른 링크로 접속해주세요.');
    return null;
  }

  if (!gameSessionId) {
    showError('게임 세션 정보가 없습니다. 올바른 링크로 접속해주세요.');
    return null;
  }

  return { token, gameSessionId, userId };
}

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
  const gameParams = parseGameParams();
  if (!gameParams) {
    return;
  }

  const game = new PhaserGame();

  // Store game params on the game registry for later use by scenes/systems
  game.registry.set('gameParams', gameParams);
}

boot();
