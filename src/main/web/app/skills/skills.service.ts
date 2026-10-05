import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { I18nService } from '../i18n';

export interface SkillResponse {
  id: string;
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Skill {
  id: string;
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
  enabled: boolean;
  createdAt: Instant;
  updatedAt: Instant;
}

export function toSkill(response: SkillResponse): Skill {
  return {
    ...response,
    createdAt: Instant.parse(response.createdAt),
    updatedAt: Instant.parse(response.updatedAt),
  };
}

export interface SkillTemplateResponse {
  id: string;
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
  nameAliases: string[];
}

export interface CreateSkillRequest {
  name: string;
  description?: string;
  instructions: string;
  allowedTools?: string[];
}

export interface UpdateSkillRequest {
  name: string;
  description?: string;
  instructions: string;
  allowedTools?: string[];
}

@Service()
export class SkillsService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #base = `${API_BASE_URL}/skills`;

  list(): Observable<Skill[]> {
    return this.#http
      .get<SkillResponse[]>(this.#base)
      .pipe(map(skills => skills.map(toSkill)));
  }

  listEnabled(): Observable<Skill[]> {
    return this.list().pipe(map(skills => skills.filter(skill => skill.enabled)));
  }

  listTemplates(): Observable<SkillTemplateResponse[]> {
    return this.#http.get<SkillTemplateResponse[]>(`${this.#base}/templates`, {
      params: this.#langParams(),
    });
  }

  get(id: string): Observable<Skill> {
    return this.#http.get<SkillResponse>(`${this.#base}/${id}`).pipe(map(toSkill));
  }

  create(request: CreateSkillRequest): Observable<Skill> {
    return this.#http.post<SkillResponse>(this.#base, request).pipe(map(toSkill));
  }

  createFromTemplate(templateId: string): Observable<Skill> {
    return this.#http
      .post<SkillResponse>(
        `${this.#base}/from-template`,
        { templateId },
        { params: this.#langParams() },
      )
      .pipe(map(toSkill));
  }

  update(id: string, request: UpdateSkillRequest): Observable<Skill> {
    return this.#http.put<SkillResponse>(`${this.#base}/${id}`, request).pipe(map(toSkill));
  }

  setEnabled(id: string, enabled: boolean): Observable<Skill> {
    return this.#http
      .patch<SkillResponse>(`${this.#base}/${id}/enabled`, { enabled })
      .pipe(map(toSkill));
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#base}/${id}`);
  }

  #langParams(): HttpParams {
    return new HttpParams().set('lang', this.#i18n.language());
  }
}
