import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import {
  parseChatStreamEvent,
  streamSsePost,
  type ChatStreamEvent,
} from '../core/streaming/sse-client';
import { DEFAULT_MODELS, DEFAULT_PROVIDERS } from './chat.constants';
import type {
  ChatMessage,
  ChatStreamRequest,
  ProviderInfo,
  ModelInfo,
  SessionInfo,
  ChatMessageData,
} from './chat.model';

type ChatStreamEventHandler = (event: ChatStreamEvent) => void;

export interface UiToolStep {
  name: string;
  label: string;
  status: 'running' | 'success' | 'error';
}

export interface UiWebSource {
  title: string;
  url: string;
  snippet: string;
}

export interface UiMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  toolSteps?: UiToolStep[];
  sources?: UiWebSource[];
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  readonly providers = signal<ProviderInfo[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly selectedProvider = signal('openai');
  readonly selectedModel = signal('deepseek-v4-flash');
  readonly isLoadingModels = signal(false);

  readonly sessions = signal<SessionInfo[]>([]);
  readonly activeSessionId = signal<string | null>(null);
  readonly messages = signal<UiMessage[]>([]);
  readonly isLoading = signal(false);
  readonly streamingMessageId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly toolsEnabled = signal(true);

  private streamAbort: (() => void) | null = null;

  loadProviders(): void {
    this.getProviders().subscribe({
      next: (data) => {
        this.providers.set(data);
        const available = data.find(p => p.status === 'available') ?? data[0];
        if (available) {
          this.selectedProvider.set(available.name);
          this.loadModels(available.name);
        }
      },
      error: () => {
        this.providers.set([
          {
            name: 'openai',
            displayName: 'DeepSeek',
            models: ['deepseek-v4-flash', 'deepseek-v4-pro'],
            status: 'available',
          },
        ]);
        this.selectedProvider.set('openai');
        this.selectedModel.set('deepseek-v4-flash');
        this.models.set([
          { name: 'deepseek-v4-flash', provider: 'openai' },
          { name: 'deepseek-v4-pro', provider: 'openai' },
        ]);
      },
    });
  }

  loadModels(provider: string): void {
    this.isLoadingModels.set(true);
    this.getModels(provider).subscribe({
      next: (data) => {
        this.models.set(data);
        if (data.length > 0) {
          const defaultModel =
            data.find(m => m.name.includes('mini') || m.name.includes('flash')) || data[0];
          this.selectedModel.set(defaultModel.name);
        }
      },
      error: () => {
        this.models.set([{ name: 'deepseek-v4-flash', provider }]);
        this.selectedModel.set('deepseek-v4-flash');
      },
      complete: () => this.isLoadingModels.set(false),
    });
  }

  setProvider(provider: string): void {
    const info = this.providers().find(p => p.name === provider);
    if (info?.status === 'unavailable') {
      this.error.set(
        `${info.displayName} is not configured. Configure the API key or enable the provider before chatting.`,
      );
      return;
    }
    this.error.set(null);
    this.selectedProvider.set(provider);
    this.loadModels(provider);
  }

  isSelectedProviderAvailable(): boolean {
    const provider = this.providers().find(p => p.name === this.selectedProvider());
    return provider == null || provider.status === 'available';
  }

  setModel(model: string): void {
    this.selectedModel.set(model);
  }

  setToolsEnabled(enabled: boolean): void {
    this.toolsEnabled.set(enabled);
  }

  loadSessions(): void {
    this.refreshSessions({ createIfEmpty: false });
  }

  initializeSessions(): void {
    if (this.sessionsInitialized || this.initializationInProgress) {
      return;
    }
    this.initializationInProgress = true;
    this.refreshSessions({ createIfEmpty: true, finalizeBootstrap: true });
  }

  private sessionsInitialized = false;
  private initializationInProgress = false;
  private sessionCreationInProgress = false;

  private refreshSessions(options: {
    createIfEmpty: boolean;
    finalizeBootstrap?: boolean;
  }): void {
    this.getSessions().subscribe({
      next: (sessions) => {
        const sorted = [...sessions].sort((a, b) => {
          const bTime = new Date(b.lastActivityAt).getTime();
          const aTime = new Date(a.lastActivityAt).getTime();
          return bTime - aTime;
        });
        this.sessions.set(sorted);
        const activeId = this.activeSessionId();
        if (activeId && sorted.some(s => s.sessionId === activeId)) {
          return;
        }
        if (sorted.length > 0) {
          this.selectSession(sorted[0].sessionId);
        } else if (options.createIfEmpty) {
          this.createSession();
        }
      },
      error: () => this.sessions.set([]),
      complete: () => {
        if (options.finalizeBootstrap) {
          this.sessionsInitialized = true;
          this.initializationInProgress = false;
        }
      },
    });
  }

  createSession(): void {
    if (this.sessionCreationInProgress) {
      return;
    }
    this.sessionCreationInProgress = true;
    this.createSessionRequest().subscribe({
      next: (session) => {
        this.sessions.update((list) => {
          const withoutCurrent = list.filter(s => s.sessionId !== session.sessionId);
          return [session, ...withoutCurrent];
        });
        this.selectSession(session.sessionId);
      },
      complete: () => {
        this.sessionCreationInProgress = false;
      },
      error: () => {
        this.sessionCreationInProgress = false;
      },
    });
  }

  selectSession(sessionId: string): void {
    if (this.streamAbort) {
      this.streamAbort();
      this.streamAbort = null;
    }
    this.activeSessionId.set(sessionId);
    this.messages.set([]);
    this.error.set(null);
    this.getSessionMessages(sessionId).subscribe({
      next: (history) => {
        this.messages.set(history.map(msg => this.toUiMessage(msg)));
      },
      error: () => this.messages.set([]),
    });
  }

  deleteSession(sessionId: string): void {
    this.deleteSessionRequest(sessionId).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.sessionId !== sessionId));
        if (this.activeSessionId() === sessionId) {
          const remaining = this.sessions();
          if (remaining.length > 0) {
            this.selectSession(remaining[0].sessionId);
          } else {
            this.activeSessionId.set(null);
            this.messages.set([]);
          }
        }
      },
    });
  }

  private syncSessionMessages(sessionId: string): void {
    if (this.activeSessionId() !== sessionId || this.isLoading()) {
      return;
    }
    this.getSessionMessages(sessionId).subscribe({
      next: (history) => {
        if (this.activeSessionId() === sessionId && !this.isLoading()) {
          this.messages.update(previous => mergeHistoryWithUiState(
            history.map(msg => this.toUiMessage(msg)),
            previous,
          ),
          );
        }
      },
    });
  }

  sendMessage(content: string, options?: { streamContent?: string }): void {
    const sessionId = this.activeSessionId();
    const displayContent = content.trim();
    if (!sessionId || !displayContent || this.isLoading()) {
      return;
    }

    if (!this.isSelectedProviderAvailable()) {
      const provider = this.providers().find(p => p.name === this.selectedProvider());
      this.error.set(
        `${provider?.displayName ?? this.selectedProvider()} is not configured. Configure the API key or enable the provider before chatting.`,
      );
      return;
    }

    if (this.streamAbort) {
      this.streamAbort();
    }

    const streamContent = (options?.streamContent ?? displayContent).trim();

    const userMsg: UiMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: displayContent,
      timestamp: Date.now(),
    };
    const assistantId = `assistant_${Date.now()}`;

    this.messages.update(msgs => [
      ...msgs,
      userMsg,
      { id: assistantId, role: 'assistant', content: '', timestamp: Date.now() },
    ]);
    this.isLoading.set(true);
    this.streamingMessageId.set(assistantId);
    this.error.set(null);

    let fullContent = '';
    const streamRequest: ChatMessage[] = [{ role: 'user', content: streamContent }];

    const { abort } = this.chatStream(
      {
        messages: streamRequest,
        sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
        toolsEnabled: this.toolsEnabled(),
      },
      (chunk) => {
        fullContent += chunk;
        this.messages.update(msgs => msgs.map((msg) => {
          if (msg.id !== assistantId) {
            return msg;
          }
          return { ...msg, content: fullContent };
        }),
        );
      },
      () => {
        this.isLoading.set(false);
        this.streamingMessageId.set(null);
        this.streamAbort = null;
        this.syncSessionMessages(sessionId);
        this.loadSessions();
        setTimeout(() => {
          this.syncSessionMessages(sessionId);
          this.loadSessions();
        }, 2500);
      },
      (err) => {
        this.error.set(err.message);
        this.messages.update(msgs => msgs.map((msg) => {
          if (msg.id !== assistantId) {
            return msg;
          }
          return { ...msg, content: err.message };
        }),
        );
        this.isLoading.set(false);
        this.streamingMessageId.set(null);
        this.streamAbort = null;
      },
      (event) => {
        if (event.type === 'message') {
          return;
        }
        this.messages.update(msgs => msgs.map((msg) => {
          if (msg.id !== assistantId) {
            return msg;
          }
          if (event.type === 'tool_call') {
            const steps = [...(msg.toolSteps ?? [])];
            steps.push({
              name: event.name,
              label: toolLabel(event.name),
              status: 'running',
            });
            return { ...msg, toolSteps: steps };
          }
          if (event.type === 'tool_result') {
            const steps = (msg.toolSteps ?? []).map((step) => {
              if (step.name !== event.name || step.status !== 'running') {
                return step;
              }
              return {
                ...step,
                status: event.ok ? 'success' as const : 'error' as const,
              };
            });
            return { ...msg, toolSteps: steps };
          }
          if (event.type === 'sources') {
            return {
              ...msg,
              sources: event.items.map(item => ({
                title: item.title,
                url: item.url,
                snippet: item.snippet,
              })),
            };
          }
          return msg;
        }));
      },
    );
    this.streamAbort = abort;
  }

  abortStream(): void {
    if (this.streamAbort) {
      this.streamAbort();
      this.streamAbort = null;
      this.isLoading.set(false);
      this.streamingMessageId.set(null);
    }
  }

  private getProviders(): Observable<ProviderInfo[]> {
    return this.http
      .get<ProviderInfo[]>(`${API_BASE_URL}/text/providers`)
      .pipe(catchError(() => of(DEFAULT_PROVIDERS)));
  }

  private getModels(provider: string): Observable<ModelInfo[]> {
    return this.http
      .get<{ provider: string; models: ModelInfo[]; count: number }>(`${API_BASE_URL}/text/models`, {
        params: { provider },
      })
      .pipe(
        map(res => res.models ?? []),
        catchError(() => of(DEFAULT_MODELS[provider] ?? DEFAULT_MODELS['openai'] ?? [])),
      );
  }

  private createSessionRequest(title?: string): Observable<SessionInfo> {
    return this.http.post<SessionInfo>(`${API_BASE_URL}/sessions`, title ? { title } : {});
  }

  private getSessions(): Observable<SessionInfo[]> {
    return this.http.get<SessionInfo[]>(`${API_BASE_URL}/sessions`);
  }

  private getSessionMessages(sessionId: string): Observable<ChatMessageData[]> {
    return this.http.get<ChatMessageData[]>(`${API_BASE_URL}/sessions/${sessionId}/messages`).pipe(
      map(messages => messages.map(msg => ({
        ...msg,
        timestamp: new Date(msg.timestamp as unknown as string).getTime(),
      }))),
    );
  }

  private deleteSessionRequest(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/sessions/${sessionId}`);
  }

  private chatStream(
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

  private toUiMessage(msg: ChatMessageData): UiMessage {
    const timestamp =
      typeof msg.timestamp === 'number'
        ? msg.timestamp
        : new Date(msg.timestamp).getTime();
    return {
      id: msg.id ?? `${msg.role}_${timestamp}`,
      role: msg.role === 'assistant' ? 'assistant' : 'user',
      content: msg.content,
      timestamp,
      sources: msg.sources?.map(source => ({
        title: source.title,
        url: source.url,
        snippet: source.snippet,
      })),
    };
  }
}

/**
 * After stream complete, history sync may race DB persistence.
 * Keep local sources when API has not returned them yet for the same content.
 */
export function mergeHistoryWithUiState(
  history: UiMessage[],
  previous: UiMessage[],
): UiMessage[] {
  const previousSources = new Map<string, UiWebSource[]>();
  for (const msg of previous) {
    if (msg.role === 'assistant' && msg.sources?.length) {
      previousSources.set(msg.content, msg.sources);
    }
  }

  return history.map((ui) => {
    if (ui.role !== 'assistant' || ui.sources?.length) {
      return ui;
    }
    const local = previousSources.get(ui.content);
    return local?.length ? { ...ui, sources: local } : ui;
  });
}

function toolLabel(name: string): string {
  const key = name.toLowerCase();
  if (key.includes('searchweb') || key === 'search_web' || (key.includes('search') && key.includes('web'))) {
    return 'Searching…';
  }
  if (key.includes('fetch')) {
    return 'Fetching page…';
  }
  if (key.includes('weather') || key.includes('forecast')) {
    return 'Checking weather…';
  }
  if (key.includes('document')) {
    return 'Searching knowledge base…';
  }
  return `Calling ${name}…`;
}
