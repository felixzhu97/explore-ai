import { describe, expect, it } from 'vitest';
import { Instant, LocalDateTime, ZoneId, ZoneOffset } from '@js-joda/core';
import { DATE_TIME, formatInstant, ONCE_TERMINAL_NEXT, TIME } from './instant-format';

describe('formatInstant', () => {
  const instant = Instant.parse('2026-07-26T08:05:09.123Z');

  it('should format date and time in the given zone', () => {
    expect(formatInstant(instant, DATE_TIME, ZoneOffset.UTC)).toBe('2026-07-26 08:05:09');
    expect(formatInstant(instant, DATE_TIME, ZoneOffset.ofHours(8))).toBe('2026-07-26 16:05:09');
  });

  it('should format only the time of day with the time pattern', () => {
    expect(formatInstant(instant, TIME, ZoneOffset.UTC)).toBe('08:05:09');
  });

  it('should use the system zone when no zone is given', () => {
    const local = LocalDateTime.ofInstant(instant, ZoneId.SYSTEM);
    expect(formatInstant(instant, TIME)).toBe(local.format(TIME));
  });
});

describe('ONCE_TERMINAL_NEXT', () => {
  it('should match the backend sentinel for finished one-time schedules', () => {
    expect(ONCE_TERMINAL_NEXT.toString()).toBe('9999-12-31T23:59:59Z');
  });
});
