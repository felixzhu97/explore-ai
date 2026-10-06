import { Instant } from '@js-joda/core';

/** The only bridge to `Date`, for third-party APIs that need it (the ng-zorro picker). */
export function toNativeDate(instant: Instant): Date {
  return new Date(instant.toEpochMilli());
}

/** Converts a JavaScript Date to an Instant. */
export function fromNativeDate(date: Date): Instant {
  return Instant.ofEpochMilli(date.getTime());
}

/** IANA name of the browser time zone, e.g. `Asia/Shanghai`. */
export function getSystemZoneName(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}
