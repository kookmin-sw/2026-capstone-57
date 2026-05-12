import { GameParams } from '../game/types/gameTypes';

export interface ParseResult {
  params: GameParams | null;
  error: string | null;
}

/**
 * URL 쿼리 파라미터에서 게임 파라미터를 추출합니다.
 * 순수 함수로, searchString을 인자로 받아 테스트 가능합니다.
 */
export function parseGameParams(searchString?: string): ParseResult {
  const search = searchString ?? (typeof window !== 'undefined' ? window.location.search : '');
  const params = new URLSearchParams(search);
  const token = params.get('token');
  const gameSessionId = params.get('gameSessionId');
  const userId = params.get('userId') ?? undefined;

  if (!token) {
    return { params: null, error: '인증 토큰이 없습니다. 올바른 링크로 접속해주세요.' };
  }

  if (!gameSessionId) {
    return { params: null, error: '게임 세션 정보가 없습니다. 올바른 링크로 접속해주세요.' };
  }

  return { params: { token, gameSessionId, userId }, error: null };
}
