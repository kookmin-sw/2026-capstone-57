import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import { InputState } from '../game/types/gameTypes';

/**
 * Feature: game-front-test, Property 6: PLAYER_INPUT message format correctness
 *
 * For any valid InputState, the published STOMP message SHALL have the exact structure
 * { "type": "PLAYER_INPUT", "input": { "left": boolean, "right": boolean, "jump": boolean } }
 * with no additional fields (specifically no userId).
 */

function buildPlayerInputMessage(input: InputState): object {
  return {
    type: 'PLAYER_INPUT',
    input: {
      left: input.left,
      right: input.right,
      jump: input.jump,
    },
  };
}

describe('Property 6: PLAYER_INPUT message format correctness', () => {
  it('메시지 구조가 정확히 { type, input: { left, right, jump } }이어야 한다', () => {
    fc.assert(
      fc.property(
        fc.record({
          left: fc.boolean(),
          right: fc.boolean(),
          jump: fc.boolean(),
        }),
        (inputState: InputState) => {
          const message = buildPlayerInputMessage(inputState);
          const parsed = JSON.parse(JSON.stringify(message));

          // 구조 검증
          expect(parsed.type).toBe('PLAYER_INPUT');
          expect(parsed.input).toBeDefined();
          expect(typeof parsed.input.left).toBe('boolean');
          expect(typeof parsed.input.right).toBe('boolean');
          expect(typeof parsed.input.jump).toBe('boolean');

          // userId가 없어야 함
          expect(parsed).not.toHaveProperty('userId');
          expect(parsed.input).not.toHaveProperty('userId');

          // 정확히 2개 키만 (type, input)
          expect(Object.keys(parsed)).toHaveLength(2);
          expect(Object.keys(parsed.input)).toHaveLength(3);
        },
      ),
      { numRuns: 100 },
    );
  });
});
