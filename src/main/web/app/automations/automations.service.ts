import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

export type ScheduleKind = 'CRON' | 'ONCE';

export interface AutomationSchedule {
  id: string;
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression: string | null;
  runAt: string | null;
  timezone: string;
  enabled: boolean;
  actionType: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
  nextRunAt: string;
  lastRunAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AutomationRun {
  id: string;
  scheduleId: string;
  startedAt: string;
  finishedAt: string | null;
  status: string;
  errorMessage: string | null;
  resultExcerpt: string | null;
  emailStatus: string;
}

export interface AutomationScheduleWriteRequest {
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression?: string | null;
  runAt?: string | null;
  timezone: string;
  pipelineTemplateId: string;
  recipientEmail: string;
  brief: string;
}

@Injectable({ providedIn: 'root' })
export class AutomationsService {
  readonly #http = inject(HttpClient);
  readonly #base = `${API_BASE_URL}/automations/schedules`;

  list(): Observable<AutomationSchedule[]> {
    return this.#http.get<AutomationSchedule[]>(this.#base);
  }

  create(request: AutomationScheduleWriteRequest): Observable<AutomationSchedule> {
    return this.#http.post<AutomationSchedule>(this.#base, request);
  }

  update(
    id: string,
    request: AutomationScheduleWriteRequest,
  ): Observable<AutomationSchedule> {
    return this.#http.put<AutomationSchedule>(`${this.#base}/${id}`, request);
  }

  setEnabled(id: string, enabled: boolean): Observable<AutomationSchedule> {
    return this.#http.patch<AutomationSchedule>(`${this.#base}/${id}/enabled`, { enabled });
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }

  listRuns(id: string, limit = 20): Observable<AutomationRun[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.#http.get<AutomationRun[]>(`${this.#base}/${id}/runs`, { params });
  }
}
