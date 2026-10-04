import { describe, expect, it } from 'vitest';
import { LocalDate, ZoneOffset } from '@js-joda/core';
import { buildRiverTimeline, monthFromLabel, parseRiverDay } from './river-timeline.util';

describe('parseRiverDay', () => {
  it('should read ISO months, days and date-times', () => {
    expect(parseRiverDay('2024-03')?.toString()).toBe('2024-03-01');
    expect(parseRiverDay('2024-03-15')?.toString()).toBe('2024-03-15');
    expect(parseRiverDay('2024-03-15T10:00:00Z')?.toString()).toBe('2024-03-15');
  });

  it('should read Chinese year-month and slash dates', () => {
    expect(parseRiverDay('2024年3月')?.toString()).toBe('2024-03-01');
    expect(parseRiverDay('2024/3/5')?.toString()).toBe('2024-03-05');
  });

  it('should return null for labels that are not dates', () => {
    expect(parseRiverDay('1月')).toBeNull();
    expect(parseRiverDay('Q1')).toBeNull();
    expect(parseRiverDay('2024-13')).toBeNull();
    expect(parseRiverDay('2024-02-30')).toBeNull();
  });
});

describe('monthFromLabel', () => {
  it('should read bare Chinese months and English month names', () => {
    expect(monthFromLabel('3月')).toBe(3);
    expect(monthFromLabel('March')).toBe(3);
    expect(monthFromLabel('dec')).toBe(12);
  });

  it('should return null outside 1 to 12 or for other labels', () => {
    expect(monthFromLabel('13月')).toBeNull();
    expect(monthFromLabel('Week 1')).toBeNull();
  });
});

describe('buildRiverTimeline', () => {
  it('should place bare months in the year of the first dated label', () => {
    const timeline = buildRiverTimeline(['2023-11', '12月']);
    expect(timeline.toAxisTime('2023-11')).toBe('2023-11-01');
    expect(timeline.toAxisTime('12月')).toBe('2023-12-01');
  });

  it('should fall back to consecutive months for unknown labels', () => {
    const timeline = buildRiverTimeline(['Alpha', 'Beta', 'Gamma']);
    expect(['Alpha', 'Beta', 'Gamma'].map(timeline.toAxisTime))
      .toEqual(['2024-01-01', '2024-02-01', '2024-03-01']);
  });

  it('should label axis ticks with the original label or the month', () => {
    const timeline = buildRiverTimeline(['1月', '2月'], ZoneOffset.UTC);
    const january = LocalDate.parse('2024-01-01').atStartOfDay(ZoneOffset.UTC).toInstant();
    const midFebruary = LocalDate.parse('2024-02-15').atStartOfDay(ZoneOffset.UTC).toInstant();
    expect(timeline.formatLabel(january.toEpochMilli())).toBe('1月');
    expect(timeline.formatLabel(midFebruary.toEpochMilli())).toBe('2月');
    expect(timeline.formatLabel('2024-02-01')).toBe('2月');
    expect(timeline.formatLabel(Number.NaN)).toBe('NaN');
  });
});
