import { describe, expect, it } from 'vitest';
import { clamp, convertValueToPercentage, roundToStep } from './number';

describe('number utils', () => {
  it('should_clamp_value_within_range', () => {
    expect(clamp(5, [0, 10])).toBe(5);
    expect(clamp(-1, [0, 10])).toBe(0);
    expect(clamp(11, [0, 10])).toBe(10);
  });

  it('should_convert_value_to_percentage', () => {
    expect(convertValueToPercentage(50, 0, 100)).toBe(50);
    expect(convertValueToPercentage(0, 0, 0)).toBe(0);
  });

  it('should_round_to_step_or_passthrough', () => {
    expect(roundToStep(7, 0, 5)).toBe(5);
    expect(roundToStep(7, 0, 0)).toBe(7);
  });
});
