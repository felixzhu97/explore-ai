import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { parseSseToken, streamSsePost } from '../core/streaming/sse-client';
import type { AgentInvokeRequest, AgentSessionInfo } from './agents.model';

@Injectable({ providedIn: 'root' })
export class AgentsService {
  private readonly http = inject(HttpClient);

  createSession(title?: string): Observable<AgentSessionInfo> {
    return this.http.post<AgentSessionInfo>(`${API_BASE_URL}/agents/sessions`, { title });
  }

  listSessions(): Observable<AgentSessionInfo[]> {
    return this.http.get<AgentSessionInfo[]>(`${API_BASE_URL}/agents/sessions`);
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/agents/sessions/${sessionId}`);
  }

  invokeStream(
    sessionId: string,
    request: AgentInvokeRequest,
    handlers: {
      onPlan?: (data: string) => void;
      onReplan?: (data: string) => void;
      onThought?: (data: string) => void;
      onTool?: (data: string) => void;
      onEvaluation?: (data: string) => void;
      onChunk: (token: string) => void;
      onDone: () => void;
      onError: (error: Error) => void;
    },
  ): { abort: () => void } {
    return streamSsePost(`${API_BASE_URL}/agents/sessions/${sessionId}/invoke/sse`, request, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          handlers.onDone();
          return true;
        }
        if (eventType === 'error') {
          handlers.onError(new Error(data || 'Agent stream error'));
          return true;
        }
        if (eventType === 'plan') {
          handlers.onPlan?.(data);
          return false;
        }
        if (eventType === 'replan') {
          handlers.onReplan?.(data);
          return false;
        }
        if (eventType === 'thought') {
          handlers.onThought?.(data);
          return false;
        }
        if (eventType === 'tool') {
          handlers.onTool?.(data);
          return false;
        }
        if (eventType === 'evaluation') {
          handlers.onEvaluation?.(data);
          return false;
        }
        if (eventType === 'message' || !eventType) {
          const token = parseSseToken(data);
          if (token !== null) {
            handlers.onChunk(token);
          }
        }
        return false;
      },
      onDone: handlers.onDone,
      onError: handlers.onError,
    });
  }
}
