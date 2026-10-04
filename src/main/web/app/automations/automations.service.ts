import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { map, type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

export type ScheduleKind = 'CRON' | 'ONCE';

export interface AutomationSchedule {
  id: string;
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression: string | null;
  runAt: Instant | null;
  timezone: string;
  enabled: boolean;
  actionType: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
  nextRunAt: Instant;
  lastRunAt: Instant | null;
  createdAt: Instant;
  updatedAt: Instant;
}

/** Schedule as sent by the API, with ISO-8601 instants. */
export type AutomationScheduleDto = Omit<
  AutomationSchedule,
  'runAt' | 'nextRunAt' | 'lastRunAt' | 'createdAt' | 'updatedAt'
> & {
  runAt: string | null;
  nextRunAt: string;
  lastRunAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export interface AutomationRun {
  id: string;
  scheduleId: string;
  startedAt: Instant;
  finishedAt: Instant | null;
  status: string;
  errorMessage: string | null;
  resultExcerpt: string | null;
  emailStatus: string;
}

/** Run as sent by the API, with ISO-8601 instants. */
export type AutomationRunDto = Omit<AutomationRun, 'startedAt' | 'finishedAt'> & {
  startedAt: string;
  finishedAt: string | null;
};

export interface AutomationScheduleWriteRequest {
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression?: string | null;
  /** ISO-8601 instant. */
  runAt?: string | null;
  timezone: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
}

function parseOptionalInstant(value: string | null): Instant | null {
  return value === null ? null : Instant.parse(value);
}

export function toAutomationSchedule(dto: AutomationScheduleDto): AutomationSchedule {
  return {
    ...dto,
    runAt: parseOptionalInstant(dto.runAt),
    nextRunAt: Instant.parse(dto.nextRunAt),
    lastRunAt: parseOptionalInstant(dto.lastRunAt),
    createdAt: Instant.parse(dto.createdAt),
    updatedAt: Instant.parse(dto.updatedAt),
  };
}

export function toAutomationRun(dto: AutomationRunDto): AutomationRun {
  return {
    ...dto,
    startedAt: Instant.parse(dto.startedAt),
    finishedAt: parseOptionalInstant(dto.finishedAt),
  };
}

@Service()
export class AutomationsService {
  readonly #http = inject(HttpClient);
  readonly #base = `${API_BASE_URL}/automations/schedules`;

  list(): Observable<AutomationSchedule[]> {
    return this.#http
      .get<AutomationScheduleDto[]>(this.#base)
      .pipe(map(schedules => schedules.map(toAutomationSchedule)));
  }

  create(request: AutomationScheduleWriteRequest): Observable<AutomationSchedule> {
    return this.#http
      .post<AutomationScheduleDto>(this.#base, request)
      .pipe(map(toAutomationSchedule));
  }

  update(
    id: string,
    request: AutomationScheduleWriteRequest,
  ): Observable<AutomationSchedule> {
    return this.#http
      .put<AutomationScheduleDto>(`${this.#base}/${id}`, request)
      .pipe(map(toAutomationSchedule));
  }

  setEnabled(id: string, enabled: boolean): Observable<AutomationSchedule> {
    return this.#http
      .patch<AutomationScheduleDto>(`${this.#base}/${id}/enabled`, { enabled })
      .pipe(map(toAutomationSchedule));
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }

  listRuns(id: string, limit = 20): Observable<AutomationRun[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.#http
      .get<AutomationRunDto[]>(`${this.#base}/${id}/runs`, { params })
      .pipe(map(runs => runs.map(toAutomationRun)));
  }
}
