import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { I18nService } from '../core/i18n';
import { parseSseToken, streamSsePost } from '../core/streaming/sse-client';
import type {
  AgentHealth,
  AgentInfo,
  AgentInvokeRequest,
  SavedWorkflowTemplate,
  WorkflowTemplate,
  WorkflowTemplateWriteRequest,
} from './pipelines.model';
import type { PipelineInvokeRequest } from './pipelines.model.graph';

@Injectable({ providedIn: 'root' })
export class PipelinesService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly templatesBase = `${API_BASE_URL}/pipelines/templates`;

  listAgents(): Observable<AgentInfo[]> {
    return this.http.get<AgentInfo[]>(`${API_BASE_URL}/pipelines/list`);
  }

  getHealth(agentType: string): Observable<AgentHealth> {
    return this.http.get<AgentHealth>(`${API_BASE_URL}/pipelines/${agentType}/health`);
  }

  listTemplates(): Observable<WorkflowTemplate[]> {
    return this.http.get<WorkflowTemplate[]>(this.templatesBase, {
      params: this.langParams(),
    });
  }

  listLibrary(): Observable<SavedWorkflowTemplate[]> {
    return this.http.get<SavedWorkflowTemplate[]>(`${this.templatesBase}/library`);
  }

  createFromTemplate(templateId: string): Observable<SavedWorkflowTemplate> {
    return this.http.post<SavedWorkflowTemplate>(
      `${this.templatesBase}/from-template`,
      { templateId },
      { params: this.langParams() },
    );
  }

  createLibraryTemplate(
    request: WorkflowTemplateWriteRequest,
  ): Observable<SavedWorkflowTemplate> {
    return this.http.post<SavedWorkflowTemplate>(
      `${this.templatesBase}/library`,
      request,
    );
  }

  updateLibraryTemplate(
    id: string,
    request: WorkflowTemplateWriteRequest,
  ): Observable<SavedWorkflowTemplate> {
    return this.http.put<SavedWorkflowTemplate>(
      `${this.templatesBase}/library/${id}`,
      request,
    );
  }

  setLibraryTemplateEnabled(
    id: string,
    enabled: boolean,
  ): Observable<SavedWorkflowTemplate> {
    return this.http.patch<SavedWorkflowTemplate>(
      `${this.templatesBase}/library/${id}/enabled`,
      { enabled },
    );
  }

  deleteLibraryTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.templatesBase}/library/${id}`);
  }

  invokeStream(
    agentType: string,
    request: AgentInvokeRequest,
    onChunk: (token: string) => void,
    onHandoff: (payload: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
  ): { abort: () => void } {
    const path =
      agentType === 'supervisor'
        ? `${API_BASE_URL}/pipelines/supervisor/invoke/sse`
        : `${API_BASE_URL}/pipelines/${agentType}/invoke/sse`;

    return this.openSse(path, request, onChunk, onHandoff, onDone, onError);
  }

  invokePipelineStream(
    request: PipelineInvokeRequest,
    onChunk: (token: string) => void,
    onHandoff: (payload: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
  ): { abort: () => void } {
    return this.openSse(
      `${API_BASE_URL}/pipelines/invoke/sse`,
      request,
      onChunk,
      onHandoff,
      onDone,
      onError,
    );
  }

  private langParams(): HttpParams {
    return new HttpParams().set('lang', this.i18n.language());
  }

  private openSse(
    path: string,
    body: unknown,
    onChunk: (token: string) => void,
    onHandoff: (payload: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
  ): { abort: () => void } {
    return streamSsePost(path, body, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          onDone();
          return true;
        }
        if (eventType === 'error') {
          onError(new Error(data || 'Pipeline stream error'));
          return true;
        }
        if (eventType === 'agent_handoff') {
          onHandoff(data);
          return false;
        }
        if (eventType === 'message' || !eventType) {
          const token = parseSseToken(data);
          if (token !== null) {
            onChunk(token);
          }
        }
        return false;
      },
      onDone,
      onError,
    });
  }
}
