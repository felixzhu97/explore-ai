import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '@core/services/api.service';
import type {
  ChatMessage,
  ProviderInfo,
  ModelInfo,
  SessionInfo,
  ChatMessageData,
} from './chat.model';

export interface UiMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly api = inject(ApiService);

  readonly providers = signal<ProviderInfo[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly selectedProvider = signal('openai');
  readonly selectedModel = signal('gpt-4o-mini');
  readonly isLoadingModels = signal(false);

  readonly sessions = signal<SessionInfo[]>([]);
  readonly activeSessionId = signal<string | null>(null);
  readonly messages = signal<UiMessage[]>([]);
  readonly isLoading = signal(false);
  readonly streamingMessageId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly toolsEnabled = signal(false);

  private streamAbort: (() => void) | null = null;

  loadProviders(): void {
    this.api.getProviders().subscribe({
      next: (data) => {
        this.providers.set(data);
        if (data.length > 0) {
          this.selectedProvider.set(data[0].name);
          this.loadModels(data[0].name);
        }
      },
      error: () => {
        this.providers.set([
          {
            name: 'openai',
            display_name: 'OpenAI',
            models: ['gpt-4o', 'gpt-4o-mini'],
            status: 'available',
          },
        ]);
      },
    });
  }

  loadModels(provider: string): void {
    this.isLoadingModels.set(true);
    this.api.getModels(provider).subscribe({
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
    this.selectedProvider.set(provider);
    this.loadModels(provider);
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
    this.api.getSessions().subscribe({
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
    this.api.createSession().subscribe({
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
    this.api.getSessionMessages(sessionId).subscribe({
      next: (history) => {
        this.messages.set(history.map(msg => this.toUiMessage(msg)));
      },
      error: () => this.messages.set([]),
    });
  }

  deleteSession(sessionId: string): void {
    this.api.deleteSession(sessionId).subscribe({
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
    this.api.getSessionMessages(sessionId).subscribe({
      next: (history) => {
        if (this.activeSessionId() === sessionId && !this.isLoading()) {
          this.messages.set(history.map(msg => this.toUiMessage(msg)));
        }
      },
    });
  }

  sendMessage(content: string): void {
    const sessionId = this.activeSessionId();
    if (!sessionId || !content.trim() || this.isLoading()) {
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
    this.isLoading.set(true);
    this.streamingMessageId.set(assistantId);
    this.error.set(null);

    let fullContent = '';
    const streamRequest: ChatMessage[] = [{ role: 'user', content: userMsg.content }];

    const { abort } = this.api.chatStream(
      {
        messages: streamRequest,
        session_id: sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
        tools_enabled: this.toolsEnabled(),
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
    };
  }
}
