import { describe, it, expect } from 'vitest';
import { parseGameParams } from '../utils/parseGameParams';

describe('parseGameParams', () => {
  describe('token 파라미터', () => {
    it('token이 있으면 정상 추출', () => {
      const result = parseGameParams('?token=abc123&gameSessionId=session1');
      expect(result.params?.token).toBe('abc123');
      expect(result.error).toBeNull();
    });

    it('token이 없으면 에러 반환', () => {
      const result = parseGameParams('?gameSessionId=session1');
      expect(result.params).toBeNull();
      expect(result.error).toBe('인증 토큰이 없습니다. 올바른 링크로 접속해주세요.');
    });

    it('token이 빈 문자열이면 에러 반환', () => {
      const result = parseGameParams('?token=&gameSessionId=session1');
      expect(result.params).toBeNull();
      expect(result.error).toBe('인증 토큰이 없습니다. 올바른 링크로 접속해주세요.');
    });
  });

  describe('gameSessionId 파라미터', () => {
    it('gameSessionId가 있으면 정상 추출', () => {
      const result = parseGameParams('?token=abc&gameSessionId=session-xyz');
      expect(result.params?.gameSessionId).toBe('session-xyz');
      expect(result.error).toBeNull();
    });

    it('gameSessionId가 없으면 에러 반환', () => {
      const result = parseGameParams('?token=abc');
      expect(result.params).toBeNull();
      expect(result.error).toBe('게임 세션 정보가 없습니다. 올바른 링크로 접속해주세요.');
    });

    it('gameSessionId가 빈 문자열이면 에러 반환', () => {
      const result = parseGameParams('?token=abc&gameSessionId=');
      expect(result.params).toBeNull();
      expect(result.error).toBe('게임 세션 정보가 없습니다. 올바른 링크로 접속해주세요.');
    });
  });

  describe('userId 파라미터', () => {
    it('userId가 있으면 추출', () => {
      const result = parseGameParams('?token=abc&gameSessionId=s1&userId=user1');
      expect(result.params?.userId).toBe('user1');
    });

    it('userId가 없으면 undefined', () => {
      const result = parseGameParams('?token=abc&gameSessionId=s1');
      expect(result.params?.userId).toBeUndefined();
    });
  });

  describe('복합 시나리오', () => {
    it('모든 파라미터가 있으면 정상 반환', () => {
      const result = parseGameParams('?token=jwt-token&gameSessionId=game-123&userId=player-A');
      expect(result.params).toEqual({
        token: 'jwt-token',
        gameSessionId: 'game-123',
        userId: 'player-A',
      });
      expect(result.error).toBeNull();
    });

    it('파라미터가 전혀 없으면 token 에러 우선', () => {
      const result = parseGameParams('');
      expect(result.params).toBeNull();
      expect(result.error).toContain('인증 토큰');
    });

    it('쿼리 문자열 없이 호출해도 동작', () => {
      const result = parseGameParams('?');
      expect(result.params).toBeNull();
    });
  });
});
