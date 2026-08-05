import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { I18nService } from '../core/i18n';
import type { Skill, SkillTemplate, SkillWriteRequest } from './skills.model';

@Injectable({ providedIn: 'root' })
export class SkillsService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly base = `${API_BASE_URL}/skills`;

  list(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.base);
  }

  listEnabled(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.base).pipe(
      map(skills => skills.filter(skill => skill.enabled)),
    );
  }

  get(id: string): Observable<Skill> {
    return this.http.get<Skill>(`${this.base}/${id}`);
  }

  listTemplates(): Observable<SkillTemplate[]> {
    return this.http.get<SkillTemplate[]>(`${this.base}/templates`, {
      params: this.langParams(),
    });
  }

  createFromTemplate(templateId: string): Observable<Skill> {
    return this.http.post<Skill>(
      `${this.base}/from-template`,
      { templateId },
      { params: this.langParams() },
    );
  }

  create(request: SkillWriteRequest): Observable<Skill> {
    return this.http.post<Skill>(this.base, request);
  }

  update(id: string, request: SkillWriteRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.base}/${id}`, request);
  }

  setEnabled(id: string, enabled: boolean): Observable<Skill> {
    return this.http.patch<Skill>(`${this.base}/${id}/enabled`, { enabled });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  private langParams(): HttpParams {
    return new HttpParams().set('lang', this.i18n.language());
  }
}
