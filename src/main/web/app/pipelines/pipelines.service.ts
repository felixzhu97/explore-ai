import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { parseSseToken, streamSsePost } from '../core/streaming/sse-client';
import type {
  AgentHealth,
  AgentInfo,
  AgentInvokeRequest,
} from './pipelines.model';
import type { PipelineInvokeRequest } from './pipelines.model.graph';

@Injectable({ providedIn: 'root' })
export class PipelinesService {
  private readonly http = inject(HttpClient);

  listAgents(): Observable<AgentInfo[]> {
    return this.http.get<AgentInfo[]>(`${API_BASE_URL}/pipelines/list`);
  }

  getHealth(agentType: string): Observable<AgentHealth> {
    return this.http.get<AgentHealth>(`${API_BASE_URL}/pipelines/${agentType}/health`);
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
