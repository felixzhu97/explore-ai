import { describe, expect, it } from 'vitest';
import { Instant } from '@js-joda/core';
import { fromNativeDate, systemZoneName, toNativeDate } from './native-date';

describe('native date bridge', () => {
  const instant = Instant.parse('2026-07-26T08:05:09.123Z');

  it('should keep the same epoch millisecond when converting to a native date', () => {
    expect(toNativeDate(instant).toISOString()).toBe('2026-07-26T08:05:09.123Z');
  });

  it('should round trip an instant through a native date', () => {
    expect(fromNativeDate(toNativeDate(instant)).equals(instant)).toBe(true);
  });

  it('should report the browser time zone name', () => {
    expect(systemZoneName()).toBe(Intl.DateTimeFormat().resolvedOptions().timeZone);
  });
});
