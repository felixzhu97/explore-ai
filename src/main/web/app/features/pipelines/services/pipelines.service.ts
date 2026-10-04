import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.constants';
import { I18nService } from '../../../core/i18n';
import { parseSseToken, streamSsePost } from '../../../core/streaming/sse-client';
import type { PipelineInvokeRequest } from '../pipeline-graph';

export interface AgentType {
  type: string;
  name: string;
  description: string;
  healthy: boolean;
  supervisor: boolean;
  runtime?: string;
  toolKeys?: string[];
  systemPrompt?: string;
}

export interface AgentHealth {
  type: string;
  healthy: boolean;
  status: string;
}

export interface AgentInvokeRequest {
  message: string;
  sessionId?: string;
  agentType?: string;
}

/** Builtin multilingual workflow template from the backend catalog. */
export interface PipelineTemplateDefinition {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  nameAliases?: string[];
}

/** Client-owned saved workflow template. */
export interface PipelineTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  sourceTemplateId?: string | null;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PipelineTemplateWriteRequest {
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
}

@Injectable({ providedIn: 'root' })
export class PipelinesService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly templatesBase = `${API_BASE_URL}/pipelines/templates`;

  listAgents(): Observable<AgentType[]> {
    return this.http.get<AgentType[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: this.langParams(),
    });
  }

  getHealth(agentType: string): Observable<AgentHealth> {
    return this.http.get<AgentHealth>(`${API_BASE_URL}/pipelines/${agentType}/health`, {
      params: this.langParams(),
    });
  }

  listTemplateDefinitions(): Observable<PipelineTemplateDefinition[]> {
    return this.http.get<PipelineTemplateDefinition[]>(`${API_BASE_URL}/pipelines/template-definitions`, {
      params: this.langParams(),
    });
  }

  listTemplates(): Observable<PipelineTemplate[]> {
    return this.http.get<PipelineTemplate[]>(this.templatesBase);
  }

  createTemplateFromDefinition(templateId: string): Observable<PipelineTemplate> {
    return this.http.post<PipelineTemplate>(
      `${this.templatesBase}/from-template`,
      { templateId },
      { params: this.langParams() },
    );
  }

  createTemplate(
    request: PipelineTemplateWriteRequest,
  ): Observable<PipelineTemplate> {
    return this.http.post<PipelineTemplate>(
      this.templatesBase,
      request,
    );
  }

  updateTemplate(
    id: string,
    request: PipelineTemplateWriteRequest,
  ): Observable<PipelineTemplate> {
    return this.http.put<PipelineTemplate>(
      `${this.templatesBase}/${id}`,
      request,
    );
  }

  setTemplateEnabled(
    id: string,
    enabled: boolean,
  ): Observable<PipelineTemplate> {
    return this.http.patch<PipelineTemplate>(
      `${this.templatesBase}/${id}/enabled`,
      { enabled },
    );
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.templatesBase}/${id}`);
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
    const separator = path.includes('?') ? '&' : '?';
    const url = `${path}${separator}lang=${encodeURIComponent(this.i18n.language())}`;
    return streamSsePost(url, body, {
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
