import { Service, inject, signal } from '@angular/core';
import { Instant } from '@js-joda/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Router } from '@angular/router';
import { type Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { STORAGE_KEYS } from '../storage-keys';
import { SKIP_ERROR_NOTIFICATION } from '../http/http-error.context';
import {
  parseChatStreamEvent,
  streamSsePost,
  type ChatStreamEvent,
  type WebSource,
} from '../http/sse-client';
import { DEFAULT_MODELS, DEFAULT_PROVIDERS } from './chat.constants';
import { stripToolCallMarkup } from '../chat-shell/tool-call-markup.util';
import type { ToolStep } from '../chat-shell/chat-bubble-list.component';
import { hasItems, hasText } from '../shared/presence';

export interface ChatStreamMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

/** GET /api/chat/sessions/{id}/messages item; `timestamp` is ISO-8601. */
export interface ChatHistoryMessage extends ChatStreamMessage {
  id?: string;
  timestamp: string;
  toolCalls?: ToolCall[];
  isLoading?: boolean;
  sources?: WebSource[];
}

/** POST /api/chat/stream */
export interface ChatStreamRequest {
  messages: ChatStreamMessage[];
  sessionId?: string;
  provider?: string;
  model?: string;
  toolsEnabled?: boolean;
  skillIds?: string[];
}

export interface ChatModel {
  name: string;
  provider: string;
  description?: string;
  maxTokens?: number;
}

export interface ChatProvider {
  name: string;
  displayName: string;
  models: string[];
  status: 'available' | 'unavailable';
}

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: 'pending' | 'running' | 'success' | 'error';
}

/** Session as sent by the API, with ISO-8601 instants. */
export interface ChatSessionSummaryDto {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  lastActivityAt: string;
}

export interface ChatSessionSummary {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: Instant;
  lastActivityAt: Instant;
}

export function toChatSessionSummary(dto: ChatSessionSummaryDto): ChatSessionSummary {
  return {
    ...dto,
    createdAt: Instant.parse(dto.createdAt),
    lastActivityAt: Instant.parse(dto.lastActivityAt),
  };
}

/** Message in the local chat thread, merged from the stream and session history. */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Instant;
  toolSteps?: ToolStep[] | undefined;
  sources?: WebSource[] | undefined;
}

type ChatStreamEventHandler = (event: ChatStreamEvent) => void;

@Service()
export class ChatService {
  readonly #http = inject(HttpClient);
  readonly #router = inject(Router);

  readonly providers = signal<ChatProvider[]>([]);
  readonly models = signal<ChatModel[]>([]);
  readonly selectedProvider = signal('openai');
  readonly selectedModel = signal('deepseek-v4-flash');
  readonly isLoadingModels = signal(false);

  readonly sessions = signal<ChatSessionSummary[]>([]);
  readonly activeSessionId = signal<string | null>(null);
  readonly messages = signal<ChatMessage[]>([]);
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

  #streamAbort: (() => void) | null = null;
  #sessionLoadGeneration = 0;

