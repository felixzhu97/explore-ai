import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { map, type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

export type ScheduleKind = 'CRON' | 'ONCE';
export type AutomationActionType = 'RUN_PIPELINE_TEMPLATE';
export type RunStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';
export type EmailDeliveryStatus = 'PENDING' | 'SENT' | 'SKIPPED' | 'FAILED';

export interface AutomationScheduleResponse {
  id: string;
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression: string | null;
  runAt: string | null;
  timezone: string;
  enabled: boolean;
  actionType: AutomationActionType;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
  nextRunAt: string;
  lastRunAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AutomationSchedule {
  id: string;
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression: string | null;
  runAt: Instant | null;
  timezone: string;
  enabled: boolean;
  actionType: AutomationActionType;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
  nextRunAt: Instant;
  lastRunAt: Instant | null;
  createdAt: Instant;
  updatedAt: Instant;
}

export interface AutomationRunResponse {
  id: string;
  scheduleId: string;
  startedAt: string;
  finishedAt: string | null;
  status: RunStatus;
  errorMessage: string | null;
  resultExcerpt: string | null;
  emailStatus: EmailDeliveryStatus;
}

export interface AutomationRun {
  id: string;
  scheduleId: string;
  startedAt: Instant;
  finishedAt: Instant | null;
  status: RunStatus;
  errorMessage: string | null;
  resultExcerpt: string | null;
  emailStatus: EmailDeliveryStatus;
}

export interface CreateAutomationScheduleRequest {
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression?: string;
  /** ISO-8601 instant. */
  runAt?: string;
  timezone: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
}

export interface UpdateAutomationScheduleRequest {
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression?: string;
  /** ISO-8601 instant. */
  runAt?: string;
  timezone: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
}

function parseOptionalInstant(value: string | null): Instant | null {
  return value === null ? null : Instant.parse(value);
}

export function toAutomationSchedule(
  response: AutomationScheduleResponse,
): AutomationSchedule {
  return {
    ...response,
    runAt: parseOptionalInstant(response.runAt),
    nextRunAt: Instant.parse(response.nextRunAt),
    lastRunAt: parseOptionalInstant(response.lastRunAt),
    createdAt: Instant.parse(response.createdAt),
    updatedAt: Instant.parse(response.updatedAt),
  };
}

export function toAutomationRun(response: AutomationRunResponse): AutomationRun {
  return {
    ...response,
    startedAt: Instant.parse(response.startedAt),
    finishedAt: parseOptionalInstant(response.finishedAt),
  };
}

@Service()
export class AutomationsService {
  readonly #http = inject(HttpClient);
  readonly #base = `${API_BASE_URL}/automations/schedules`;

  list(): Observable<AutomationSchedule[]> {
    return this.#http
      .get<AutomationScheduleResponse[]>(this.#base)
      .pipe(map(schedules => schedules.map(toAutomationSchedule)));
  }

  listRuns(id: string, limit = 20): Observable<AutomationRun[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.#http
      .get<AutomationRunResponse[]>(`${this.#base}/${id}/runs`, { params })
      .pipe(map(runs => runs.map(toAutomationRun)));
  }

  create(request: CreateAutomationScheduleRequest): Observable<AutomationSchedule> {
    return this.#http
      .post<AutomationScheduleResponse>(this.#base, request)
      .pipe(map(toAutomationSchedule));
  }

  update(
    id: string,
    request: UpdateAutomationScheduleRequest,
  ): Observable<AutomationSchedule> {
    return this.#http
      .put<AutomationScheduleResponse>(`${this.#base}/${id}`, request)
      .pipe(map(toAutomationSchedule));
  }

  setEnabled(id: string, enabled: boolean): Observable<AutomationSchedule> {
    return this.#http
      .patch<AutomationScheduleResponse>(`${this.#base}/${id}/enabled`, { enabled })
      .pipe(map(toAutomationSchedule));
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }
}
