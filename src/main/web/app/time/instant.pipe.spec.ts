import { describe, expect, it } from 'vitest';
import { Instant } from '@js-joda/core';
import { DATE_TIME, formatInstant, TIME } from './instant-format';
import { InstantPipe } from './instant.pipe';

describe('InstantPipe', () => {
  const pipe = new InstantPipe();
  const instant = Instant.parse('2026-07-26T08:05:09Z');

  it('should format date and time by default', () => {
    expect(pipe.transform(instant)).toBe(formatInstant(instant, DATE_TIME));
  });

  it('should format only the time with the time pattern', () => {
    expect(pipe.transform(instant, 'time')).toBe(formatInstant(instant, TIME));
  });
});
