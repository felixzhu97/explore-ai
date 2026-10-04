import { Pipe, type PipeTransform } from '@angular/core';
import type { Instant } from '@js-joda/core';

import { DATE_TIME, formatInstant, TIME } from './instant-format';

export type InstantPattern = 'dateTime' | 'time';

const FORMATTERS = { dateTime: DATE_TIME, time: TIME } as const;

@Pipe({ name: 'instant' })
export class InstantPipe implements PipeTransform {
  transform(instant: Instant, pattern: InstantPattern = 'dateTime'): string {
    return formatInstant(instant, FORMATTERS[pattern]);
  }
}
