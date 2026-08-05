import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type { AgentInfo } from '../pipelines/pipelines.model';
import type { SavedAgent, SavedAgentWriteRequest } from './agents.model';
import { I18nService } from '../core/i18n';
import { HttpParams } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AgentsService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly libraryBase = `${API_BASE_URL}/pipelines/agents/library`;

  /** Merged builtins + enabled library (for display of effective catalog). */
  listCatalog(): Observable<AgentInfo[]> {
    return this.http.get<AgentInfo[]>(`${API_BASE_URL}/pipelines/list`, {
      params: new HttpParams().set('lang', this.i18n.language()),
    });
  }

  listLibrary(): Observable<SavedAgent[]> {
    return this.http.get<SavedAgent[]>(this.libraryBase);
  }

  create(request: SavedAgentWriteRequest & { typeKey: string }): Observable<SavedAgent> {
    return this.http.post<SavedAgent>(this.libraryBase, request);
  }

  update(id: string, request: SavedAgentWriteRequest): Observable<SavedAgent> {
    return this.http.put<SavedAgent>(`${this.libraryBase}/${id}`, {
      name: request.name,
      description: request.description,
      systemPrompt: request.systemPrompt,
      toolKeys: request.toolKeys,
    });
  }

  setEnabled(id: string, enabled: boolean): Observable<SavedAgent> {
    return this.http.patch<SavedAgent>(`${this.libraryBase}/${id}/enabled`, { enabled });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.libraryBase}/${id}`);
  }
}
