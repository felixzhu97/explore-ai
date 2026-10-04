import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AgentType } from '../pipelines/pipelines.service';
import { I18nService } from '../i18n';

export interface SavedAgent {
  id: string;
  typeKey: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SavedAgentWriteRequest {
  typeKey?: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
}

@Injectable({ providedIn: 'root' })
export class AgentsService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #savedAgentsBase = `${API_BASE_URL}/pipelines/agents`;

  /** Merged builtins + enabled library (for display of effective catalog). */
  listCatalog(): Observable<AgentType[]> {
    return this.#http.get<AgentType[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: new HttpParams().set('lang', this.#i18n.language()),
    });
  }

  listSavedAgents(): Observable<SavedAgent[]> {
    return this.#http.get<SavedAgent[]>(this.#savedAgentsBase);
  }

  create(request: SavedAgentWriteRequest & { typeKey: string }): Observable<SavedAgent> {
    return this.#http.post<SavedAgent>(this.#savedAgentsBase, request);
  }

  update(id: string, request: SavedAgentWriteRequest): Observable<SavedAgent> {
    return this.#http.put<SavedAgent>(`${this.#savedAgentsBase}/${id}`, {
      name: request.name,
      description: request.description,
      systemPrompt: request.systemPrompt,
      toolKeys: request.toolKeys,
    });
  }

  setEnabled(id: string, enabled: boolean): Observable<SavedAgent> {
    return this.#http.patch<SavedAgent>(`${this.#savedAgentsBase}/${id}/enabled`, { enabled });
  }

  delete(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#savedAgentsBase}/${id}`);
  }
}
