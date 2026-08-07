import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type {
  AutomationRun,
  AutomationSchedule,
  AutomationScheduleWriteRequest,
} from './automations.model';

@Injectable({ providedIn: 'root' })
export class AutomationsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/automations/schedules`;

  list(): Observable<AutomationSchedule[]> {
    return this.http.get<AutomationSchedule[]>(this.base);
  }

  create(request: AutomationScheduleWriteRequest): Observable<AutomationSchedule> {
    return this.http.post<AutomationSchedule>(this.base, request);
  }

  update(
    id: string,
    request: AutomationScheduleWriteRequest,
  ): Observable<AutomationSchedule> {
    return this.http.put<AutomationSchedule>(`${this.base}/${id}`, request);
  }

  setEnabled(id: string, enabled: boolean): Observable<AutomationSchedule> {
    return this.http.patch<AutomationSchedule>(`${this.base}/${id}/enabled`, { enabled });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  listRuns(id: string, limit = 20): Observable<AutomationRun[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<AutomationRun[]>(`${this.base}/${id}/runs`, { params });
  }
}
