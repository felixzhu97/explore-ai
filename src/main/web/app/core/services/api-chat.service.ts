import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import type {
  ChatMessageData,
  ChatStreamRequest,
  ModelInfo,
  ProviderInfo,
  SessionInfo,
} from '@shared/models';
import {
  parseChatStreamEvent,
  streamSsePost,
  type ChatStreamEvent,
} from '@core/streaming/sse-client';
import { API_BASE_URL, DEFAULT_MODELS, DEFAULT_PROVIDERS } from './api.constants';

export type ChatStreamEventHandler = (event: ChatStreamEvent) => void;

@Injectable({ providedIn: 'root' })
export class ApiChatService {
  private http = inject(HttpClient);

  getProviders(): Observable<ProviderInfo[]> {
    return this.http
      .get<ProviderInfo[]>(`${API_BASE_URL}/text/providers`)
      .pipe(catchError(() => of(DEFAULT_PROVIDERS)));
  }

  getModels(provider: string): Observable<ModelInfo[]> {
    return this.http
      .get<{ provider: string; models: ModelInfo[]; count: number }>(`${API_BASE_URL}/text/models`, {
        params: { provider },
      })
      .pipe(
        map(res => res.models ?? []),
        catchError(() => of(DEFAULT_MODELS[provider] ?? DEFAULT_MODELS['openai'] ?? [])),
      );
  }

  createSession(title?: string): Observable<SessionInfo> {
    return this.http.post<SessionInfo>(`${API_BASE_URL}/sessions`, title ? { title } : {});
  }

  getSessions(): Observable<SessionInfo[]> {
    return this.http.get<SessionInfo[]>(`${API_BASE_URL}/sessions`);
  }

  getSessionMessages(sessionId: string): Observable<ChatMessageData[]> {
    return this.http.get<ChatMessageData[]>(`${API_BASE_URL}/sessions/${sessionId}/messages`).pipe(
      map(messages => messages.map(msg => ({
        ...msg,
        timestamp: new Date(msg.timestamp as unknown as string).getTime(),
      }))),
    );
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/sessions/${sessionId}`);
  }

  chatStream(
    request: ChatStreamRequest,
    onChunk: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void,
    onEvent?: ChatStreamEventHandler,
  ): { abort: () => void } {
    let finished = false;
    const finish = () => {
      if (!finished) {
        finished = true;
        onDone();
      }
    };

    return streamSsePost(`${API_BASE_URL}/text/chat/stream`, request, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          finish();
          return true;
        }

        if (eventType === 'error') {
          let msg = 'Stream error';
          try {
            const parsed = JSON.parse(data);
            msg = parsed.error ?? parsed.message ?? msg;
          } catch {
            if (data) {
              msg = data;
            }
          }
          onError(new Error(msg));
          return true;
        }

        const event = parseChatStreamEvent(data);
        if (event === null) {
          return false;
        }
        if (event.type === 'message') {
          onChunk(event.token);
        }
        onEvent?.(event);
        return false;
      },
      onDone: finish,
      onError,
    });
  }
}
