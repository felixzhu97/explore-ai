import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AgentInfoResponse } from '../pipelines/pipelines.service';
import { I18nService } from '../i18n';

export interface CustomAgentResponse {
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

export interface CustomAgent {
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

/** Maps an API response to a custom agent. */
export function toCustomAgent(response: CustomAgentResponse): CustomAgent {
  return {
    ...response,
    createdAt: Instant.parse(response.createdAt),
    updatedAt: Instant.parse(response.updatedAt),
  };
}

export interface CreateCustomAgentRequest {
  typeKey: string;
  name: string;
  description?: string;
  systemPrompt: string;
  toolKeys?: string[];
}

export interface UpdateCustomAgentRequest {
  name: string;
  description?: string;
  systemPrompt: string;
  toolKeys?: string[];
}

@Service()
export class AgentsService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #customAgentsBase = `${API_BASE_URL}/pipelines/agents`;

  /** Merged builtins + enabled library (for display of effective catalog). */
  listCatalog(): Observable<AgentInfoResponse[]> {
    return this.#http.get<AgentInfoResponse[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: new HttpParams().set('lang', this.#i18n.language()),
    });
  }

  /** Lists the custom agents. */
  listCustomAgents(): Observable<CustomAgent[]> {
    return this.#http
      .get<CustomAgentResponse[]>(this.#customAgentsBase)
      .pipe(map(agents => agents.map(toCustomAgent)));
  }

  /** Creates a custom agent. */
  create(request: CreateCustomAgentRequest): Observable<CustomAgent> {
    return this.#http
      .post<CustomAgentResponse>(this.#customAgentsBase, request)
      .pipe(map(toCustomAgent));
  }

  /** Updates a custom agent. */
  update(id: string, request: UpdateCustomAgentRequest): Observable<CustomAgent> {
    return this.#http
      .put<CustomAgentResponse>(`${this.#customAgentsBase}/${id}`, request)
      .pipe(map(toCustomAgent));
  }

  /** Turns a custom agent on or off. */
  setEnabled(id: string, enabled: boolean): Observable<CustomAgent> {
    return this.#http
      .patch<CustomAgentResponse>(`${this.#customAgentsBase}/${id}/enabled`, { enabled })
      .pipe(map(toCustomAgent));
  }

  /** Deletes a custom agent. */
  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#customAgentsBase}/${id}`);
  }
}
