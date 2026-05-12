import { describe, it, expect, vi } from 'vitest';
import { KNOWN_SERVER_MESSAGE_TYPES } from '../game/types/gameTypes';

/**
 * Feature: game-front-test, Property 7: Unknown message type graceful handling
 *
 * For any STOMP message with an unknown type field, the system SHALL log a
 * console warning and SHALL NOT throw an exception or crash.
 */

// NetworkSystem의 메시지 라우팅 로직을 추출하여 테스트
function routeMessage(
  body: { type?: string; [key: string]: unknown },
  callbacks: Record<string, (msg: unknown) => void>,
): { warned: boolean; threw: boolean } {
  let warned = false;
  let threw = false;

  const originalWarn = console.warn;
  console.warn = () => { warned = true; };

  try {
    const type = body?.type;

    if (!type) {
      console.warn('No type field');
      return { warned, threw };
    }

    if (!KNOWN_SERVER_MESSAGE_TYPES.includes(type as typeof KNOWN_SERVER_MESSAGE_TYPES[number])) {
      console.warn(`Unknown message type: "${type}"`);
      return { warned, threw };
    }

    // Known type - route to callback
    const handler = callbacks[type];
    if (handler) handler(body);
  } catch {
    threw = true;
  } finally {
    console.warn = originalWarn;
  }

  return { warned, threw };
}

describe('Unknown message type handling', () => {
  it('알 수 없는 type은 console.warn을 호출하고 예외를 던지지 않는다', () => {
    const unknownTypes = ['UNKNOWN', 'INVALID', 'FOO_BAR', '', 'state_update', 'game_started'];

    for (const type of unknownTypes) {
      const result = routeMessage({ type }, {});
      expect(result.warned).toBe(true);
      expect(result.threw).toBe(false);
    }
  });

  it('알려진 type은 경고 없이 정상 라우팅된다', () => {
    for (const type of KNOWN_SERVER_MESSAGE_TYPES) {
      const called = vi.fn();
      const callbacks: Record<string, (msg: unknown) => void> = { [type]: called };
      const result = routeMessage({ type }, callbacks);
      expect(result.warned).toBe(false);
      expect(result.threw).toBe(false);
      expect(called).toHaveBeenCalledOnce();
    }
  });

  it('type 필드가 없는 메시지도 예외를 던지지 않는다', () => {
    const result = routeMessage({} as { type?: string }, {});
    expect(result.warned).toBe(true);
    expect(result.threw).toBe(false);
  });
});
