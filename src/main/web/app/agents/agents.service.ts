import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/services/api.constants';
import { parseSseToken, streamSsePost } from '@core/streaming/sse-client';
import type { AgentHealth, AgentInfo, AgentInvokeRequest } from './agents.model';

@Injectable({ providedIn: 'root' })
export class AgentsService {
  private readonly http = inject(HttpClient);

  listAgents(): Observable<AgentInfo[]> {
    return this.http.get<AgentInfo[]>(`${API_BASE_URL}/agents/list`);
  }

  getHealth(agentType: string): Observable<AgentHealth> {
    return this.http.get<AgentHealth>(`${API_BASE_URL}/agents/${agentType}/health`);
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
        ? `${API_BASE_URL}/agents/supervisor/invoke/sse`
        : `${API_BASE_URL}/agents/${agentType}/invoke/sse`;

    return streamSsePost(path, request, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          onDone();
          return true;
        }
        if (eventType === 'error') {
          onError(new Error(data || 'Agent stream error'));
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
