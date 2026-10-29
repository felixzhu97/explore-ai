import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AgentInfoResponse } from '../pipelines/pipelines.service';
import { I18nService } from '../i18n';

export interface CustomAgentResponse {
  id: string;
  agentType: string;
  name: string;
  description: string;
  systemPrompt: string;
  tools: string[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CustomAgent {
  id: string;
  agentType: string;
  name: string;
  description: string;
  systemPrompt: string;
  tools: string[];
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
  agentType: string;
  name: string;
  description?: string;
  systemPrompt: string;
  tools?: string[];
}

export interface UpdateCustomAgentRequest {
  name: string;
  description?: string;
  systemPrompt: string;
  tools?: string[];
}

/** Published Language summary from `/api/agents` (no system prompt). */
export interface AgentSummaryResponse {
  type: string;
  name: string;
  description: string;
  healthy: boolean;
  tools: string[];
}

export interface AgentInvokeResponse {
  type: string;
  message: string;
  reply: string;
}

export interface AgentCardWire {
  name: string;
  description?: string;
  version?: string;
  url?: string;
  skills?: { id: string; name: string; description?: string }[];
}

export interface A2aTaskWire {
  id: string;
  status: { state: string };
  artifacts?: {
    artifactId: string;
    name: string;
    parts: { text?: string }[];
  }[];
}

@Service()
export class AgentsService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #customAgentsBase = `${API_BASE_URL}/pipelines/agents`;
  readonly #publishedBase = `${API_BASE_URL}/agents`;

  /** Merged builtins + enabled library (for display of effective catalog). */
  listCatalog(): Observable<AgentInfoResponse[]> {
    return this.#http.get<AgentInfoResponse[]>(`${API_BASE_URL}/pipelines/agent-types`, {
      params: new HttpParams().set('lang', this.#i18n.language()),
    });
  }

  /** Sibling Published Language list (opaque; no system prompt). */
  listPublishedAgents(): Observable<AgentSummaryResponse[]> {
    return this.#http.get<AgentSummaryResponse[]>(this.#publishedBase, {
      params: new HttpParams().set('lang', this.#i18n.language()),
    });
  }

  /** Runs one catalog Agent via Published Language. */
  invokePublished(agentType: string, message: string): Observable<AgentInvokeResponse> {
    return this.#http.post<AgentInvokeResponse>(
      `${this.#publishedBase}/${encodeURIComponent(agentType)}/invoke`,
      { message },
      { params: new HttpParams().set('lang', this.#i18n.language()) },
    );
  }

  /** Fetches the local A2A Agent Card. */
  getAgentCard(): Observable<AgentCardWire> {
    return this.#http.get<AgentCardWire>('/.well-known/agent-card.json');
  }

  /** Sends one A2A Task to the local A2A Server. */
  sendA2aMessage(text: string, skill = 'research'): Observable<A2aTaskWire> {
    return this.#http.post<A2aTaskWire>('/a2a/v1/message:send', {
      message: {
        messageId: crypto.randomUUID(),
        role: 'user',
        parts: [{ text }],
        metadata: { skill },
      },
    });
  }

  /** Lists the custom agents. */
  listCustomAgents(): Observable<CustomAgent[]> {
    return this.#http
      .get<CustomAgentResponse[]>(this.#customAgentsBase)
      .pipe(map(agents => agents.map(toCustomAgent)));
  }

  /** Creates a custom agent. */
  createAgent(request: CreateCustomAgentRequest): Observable<CustomAgent> {
    return this.#http
      .post<CustomAgentResponse>(this.#customAgentsBase, request)
      .pipe(map(toCustomAgent));
  }

  /** Updates a custom agent. */
  updateAgent(id: string, request: UpdateCustomAgentRequest): Observable<CustomAgent> {
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
  deleteAgent(id: string): Observable<void> {
    return this.#http.delete<void>(`${this.#customAgentsBase}/${id}`);
  }
}
