import { describe, expect, it } from 'vitest';
import { clamp, convertValueToPercentage, roundToStep } from './number';

describe('number utils', () => {
  it('should clamp value within range', () => {
    expect(clamp(5, [0, 10])).toBe(5);
    expect(clamp(-1, [0, 10])).toBe(0);
    expect(clamp(11, [0, 10])).toBe(10);
  });

  it('should convert value to percentage', () => {
    expect(convertValueToPercentage(50, 0, 100)).toBe(50);
    expect(convertValueToPercentage(0, 0, 0)).toBe(0);
  });

  it('should round to step or passthrough', () => {
    expect(roundToStep(7, 0, 5)).toBe(5);
    expect(roundToStep(7, 0, 0)).toBe(7);
  });
});
