import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { I18nService } from '../i18n';
import type { HealthStatus } from '../http/health-status';
import { parseSseToken, streamSsePost } from '../http/sse-client';
import type { PipelineInvokeRequest } from './pipeline-graph';
import { textOr } from '../shared/presence';

export type AgentRuntime = 'single' | 'deep';

export interface AgentInfoResponse {
  type: string;
  name: string;
  description: string;
  healthy: boolean;
  supervisor: boolean;
  runtime: AgentRuntime;
  toolKeys: string[];
  systemPrompt: string;
}

export interface AgentHealthResponse {
  type: string;
  healthy: boolean;
  status: HealthStatus;
}

export interface AgentInvokeRequest {
  message: string;
  sessionId?: string;
  agentType?: string;
}

/** Builtin multilingual workflow template from the backend catalog. */
export interface PipelineTemplateDefinitionResponse {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  nameAliases: string[];
}

/** Client-owned saved workflow template, as sent by the backend. */
export interface PipelineTemplateResponse {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  sourceTemplateId: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PipelineTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  sourceTemplateId: string | null;
  enabled: boolean;
  createdAt: Instant;
  updatedAt: Instant;
}

export function toPipelineTemplate(response: PipelineTemplateResponse): PipelineTemplate {
  return {
    ...response,
    createdAt: Instant.parse(response.createdAt),
    updatedAt: Instant.parse(response.updatedAt),
  };
}

export interface CreatePipelineTemplateRequest {
  name: string;
  description?: string;
  agentTypes: string[];
  shortTopic?: string;
  briefPrompt: string;
}

export interface UpdatePipelineTemplateRequest {
  name: string;
  description?: string;
  agentTypes: string[];
  shortTopic?: string;
  briefPrompt: string;
}

@Service()
export class PipelinesService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #templatesBase = `${API_BASE_URL}/pipelines/templates`;

  listAgents(): Observable<AgentInfoResponse[]> {
    return this.#http.get<AgentInfoResponse[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: this.#langParams(),
    });
  }

  getHealth(agentType: string): Observable<AgentHealthResponse> {
    return this.#http.get<AgentHealthResponse>(`${API_BASE_URL}/pipelines/${agentType}/health`, {
      params: this.#langParams(),
    });
  }

  listTemplateDefinitions(): Observable<PipelineTemplateDefinitionResponse[]> {
    return this.#http.get<PipelineTemplateDefinitionResponse[]>(
      `${API_BASE_URL}/pipelines/template-definitions`,
      { params: this.#langParams() },
    );
  }

  listTemplates(): Observable<PipelineTemplate[]> {
    return this.#http
      .get<PipelineTemplateResponse[]>(this.#templatesBase)
      .pipe(map(templates => templates.map(toPipelineTemplate)));
  }

  createTemplateFromDefinition(templateId: string): Observable<PipelineTemplate> {
    return this.#http
      .post<PipelineTemplateResponse>(
        `${this.#templatesBase}/from-template`,
        { templateId },
        { params: this.#langParams() },
      )
      .pipe(map(toPipelineTemplate));
  }

  createTemplate(request: CreatePipelineTemplateRequest): Observable<PipelineTemplate> {
    return this.#http
      .post<PipelineTemplateResponse>(this.#templatesBase, request)
      .pipe(map(toPipelineTemplate));
  }

  updateTemplate(
    id: string,
    request: UpdatePipelineTemplateRequest,
  ): Observable<PipelineTemplate> {
    return this.#http
      .put<PipelineTemplateResponse>(`${this.#templatesBase}/${id}`, request)
      .pipe(map(toPipelineTemplate));
  }

  setTemplateEnabled(id: string, enabled: boolean): Observable<PipelineTemplate> {
    return this.#http
      .patch<PipelineTemplateResponse>(`${this.#templatesBase}/${id}/enabled`, { enabled })
      .pipe(map(toPipelineTemplate));
  }

  deleteTemplate(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#templatesBase}/${id}`);
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

    return this.#openSse(path, request, onChunk, onHandoff, onDone, onError);
  }

  invokePipelineStream(
    request: PipelineInvokeRequest,
    onChunk: (token: string) => void,
    onHandoff: (payload: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
  ): { abort: () => void } {
    return this.#openSse(
      `${API_BASE_URL}/pipelines/invoke/sse`,
      request,
      onChunk,
      onHandoff,
      onDone,
      onError,
    );
  }

  #langParams(): HttpParams {
    return new HttpParams().set('lang', this.#i18n.language());
  }

  #openSse(
    path: string,
    body: unknown,
    onChunk: (token: string) => void,
    onHandoff: (payload: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
  ): { abort: () => void } {
    const separator = path.includes('?') ? '&' : '?';
    const url = `${path}${separator}lang=${encodeURIComponent(this.#i18n.language())}`;
    return streamSsePost(url, body, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          onDone();
          return true;
        }
        if (eventType === 'error') {
          onError(new Error(textOr(data, 'Pipeline stream error')));
          return true;
        }
        if (eventType === 'agent_handoff') {
          onHandoff(data);
          return false;
        }
        if (eventType === 'message' || eventType === '') {
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
