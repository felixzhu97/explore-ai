import { describe, expect, it } from 'vitest';
import { hasItems, hasText, textOr } from './presence';

describe('presence', () => {
  it('should treat a list with elements as present', () => {
    expect(hasItems([1])).toBe(true);
  });

  it('should treat an empty list, null or undefined as absent', () => {
    expect(hasItems([])).toBe(false);
    expect(hasItems(null)).toBe(false);
    expect(hasItems(undefined)).toBe(false);
  });

  it('should treat a non-empty string as present', () => {
    expect(hasText(' ')).toBe(true);
  });

  it('should treat an empty string, null or undefined as absent', () => {
    expect(hasText('')).toBe(false);
    expect(hasText(null)).toBe(false);
    expect(hasText(undefined)).toBe(false);
  });

  it('should fall back only when the text is absent', () => {
    expect(textOr('a', 'b')).toBe('a');
    expect(textOr('', 'b')).toBe('b');
    expect(textOr(null, undefined)).toBeUndefined();
  });
});
