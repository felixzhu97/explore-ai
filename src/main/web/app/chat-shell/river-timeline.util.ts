import {
  DateTimeException,
  DateTimeFormatter,
  DateTimeParseException,
  Instant,
  LocalDate,
  YearMonth,
  ZoneId,
} from '@js-joda/core';

const DEFAULT_YEAR = 2024;
const SLASH_DATE = DateTimeFormatter.ofPattern('yyyy/M/d');
const MONTH_NAMES: Record<string, number> = {
  jan: 1, feb: 2, mar: 3, apr: 4, may: 5, jun: 6,
  jul: 7, aug: 8, sep: 9, oct: 10, nov: 11, dec: 12,
};

export interface RiverTimeline {
  /** ISO day (yyyy-MM-dd) on the time axis for an original label. */
  toAxisTime: (original: string) => string;
  /** Original label for an axis tick, or the month as `M月` between labels. */
  formatLabel: (axisValue: string | number) => string;
}

/** Calendar day for a label that names a date or month, e.g. `2024-01`, `2024年1月`. */
export function parseRiverDay(label: string): LocalDate | null {
  const trimmed = label.trim();
  try {
    const chinese = /^(\d{4})\s*年\s*(\d{1,2})\s*月$/.exec(trimmed);
    if (chinese !== null) {
      return YearMonth.of(Number(chinese[1]), Number(chinese[2])).atDay(1);
    }
    if (/^\d{4}-\d{2}$/.test(trimmed)) {
      return YearMonth.parse(trimmed).atDay(1);
    }
    const isoDay = /^(\d{4}-\d{2}-\d{2})(?:[T ].*)?$/.exec(trimmed);
    if (isoDay !== null) {
      return LocalDate.parse(isoDay[1] ?? '');
    }
    if (/^\d{4}\/\d{1,2}\/\d{1,2}$/.test(trimmed)) {
      return LocalDate.parse(trimmed, SLASH_DATE);
    }
  } catch (error) {
    if (error instanceof DateTimeParseException || error instanceof DateTimeException) {
      return null;
    }
    throw error;
  }
  return null;
}

/** Month number for a label without a year, e.g. `1月` or `Jan`. */
export function parseMonthFromLabel(label: string): number | null {
  const trimmed = label.trim();
  const bare = /^(\d{1,2})\s*月$/.exec(trimmed);
  if (bare !== null) {
    const month = Number(bare[1]);
    return month >= 1 && month <= 12 ? month : null;
  }
  const named = /^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*$/i.exec(trimmed);
  if (named !== null) {
    return MONTH_NAMES[(named[1] ?? '').toLowerCase()] ?? null;
  }
  return null;
}

/**
 * ThemeRiver lays layers out on a time axis. Map category labels (1月…) to
 * real days and keep a reverse lookup for the axis ticks.
 */
export function buildRiverTimeline(
  times: string[],
  zone: ZoneId = ZoneId.SYSTEM,
): RiverTimeline {
  const parsed = times.map(time => ({ time, day: parseRiverDay(time) }));
  const firstDay = parsed.find(entry => entry.day !== null)?.day ?? null;
  const yearHint = firstDay === null ? DEFAULT_YEAR : firstDay.year();

  const originalToAxis = new Map<string, LocalDate>();
  const axisToOriginal = new Map<string, string>();
  parsed.forEach(({ time, day }, index) => {
    const month = parseMonthFromLabel(time);
    let axisDay: LocalDate;
    if (day !== null) {
      axisDay = day;
    } else if (month !== null) {
      axisDay = LocalDate.of(yearHint, month, 1);
    } else {
      axisDay = LocalDate.of(yearHint, 1, 1).plusMonths(index);
    }
    originalToAxis.set(time, axisDay);
    axisToOriginal.set(axisDay.toString(), time);
  });

  const getDayOfAxisValue = (axisValue: string | number): LocalDate | null => {
    if (typeof axisValue === 'string') {
      return parseRiverDay(axisValue);
    }
    if (!Number.isFinite(axisValue)) {
      return null;
    }
    return LocalDate.ofInstant(Instant.ofEpochMilli(axisValue), zone);
  };

  return {
    toAxisTime: (original) => {
      const day = originalToAxis.get(original) ?? parseRiverDay(original);
      return day === null ? original : day.toString();
    },
    formatLabel: (axisValue) => {
      const day = getDayOfAxisValue(axisValue);
      if (day === null) {
        return String(axisValue);
      }
      return axisToOriginal.get(day.toString()) ?? `${String(day.monthValue())}月`;
    },
  };
}