  loadProviders(): void {
    this.#getProviders().subscribe({
      next: (data) => {
        this.providers.set(data);
        const available = data.find(p => p.status === 'available') ?? data[0];
        if (available !== undefined) {
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
    this.#getModels(provider).subscribe({
      next: (data) => {
        this.models.set(data);
        const defaultModel =
          data.find(m => m.name.includes('mini') || m.name.includes('flash')) ?? data[0];
        if (defaultModel !== undefined) {
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
    return provider === undefined || provider.status === 'available';
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
    this.#refreshSessions({ createIfEmpty: false });
  }

  /**
   * Clears owner-scoped chat UI and reloads sessions for the current Owner Key
   * (guest after logout, or account after sign-in).
   */
  resetForOwnerChange(): void {
    if (this.#streamAbort !== null) {
      this.#streamAbort();
      this.#streamAbort = null;
    }
    this.isLoading.set(false);
    this.streamingMessageId.set(null);
    this.error.set(null);
    this.sessions.set([]);
    this.selectedSkillIds.set([]);
    this.#sessionsInitialized = false;
    this.sessionsReady.set(false);
    this.#initializationInProgress = false;
    this.#clearActiveChat();
    this.initializeSessions();
  }

  initializeSessions(): void {
    if (this.#sessionsInitialized || this.#initializationInProgress) {
      return;
    }
    this.#initializationInProgress = true;
    this.#refreshSessions({ createIfEmpty: true, finalizeBootstrap: true });
  }

  #sessionsInitialized = false;
  #initializationInProgress = false;
  #sessionCreationInProgress = false;
  /** Blocks selectSession/sync while a `/chat` redirect is in flight. */
  #chatRedirectInFlight = false;

  #markSessionsReady(): void {
    this.#sessionsInitialized = true;
    this.#initializationInProgress = false;
    this.sessionsReady.set(true);
  }

  #refreshSessions(options: {
    createIfEmpty: boolean;
    finalizeBootstrap?: boolean;
  }): void {
    this.#getSessions().subscribe({
      next: (sessions) => {
        const sorted = this.#sortSessionsByActivity(sessions);
        this.sessions.set(sorted);
        this.#resolveBootstrapSession(sorted, options);
        if (options.finalizeBootstrap === true) {
          this.#markSessionsReady();
        }
      },
      error: () => {
        this.sessions.set([]);
        if (options.finalizeBootstrap === true) {
          this.#markSessionsReady();
          this.#ensureEmptyDraft(options.createIfEmpty);
        }
      },
    });
  }

  createSession(): void {
    this.#ensureEmptyDraft(true, { navigateToChat: true });
  }

  selectSession(sessionId: string, options?: { navigateToChat?: boolean }): void {
    if (sessionId === '' || this.#chatRedirectInFlight) {
      return;
    }
    const syncOpts = options?.navigateToChat === true
      ? ({ navigateToChat: true } as const)
      : undefined;
    const owned = this.sessions();
    const canValidateOwnership = this.#sessionsInitialized || owned.length > 0;
    if (canValidateOwnership && !owned.some(session => session.sessionId === sessionId)) {
      this.#redirectToChat(sessionId);
      return;
    }
    if (this.activeSessionId() === sessionId && this.isLoadingSession()) {
      this.#syncChatUrl(sessionId, syncOpts);
      return;
    }
    // Same session while a reply is streaming (e.g. URL promote `/chat` → `/chat/:id`).
    // Must not abort SSE or reload history mid-flight.
    if (this.activeSessionId() === sessionId && this.isLoading()) {
      this.#syncChatUrl(sessionId, syncOpts);
      return;
    }
    if (
      this.activeSessionId() === sessionId
      && !this.isLoadingSession()
      && this.#sessionLoadGeneration > 0
    ) {
      this.#syncChatUrl(sessionId, syncOpts);
      return;
    }
    if (this.#streamAbort !== null) {
      this.abortStream();
    }
    const loadId = ++this.#sessionLoadGeneration;
    this.activeSessionId.set(sessionId);
    this.messages.set([]);
    this.error.set(null);
    this.isLoadingSession.set(true);
    this.#rememberActiveSessionIfNeeded(sessionId);
    this.#syncChatUrl(sessionId, syncOpts);
    this.#getSessionMessages(sessionId).subscribe({
      next: (history) => {
        if (this.#isStaleSessionLoad(loadId, sessionId)) {
          return;
        }
        this.messages.set(withoutEmptyBodies(
          history.map(message => this.#toChatMessage(message)),
        ));
        this.isLoadingSession.set(false);
        this.#rememberActiveSessionIfNeeded(sessionId);
        this.#syncChatUrl(sessionId, syncOpts);
      },
      error: () => {
        if (this.#isStaleSessionLoad(loadId, sessionId)) {
          return;
        }
        this.#redirectToChat(sessionId);
      },
    });
  }

  deleteSession(sessionId: string): void {
    this.#deleteSessionRequest(sessionId).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.sessionId !== sessionId));
        if (this.activeSessionId() === sessionId) {
          const [next] = this.sessions();
          if (next !== undefined) {
            this.selectSession(next.sessionId);
          } else {
            this.#clearActiveChat();
          }
        }
      },
    });
  }

  #resolveBootstrapSession(
    sorted: ChatSessionSummary[],
    options: { createIfEmpty: boolean },
  ): void {
    const preferredId = this.#sessionIdFromRoute();
    if (
      preferredId !== null
      && sorted.some(session => session.sessionId === preferredId)
    ) {
      this.selectSession(preferredId);
      return;
    }
    if (preferredId !== null) {
      // Unknown / foreign `/chat/:id` → redirect to `/chat` first, then draft.
      this.#redirectToChat(preferredId, options.createIfEmpty);
      return;
    }

    // Bare `/chat`: empty draft only — never auto-open a history thread.
    const activeId = this.activeSessionId();
    if (hasText(activeId) && sorted.some(session => session.sessionId === activeId)) {
      this.#syncChatUrl(activeId);
      return;
    }
    this.#ensureEmptyDraft(options.createIfEmpty);
  }

  /** Reuse the newest empty draft, or create one when allowed. */
  #ensureEmptyDraft(
    createIfMissing: boolean,
    options?: { navigateToChat?: boolean },
  ): void {
    const existingEmpty = this.#newestEmptySession();
    if (existingEmpty !== undefined) {
      this.selectSession(existingEmpty.sessionId, options);
      this.#pruneExtraEmptySessions(existingEmpty.sessionId);
      return;
    }
    if (!createIfMissing) {
      this.#clearActiveChat();
      return;
    }
    if (this.#sessionCreationInProgress) {
      return;
    }
    this.#sessionCreationInProgress = true;
    this.#createSessionRequest().subscribe({
      next: (session) => {
        this.sessions.update((list) => {
          const withoutCurrent = list.filter(s => s.sessionId !== session.sessionId);
          return [session, ...withoutCurrent];
        });
        this.selectSession(session.sessionId, options);
        this.#pruneExtraEmptySessions(session.sessionId);
      },
      complete: () => {
        this.#sessionCreationInProgress = false;
      },
      error: () => {
        this.#sessionCreationInProgress = false;
      },
    });
  }

  #newestEmptySession(): ChatSessionSummary | undefined {
    return this.#sortSessionsByActivity(
      this.sessions().filter(session => !this.#sessionRecordHasHistory(session)),
    )[0];
  }

  #pruneExtraEmptySessions(keepId: string): void {
    const extras = this.sessions().filter(
      session => !this.#sessionRecordHasHistory(session) && session.sessionId !== keepId,
    );
    for (const extra of extras) {
      this.#deleteSessionRequest(extra.sessionId).subscribe({
        next: () => {
          this.sessions.update(list => list.filter(s => s.sessionId !== extra.sessionId));
        },
      });
    }
  }

  #sortSessionsByActivity(sessions: ChatSessionSummary[]): ChatSessionSummary[] {
    return [...sessions].sort((a, b) => b.lastActivityAt.compareTo(a.lastActivityAt));
  }

  #clearActiveChat(): void {
    this.#sessionLoadGeneration += 1;
    this.activeSessionId.set(null);
    this.#persistActiveSessionId(null);
    this.messages.set([]);
    this.isLoadingSession.set(false);
    this.#syncChatUrl(null);
  }

  #isStaleSessionLoad(loadId: number, sessionId: string): boolean {
    return loadId !== this.#sessionLoadGeneration || this.activeSessionId() !== sessionId;
  }

  #sessionRecordHasHistory(session: ChatSessionSummary): boolean {
    return session.messageCount > 0;
  }

  #hasHistory(sessionId: string): boolean {
    const isActive = this.activeSessionId() === sessionId;
    if (isActive && (this.messages().length > 0 || this.isLoading())) {
      return true;
    }
    const session = this.sessions().find(item => item.sessionId === sessionId);
    return session !== undefined && this.#sessionRecordHasHistory(session);
  }

  #persistActiveSessionId(sessionId: string | null): void {
    try {
      if (hasText(sessionId)) {
        sessionStorage.setItem(STORAGE_KEYS.CHAT_ACTIVE_SESSION_ID, sessionId);
      } else {
        sessionStorage.removeItem(STORAGE_KEYS.CHAT_ACTIVE_SESSION_ID);
      }
    } catch {
      // Ignore private-mode / storage quota failures.
    }
  }

  #rememberActiveSessionIfNeeded(sessionId: string | null): void {
    this.#persistActiveSessionId(
      hasText(sessionId) && this.#hasHistory(sessionId) ? sessionId : null,
    );
  }

  #sessionIdFromRoute(): string | null {
    const tree = this.#router.parseUrl(this.#router.url);
    const primary = tree.root.children['primary'];
    const segments = primary?.segments.map(segment => segment.path) ?? [];
    const [section, routeSessionId] = segments;
    if (section === 'chat' && hasText(routeSessionId)) {
      return routeSessionId;
    }
    const session: unknown = tree.queryParams['session'];
    return typeof session === 'string' && session !== '' ? session : null;
  }

  #currentChatPath(): string {
    return this.#router.url.split('?')[0] ?? '';
  }

  #shouldExposeSessionInUrl(sessionId: string): boolean {
    if (this.#chatRedirectInFlight) {
      return false;
    }
    if (this.#hasHistory(sessionId)) {
      return true;
    }
    // Preserve deep link while this session's history is loading (refresh race).
    return this.isLoadingSession()
      && this.activeSessionId() === sessionId
      && this.#currentChatPath() === `/chat/${sessionId}`;
  }

  #syncChatUrl(
    sessionId: string | null,
    options?: { navigateToChat?: boolean },
  ): void {
    if (this.#chatRedirectInFlight) {
      return;
    }
    const currentPath = this.#currentChatPath();
    const onChat = currentPath === '/chat' || currentPath.startsWith('/chat/');
    // Passive sync (bootstrap) must not leave RAG/Policies/etc. User actions may.
    if (!onChat && options?.navigateToChat !== true) {
      return;
    }
    const expose = hasText(sessionId) && this.#shouldExposeSessionInUrl(sessionId);
    const targetUrl = hasText(sessionId) && expose ? `/chat/${sessionId}` : '/chat';
    if (currentPath === targetUrl) {
      return;
    }
    void this.#router.navigateByUrl(targetUrl, { replaceUrl: onChat });
  }

  /** Missing / foreign session: navigate to `/chat`, then open an empty draft. */
  #redirectToChat(failedSessionId: string, createDraft = true): void {
    if (this.#chatRedirectInFlight) {
      return;
    }
    this.#chatRedirectInFlight = true;
    this.sessions.update((list) => {
      return list.filter(session => session.sessionId !== failedSessionId);
    });
    this.#sessionLoadGeneration += 1;
    this.isLoadingSession.set(false);
    this.activeSessionId.set(null);
    this.#persistActiveSessionId(null);
    this.messages.set([]);

    void this.#router.navigateByUrl('/chat', { replaceUrl: true }).then((succeeded) => {
      this.#chatRedirectInFlight = false;
      if (!succeeded && this.#currentChatPath() !== '/chat') {
        void this.#router.navigateByUrl('/chat', { replaceUrl: true });
      }
      if (createDraft) {
        this.#ensureEmptyDraft(true);
      }
    });
  }

  #syncSessionMessages(sessionId: string): void {
    if (this.activeSessionId() !== sessionId || this.isLoading()) {
      return;
    }
    this.#getSessionMessages(sessionId).subscribe({
      next: (history) => {
        if (this.activeSessionId() === sessionId && !this.isLoading()) {
          this.messages.update((previous) => {
            const fromApi = withoutEmptyBodies(mergeHistoryWithLocalMessages(
              history.map(message => this.#toChatMessage(message)),
              previous,
            ));
            const apiIds = new Set(fromApi.map(message => message.id));
            const apiHasAssistant = fromApi.some(message => message.role === 'assistant');
            const localOnly = apiHasAssistant
              ? []
              : previous.filter(
                  message => message.role === 'assistant'
                    && !apiIds.has(message.id)
                    && hasRenderableBody(message),
                );
            return withoutEmptyBodies([...fromApi, ...localOnly]);
          });
        }
      },
    });
  }

  sendMessage(content: string): void {
    const sessionId = this.activeSessionId();
    if (
      !hasText(sessionId)
      || content.trim() === ''
      || this.isLoading()
      || this.isLoadingSession()
    ) {
      return;
    }

    if (!this.isSelectedProviderAvailable()) {
      const provider = this.providers().find(p => p.name === this.selectedProvider());
      this.error.set(
        `${provider?.displayName ?? this.selectedProvider()} is not configured. Configure the API key or enable the provider before chatting.`,
      );
      return;
    }

    if (this.#streamAbort !== null) {
      this.#streamAbort();
    }

    const now = Instant.now();
    const userMsg: ChatMessage = {
      id: `user_${now.toEpochMilli()}`,
      role: 'user',
      content: content.trim(),
      timestamp: now,
    };
    const assistantId = `assistant_${now.toEpochMilli()}`;

    this.messages.update(messages => [
      ...messages,
      userMsg,
      { id: assistantId, role: 'assistant', content: '', timestamp: now },
    ]);
    this.sessions.update(list => list.map(session => (
      session.sessionId === sessionId
        ? { ...session, messageCount: Math.max(session.messageCount, 1) }
        : session
    )));
    // Promote bare `/chat` → `/chat/<id>` once the session has content.
    this.#rememberActiveSessionIfNeeded(sessionId);
    this.#syncChatUrl(sessionId);
    this.isLoading.set(true);
    this.streamingMessageId.set(assistantId);
    this.error.set(null);

    let fullContent = '';
    const streamRequest: ChatStreamMessage[] = [{ role: 'user', content: userMsg.content }];

    const { abort } = this.#chatStream(
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
        this.messages.update(messages => messages.map((message) => {
          if (message.id !== assistantId) {
            return message;
          }
          return { ...message, content: displayContent };
        }),
        );
      },
      () => {
        // Drop empty placeholder before clearing streaming to avoid a blank-bubble flash.
        this.messages.update((messages) => {
          const target = messages.find(message => message.id === assistantId);
          if (target !== undefined && !hasRenderableBody(target)) {
            return messages.filter(message => message.id !== assistantId);
          }
          return messages;
        });
        this.isLoading.set(false);
        this.streamingMessageId.set(null);
        this.#streamAbort = null;
        this.#syncSessionMessages(sessionId);
        this.loadSessions();
        setTimeout(() => {
          this.#syncSessionMessages(sessionId);
          this.loadSessions();
        }, 2500);
      },
      (error) => {
        this.error.set(error.message);
        this.messages.update(messages => messages.map((message) => {
          if (message.id !== assistantId) {
            return message;
          }
          return { ...message, content: error.message };
        }),
        );
        this.isLoading.set(false);
        this.streamingMessageId.set(null);
        this.#streamAbort = null;
      },
      (event) => {
        if (event.type === 'message') {
          return;
        }
        this.messages.update(messages => messages.map((message) => {
          if (message.id !== assistantId) {
            return message;
          }
          if (event.type === 'tool_call') {
            const steps = [...(message.toolSteps ?? [])];
            steps.push({
              name: event.name,
              label: toolLabel(event.name),
              status: 'running',
            });
            return { ...message, toolSteps: steps };
          }
          if (event.type === 'tool_result') {
            const steps = (message.toolSteps ?? []).map((step) => {
              if (step.name !== event.name || step.status !== 'running') {
                return step;
              }
              return {
                ...step,
                status: event.ok ? 'success' as const : 'error' as const,
              };
            });
            return { ...message, toolSteps: steps };
          }
          return {
            ...message,
            sources: event.items.map(item => ({
              title: item.title,
              url: item.url,
              snippet: item.snippet,
              publishedAt: item.publishedAt === '' ? undefined : item.publishedAt,
            })),
          };
        }));
      },
    );
    this.#streamAbort = abort;
  }

  abortStream(): void {
    if (this.#streamAbort === null) {
      return;
    }
    this.#streamAbort();
    this.#streamAbort = null;
    const streamingId = this.streamingMessageId();
    this.isLoading.set(false);
    this.streamingMessageId.set(null);
    if (streamingId === null) {
      return;
    }
    // Drop the empty assistant placeholder so the UI does not stay on "thinking".
    this.messages.update((messages) => {
      const target = messages.find(message => message.id === streamingId);
      if (target !== undefined && !hasRenderableBody(target)) {
        return messages.filter(message => message.id !== streamingId);
      }
      return messages;
    });
  }

  #getProviders(): Observable<ChatProvider[]> {
    return this.#http
      .get<ChatProvider[]>(`${API_BASE_URL}/chat/providers`)
      .pipe(catchError(() => of(DEFAULT_PROVIDERS)));
  }

  #getModels(provider: string): Observable<ChatModel[]> {
    return this.#http
      .get<{ provider: string; models: ChatModel[]; count: number }>(`${API_BASE_URL}/chat/models`, {
        params: { provider },
      })
      .pipe(
        map(res => res.models),
        catchError(() => of(DEFAULT_MODELS[provider] ?? DEFAULT_MODELS['openai'] ?? [])),
      );
  }

  #createSessionRequest(title?: string): Observable<ChatSessionSummary> {
    return this.#http
      .post<ChatSessionSummaryDto>(
        `${API_BASE_URL}/chat/sessions`,
        hasText(title) ? { title } : {},
      )
      .pipe(map(toChatSessionSummary));
  }

  #getSessions(): Observable<ChatSessionSummary[]> {
    return this.#http
      .get<ChatSessionSummaryDto[]>(`${API_BASE_URL}/chat/sessions`)
      .pipe(map(sessions => sessions.map(toChatSessionSummary)));
  }

  #getSessionMessages(sessionId: string): Observable<ChatHistoryMessage[]> {
    return this.#http.get<ChatHistoryMessage[]>(`${API_BASE_URL}/chat/sessions/${sessionId}/messages`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  #deleteSessionRequest(sessionId: string): Observable<void> {
    return this.#http.delete<void>(`${API_BASE_URL}/chat/sessions/${sessionId}`);
  }

  #chatStream(
    request: ChatStreamRequest,
    onChunk: (token: string) => void,
    onDone: () => void,
    onError: (error: Error) => void,
    onEvent?: ChatStreamEventHandler,
  ): { abort: () => void } {
    let finished = false;
    const finish = () => {
      if (!finished) {
        finished = true;
        onDone();
      }
    };

    return streamSsePost(`${API_BASE_URL}/chat/stream`, request, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]' || eventType === 'done') {
          finish();
          return true;
        }

        if (eventType === 'error') {
          let message = 'Stream error';
          try {
            type ErrorBody = { error?: string; message?: string } | null;
            const parsed = JSON.parse(data) as ErrorBody;
            message = parsed?.error ?? parsed?.message ?? message;
          } catch {
            if (data !== '') {
              message = data;
            }
          }
          onError(new Error(message));
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

  #toChatMessage(message: ChatHistoryMessage): ChatMessage {
    const timestamp = Instant.parse(message.timestamp);
    const content = message.role === 'assistant'
      ? stripToolCallMarkup(message.content)
      : message.content;
    return {
      id: message.id ?? `${message.role}_${timestamp.toEpochMilli()}`,
      role: message.role === 'assistant' ? 'assistant' : 'user',
      content,
      timestamp,
      sources: message.sources?.map(source => ({
        title: source.title,
        url: source.url,
        snippet: source.snippet,
        publishedAt: source.publishedAt === '' ? undefined : source.publishedAt,
      })),
    };
  }
}

/**
 * After stream complete, history sync may race DB persistence.
 * Keep local sources when API has not returned them yet for the same content.
 */
export function mergeHistoryWithLocalMessages(
  history: ChatMessage[],
  previous: ChatMessage[],
): ChatMessage[] {
  const previousSources = new Map<string, WebSource[]>();
  for (const message of previous) {
    if (message.role === 'assistant' && hasItems(message.sources)) {
      previousSources.set(message.content, message.sources);
      const stripped = stripToolCallMarkup(message.content);
      if (stripped !== message.content) {
        previousSources.set(stripped, message.sources);
      }
    }
  }

  return history.map((ui) => {
    if (ui.role !== 'assistant' || hasItems(ui.sources)) {
      return ui;
    }
    const local = previousSources.get(ui.content)
      ?? previousSources.get(stripToolCallMarkup(ui.content));
    return hasItems(local) ? { ...ui, sources: local } : ui;
  });
}

function hasRenderableBody(message: ChatMessage): boolean {
  return message.content.trim() !== ''
    || hasItems(message.toolSteps)
    || hasItems(message.sources);
}

function withoutEmptyBodies(messages: ChatMessage[]): ChatMessage[] {
  return messages.filter(hasRenderableBody);
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
