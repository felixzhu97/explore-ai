import { DateTimeFormatter, Instant, LocalDateTime, ZoneId } from '@js-joda/core';

export const DATE_TIME = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss');
export const TIME = DateTimeFormatter.ofPattern('HH:mm:ss');

/** Mirrors the backend `AutomationSchedule.ONCE_TERMINAL_NEXT` sentinel. */
export const ONCE_TERMINAL_NEXT = Instant.parse('9999-12-31T23:59:59Z');

/** Formats an instant in the zone, local time by default. */
export function formatInstant(
  instant: Instant,
  formatter: DateTimeFormatter,
  zone: ZoneId = ZoneId.SYSTEM,
): string {
  return LocalDateTime.ofInstant(instant, zone).format(formatter);
}
