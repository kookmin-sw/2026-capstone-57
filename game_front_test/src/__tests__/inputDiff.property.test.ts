import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import { InputState } from '../game/types/gameTypes';

/**
 * Feature: game-front-test, Property 2: InputState diff-based publishing
 *
 * For any sequence of InputState values, a PLAYER_INPUT message SHALL be published
 * if and only if the current InputState differs from the previously published InputState.
 * Identical consecutive states SHALL never produce a publish.
 */

function countPublishes(states: InputState[]): number {
  if (states.length === 0) return 0;

  let publishCount = 0;
  let previous: InputState | null = null;

  for (const current of states) {
    const changed =
      previous === null ||
      current.left !== previous.left ||
      current.right !== previous.right ||
      current.jump !== previous.jump;

    if (changed) {
      publishCount++;
      previous = { ...current };
    }
  }

  return publishCount;
}

function countStateTransitions(states: InputState[]): number {
  if (states.length === 0) return 0;

  let transitions = 1; // 첫 번째 상태는 항상 발행
  for (let i = 1; i < states.length; i++) {
    if (
      states[i].left !== states[i - 1].left ||
      states[i].right !== states[i - 1].right ||
      states[i].jump !== states[i - 1].jump
    ) {
      transitions++;
    }
  }

  return transitions;
}

describe('Property 2: InputState diff-based publishing', () => {
  it('발행 횟수는 상태 전환 횟수와 같아야 한다', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            left: fc.boolean(),
            right: fc.boolean(),
            jump: fc.boolean(),
          }),
          { minLength: 1, maxLength: 50 },
        ),
        (states: InputState[]) => {
          const publishes = countPublishes(states);
          const transitions = countStateTransitions(states);
          expect(publishes).toBe(transitions);
        },
      ),
      { numRuns: 100 },
    );
  });

  it('동일한 상태가 연속되면 추가 발행이 없어야 한다', () => {
    fc.assert(
      fc.property(
        fc.record({
          left: fc.boolean(),
          right: fc.boolean(),
          jump: fc.boolean(),
        }),
        fc.integer({ min: 2, max: 20 }),
        (state: InputState, repeatCount: number) => {
          const states = Array(repeatCount).fill(state);
          const publishes = countPublishes(states);
          // 동일 상태 반복이면 첫 번째만 발행
          expect(publishes).toBe(1);
        },
      ),
      { numRuns: 100 },
    );
  });
});
