import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AgentInfoResponse } from '../pipelines/pipelines.service';
import { I18nService } from '../i18n';

export interface SavedAgentResponse {
  id: string;
  typeKey: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SavedAgent {
  id: string;
  typeKey: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
  enabled: boolean;
  createdAt: Instant;
  updatedAt: Instant;
}

export function toSavedAgent(response: SavedAgentResponse): SavedAgent {
  return {
    ...response,
    createdAt: Instant.parse(response.createdAt),
    updatedAt: Instant.parse(response.updatedAt),
  };
}

export interface CreateSavedAgentRequest {
  typeKey: string;
  name: string;
  description?: string;
  systemPrompt: string;
  toolKeys?: string[];
}

export interface UpdateSavedAgentRequest {
  name: string;
  description?: string;
  systemPrompt: string;
  toolKeys?: string[];
}

@Service()
export class AgentsService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #savedAgentsBase = `${API_BASE_URL}/pipelines/agents`;

  /** Merged builtins + enabled library (for display of effective catalog). */
  listCatalog(): Observable<AgentInfoResponse[]> {
    return this.#http.get<AgentInfoResponse[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: new HttpParams().set('lang', this.#i18n.language()),
    });
  }

  listSavedAgents(): Observable<SavedAgent[]> {
    return this.#http
      .get<SavedAgentResponse[]>(this.#savedAgentsBase)
      .pipe(map(agents => agents.map(toSavedAgent)));
  }

  create(request: CreateSavedAgentRequest): Observable<SavedAgent> {
    return this.#http
      .post<SavedAgentResponse>(this.#savedAgentsBase, request)
      .pipe(map(toSavedAgent));
  }

  update(id: string, request: UpdateSavedAgentRequest): Observable<SavedAgent> {
    return this.#http
      .put<SavedAgentResponse>(`${this.#savedAgentsBase}/${id}`, request)
      .pipe(map(toSavedAgent));
  }

  setEnabled(id: string, enabled: boolean): Observable<SavedAgent> {
    return this.#http
      .patch<SavedAgentResponse>(`${this.#savedAgentsBase}/${id}/enabled`, { enabled })
      .pipe(map(toSavedAgent));
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#savedAgentsBase}/${id}`);
  }
}
