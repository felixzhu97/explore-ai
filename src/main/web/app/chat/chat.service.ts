import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { SKIP_ERROR_NOTIFICATION } from '../core/interceptors/http-error.context';
import {
  parseChatStreamEvent,
  streamSsePost,
  type ChatStreamEvent,
} from '../core/streaming/sse-client';
import { DEFAULT_MODELS, DEFAULT_PROVIDERS } from './chat.constants';
import { stripToolCallMarkup } from '../shared/utils/tool-call-markup.filter';
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
  publishedAt?: string;
}

export interface UiMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  toolSteps?: UiToolStep[];
  sources?: UiWebSource[];
}

const ACTIVE_SESSION_STORAGE_KEY = 'explore-ai.chat.activeSessionId';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly providers = signal<ProviderInfo[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly selectedProvider = signal('openai');
  readonly selectedModel = signal('deepseek-v4-flash');
  readonly isLoadingModels = signal(false);

  readonly sessions = signal<SessionInfo[]>([]);
  readonly activeSessionId = signal<string | null>(null);
  readonly messages = signal<UiMessage[]>([]);
  readonly isLoading = signal(false);
  /** True while history for the selected session is loading. */
  readonly isLoadingSession = signal(false);
  /** True after the first sessions bootstrap has finished (success or failure). */
  readonly sessionsReady = signal(false);
  readonly streamingMessageId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly toolsEnabled = signal(true);
  readonly availableSkills = signal<{ id: string; name: string }[]>([]);
  readonly selectedSkillIds = signal<string[]>([]);

  private streamAbort: (() => void) | null = null;
  private sessionLoadGeneration = 0;

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

  setAvailableSkills(skills: { id: string; name: string }[]): void {
    this.availableSkills.set(skills);
    const enabledIds = new Set(skills.map(skill => skill.id));
    this.selectedSkillIds.update(ids => ids.filter(id => enabledIds.has(id)));
  }

  toggleSkillId(skillId: string): void {
    this.selectedSkillIds.update((ids) => {
      if (ids.includes(skillId)) {
        return ids.filter(id => id !== skillId);
      }
      return [...ids, skillId];
    });
  }

  isSkillSelected(skillId: string): boolean {
    return this.selectedSkillIds().includes(skillId);
  }

  loadSessions(): void {
    this.refreshSessions({ createIfEmpty: false });
  }

  /**
   * Clears owner-scoped chat UI and reloads sessions for the current Owner Key
   * (guest after logout, or account after sign-in).
   */
  resetForOwnerChange(): void {
    if (this.streamAbort) {
      this.streamAbort();
      this.streamAbort = null;
    }
    this.isLoading.set(false);
    this.streamingMessageId.set(null);
    this.error.set(null);
    this.sessions.set([]);
    this.selectedSkillIds.set([]);
    this.sessionsInitialized = false;
    this.sessionsReady.set(false);
    this.initializationInProgress = false;
    this.clearActiveChat();
    this.initializeSessions();
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
  /** Blocks selectSession/sync while a `/chat` redirect is in flight. */
  private chatRedirectInFlight = false;

  private markSessionsReady(): void {
    this.sessionsInitialized = true;
    this.initializationInProgress = false;
    this.sessionsReady.set(true);
  }

  private refreshSessions(options: {
    createIfEmpty: boolean;
    finalizeBootstrap?: boolean;
  }): void {
    this.getSessions().subscribe({
      next: (sessions) => {
        const sorted = this.sortSessionsByActivity(sessions);
        this.sessions.set(sorted);
        this.resolveBootstrapSession(sorted, options);
        if (options.finalizeBootstrap) {
          this.markSessionsReady();
        }
      },
      error: () => {
        this.sessions.set([]);
        if (options.finalizeBootstrap) {
          this.markSessionsReady();
          this.ensureEmptyDraft(options.createIfEmpty);
        }
      },
    });
  }

  createSession(): void {
    this.ensureEmptyDraft(true, { navigateToChat: true });
  }

  selectSession(sessionId: string, options?: { navigateToChat?: boolean }): void {
    if (!sessionId || this.chatRedirectInFlight) {
      return;
    }
    const syncOpts = options?.navigateToChat
      ? ({ navigateToChat: true } as const)
      : undefined;
    const owned = this.sessions();
    const canValidateOwnership = this.sessionsInitialized || owned.length > 0;
    if (canValidateOwnership && !owned.some(session => session.sessionId === sessionId)) {
      this.redirectToChat(sessionId);
      return;
    }
    if (this.activeSessionId() === sessionId && this.isLoadingSession()) {
      this.syncChatUrl(sessionId, syncOpts);
      return;
    }
    // Same session while a reply is streaming (e.g. URL promote `/chat` → `/chat/:id`).
    // Must not abort SSE or reload history mid-flight.
    if (this.activeSessionId() === sessionId && this.isLoading()) {
      this.syncChatUrl(sessionId, syncOpts);
      return;
    }
    if (
      this.activeSessionId() === sessionId
      && !this.isLoadingSession()
      && this.sessionLoadGeneration > 0
    ) {
      this.syncChatUrl(sessionId, syncOpts);
      return;
    }
    if (this.streamAbort) {
      this.abortStream();
    }
    const loadId = ++this.sessionLoadGeneration;
    this.activeSessionId.set(sessionId);
    this.messages.set([]);
    this.error.set(null);
    this.isLoadingSession.set(true);
    this.rememberActiveSessionIfNeeded(sessionId);
    this.syncChatUrl(sessionId, syncOpts);
    this.getSessionMessages(sessionId).subscribe({
      next: (history) => {
        if (this.isStaleSessionLoad(loadId, sessionId)) {
          return;
        }
        this.messages.set(history.map(msg => this.toUiMessage(msg)));
        this.isLoadingSession.set(false);
        this.rememberActiveSessionIfNeeded(sessionId);
        this.syncChatUrl(sessionId, syncOpts);
      },
      error: () => {
        if (this.isStaleSessionLoad(loadId, sessionId)) {
          return;
        }
        this.redirectToChat(sessionId);
      },
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
            this.clearActiveChat();
          }
        }
      },
    });
  }

  private resolveBootstrapSession(
    sorted: SessionInfo[],
    options: { createIfEmpty: boolean },
  ): void {
    const preferredId = this.sessionIdFromRoute();
    if (preferredId && sorted.some(session => session.sessionId === preferredId)) {
      this.selectSession(preferredId);
      return;
    }
    if (preferredId) {
      // Unknown / foreign `/chat/:id` → redirect to `/chat` first, then draft.
      this.redirectToChat(preferredId, options.createIfEmpty);
      return;
    }

    // Bare `/chat`: empty draft only — never auto-open a history thread.
    const activeId = this.activeSessionId();
    if (activeId && sorted.some(session => session.sessionId === activeId)) {
      this.syncChatUrl(activeId);
      return;
    }
    this.ensureEmptyDraft(options.createIfEmpty);
  }

  /** Reuse the newest empty draft, or create one when allowed. */
  private ensureEmptyDraft(
    createIfMissing: boolean,
    options?: { navigateToChat?: boolean },
  ): void {
    const existingEmpty = this.newestEmptySession();
    if (existingEmpty) {
      this.selectSession(existingEmpty.sessionId, options);
      this.pruneExtraEmptySessions(existingEmpty.sessionId);
      return;
    }
    if (!createIfMissing) {
      this.clearActiveChat();
      return;
    }
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
        this.selectSession(session.sessionId, options);
        this.pruneExtraEmptySessions(session.sessionId);
      },
      complete: () => {
        this.sessionCreationInProgress = false;
      },
      error: () => {
        this.sessionCreationInProgress = false;
      },
    });
  }

  private newestEmptySession(): SessionInfo | undefined {
    return this.sortSessionsByActivity(
      this.sessions().filter(session => !this.sessionRecordHasHistory(session)),
    )[0];
  }

  private pruneExtraEmptySessions(keepId: string): void {
    const extras = this.sessions().filter(
      session => !this.sessionRecordHasHistory(session) && session.sessionId !== keepId,
    );
    for (const extra of extras) {
      this.deleteSessionRequest(extra.sessionId).subscribe({
        next: () => {
          this.sessions.update(list => list.filter(s => s.sessionId !== extra.sessionId));
        },
      });
    }
  }

  private sortSessionsByActivity(sessions: SessionInfo[]): SessionInfo[] {
    return [...sessions].sort((a, b) => {
      const bTime = new Date(b.lastActivityAt).getTime();
      const aTime = new Date(a.lastActivityAt).getTime();
      return bTime - aTime;
    });
  }

  private clearActiveChat(): void {
    this.sessionLoadGeneration += 1;
    this.activeSessionId.set(null);
    this.persistActiveSessionId(null);
    this.messages.set([]);
    this.isLoadingSession.set(false);
    this.syncChatUrl(null);
  }

  private isStaleSessionLoad(loadId: number, sessionId: string): boolean {
    return loadId !== this.sessionLoadGeneration || this.activeSessionId() !== sessionId;
  }

  private sessionRecordHasHistory(session: SessionInfo): boolean {
    return session.messageCount > 0;
  }

  private hasHistory(sessionId: string): boolean {
    const isActive = this.activeSessionId() === sessionId;
    if (isActive && (this.messages().length > 0 || this.isLoading())) {
      return true;
    }
    const session = this.sessions().find(item => item.sessionId === sessionId);
    return session != null && this.sessionRecordHasHistory(session);
  }

  private persistActiveSessionId(sessionId: string | null): void {
    try {
      if (sessionId) {
        sessionStorage.setItem(ACTIVE_SESSION_STORAGE_KEY, sessionId);
      } else {
        sessionStorage.removeItem(ACTIVE_SESSION_STORAGE_KEY);
      }
    } catch {
      // Ignore private-mode / storage quota failures.
    }
  }

  private readPersistedActiveSessionId(): string | null {
    try {
      return sessionStorage.getItem(ACTIVE_SESSION_STORAGE_KEY);
    } catch {
      return null;
    }
  }

  private rememberActiveSessionIfNeeded(sessionId: string | null): void {
    this.persistActiveSessionId(
      sessionId && this.hasHistory(sessionId) ? sessionId : null,
    );
  }

  private sessionIdFromRoute(): string | null {
    const tree = this.router.parseUrl(this.router.url);
    const primary = tree.root.children['primary'];
    const segments = primary?.segments.map(segment => segment.path) ?? [];
    if (segments[0] === 'chat' && segments[1]) {
      return segments[1];
    }
    return tree.queryParams['session'] ?? null;
  }

  private currentChatPath(): string {
    return this.router.url.split('?')[0];
  }

  private shouldExposeSessionInUrl(sessionId: string): boolean {
    if (this.chatRedirectInFlight) {
      return false;
    }
    if (this.hasHistory(sessionId)) {
      return true;
    }
    // Preserve deep link while this session's history is loading (refresh race).
    return this.isLoadingSession()
      && this.activeSessionId() === sessionId
      && this.currentChatPath() === `/chat/${sessionId}`;
  }

  private syncChatUrl(
    sessionId: string | null,
    options?: { navigateToChat?: boolean },
  ): void {
    if (this.chatRedirectInFlight) {
      return;
    }
    const currentPath = this.currentChatPath();
    const onChat = currentPath === '/chat' || currentPath.startsWith('/chat/');
    // Passive sync (bootstrap) must not leave RAG/Policies/etc. User actions may.
    if (!onChat && !options?.navigateToChat) {
      return;
    }
    const expose = sessionId != null && this.shouldExposeSessionInUrl(sessionId);
    const targetUrl = expose && sessionId ? `/chat/${sessionId}` : '/chat';
    if (currentPath === targetUrl) {
      return;
    }
    void this.router.navigateByUrl(targetUrl, { replaceUrl: onChat });
  }

  /** Missing / foreign session: navigate to `/chat`, then open an empty draft. */
  private redirectToChat(failedSessionId: string, createDraft = true): void {
    if (this.chatRedirectInFlight) {
      return;
    }
    this.chatRedirectInFlight = true;
    this.sessions.update((list) => {
      return list.filter(session => session.sessionId !== failedSessionId);
    });
    this.sessionLoadGeneration += 1;
    this.isLoadingSession.set(false);
    this.activeSessionId.set(null);
    this.persistActiveSessionId(null);
    this.messages.set([]);

    void this.router.navigateByUrl('/chat', { replaceUrl: true }).then((succeeded) => {
      this.chatRedirectInFlight = false;
      if (!succeeded && this.currentChatPath() !== '/chat') {
        void this.router.navigateByUrl('/chat', { replaceUrl: true });
      }
      if (createDraft) {
        this.ensureEmptyDraft(true);
      }
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

  sendMessage(content: string): void {
    const sessionId = this.activeSessionId();
    if (!sessionId || !content.trim() || this.isLoading() || this.isLoadingSession()) {
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

    const userMsg: UiMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: content.trim(),
      timestamp: Date.now(),
    };
    const assistantId = `assistant_${Date.now()}`;

    this.messages.update(msgs => [
      ...msgs,
      userMsg,
      { id: assistantId, role: 'assistant', content: '', timestamp: Date.now() },
    ]);
    this.sessions.update(list => list.map(session => (
      session.sessionId === sessionId
        ? { ...session, messageCount: Math.max(session.messageCount, 1) }
        : session
    )));
    // Promote bare `/chat` → `/chat/<id>` once the session has content.
    this.rememberActiveSessionIfNeeded(sessionId);
    this.syncChatUrl(sessionId);
    this.isLoading.set(true);
    this.streamingMessageId.set(assistantId);
    this.error.set(null);

    let fullContent = '';
    const streamRequest: ChatMessage[] = [{ role: 'user', content: userMsg.content }];

    const { abort } = this.chatStream(
      {
        messages: streamRequest,
        sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
        toolsEnabled: this.toolsEnabled(),
        skillIds: this.selectedSkillIds(),
      },
      (chunk) => {
        fullContent += chunk;
        const displayContent = stripToolCallMarkup(fullContent);
        this.messages.update(msgs => msgs.map((msg) => {
          if (msg.id !== assistantId) {
            return msg;
          }
          return { ...msg, content: displayContent };
        }),
        );
      },
      () => {
        // Drop empty placeholder before clearing streaming to avoid a blank-bubble flash.
        this.messages.update((msgs) => {
          const target = msgs.find(msg => msg.id === assistantId);
          if (
            target?.role === 'assistant'
            && !target.content
            && !(target.toolSteps?.length)
          ) {
            return msgs.filter(msg => msg.id !== assistantId);
          }
          return msgs;
        });
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
                publishedAt: item.publishedAt || undefined,
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
    if (!this.streamAbort) {
      return;
    }
    this.streamAbort();
    this.streamAbort = null;
    const streamingId = this.streamingMessageId();
    this.isLoading.set(false);
    this.streamingMessageId.set(null);
    if (!streamingId) {
      return;
    }
    // Drop the empty assistant placeholder so the UI does not stay on "thinking".
    this.messages.update((msgs) => {
      const target = msgs.find(msg => msg.id === streamingId);
      if (
        target?.role === 'assistant'
        && !target.content
        && !(target.toolSteps?.length)
      ) {
        return msgs.filter(msg => msg.id !== streamingId);
      }
      return msgs;
    });
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
    return this.http.get<ChatMessageData[]>(`${API_BASE_URL}/sessions/${sessionId}/messages`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    }).pipe(
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
    const content = msg.role === 'assistant'
      ? stripToolCallMarkup(msg.content ?? '')
      : (msg.content ?? '');
    return {
      id: msg.id ?? `${msg.role}_${timestamp}`,
      role: msg.role === 'assistant' ? 'assistant' : 'user',
      content,
      timestamp,
      sources: msg.sources?.map(source => ({
        title: source.title,
        url: source.url,
        snippet: source.snippet,
        publishedAt: source.publishedAt || undefined,
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
      const stripped = stripToolCallMarkup(msg.content);
      if (stripped !== msg.content) {
        previousSources.set(stripped, msg.sources);
      }
    }
  }

  return history.map((ui) => {
    if (ui.role !== 'assistant' || ui.sources?.length) {
      return ui;
    }
    const local = previousSources.get(ui.content)
      ?? previousSources.get(stripToolCallMarkup(ui.content));
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
