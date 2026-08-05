import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  model,
  signal,
} from '@angular/core';
import {
  ChatBubbleMessage,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
} from '../shared/components/chat-shell';
import { I18nService } from '../core/i18n';
import { ZardAlertComponent } from '../shared/components/alert';
import { AgentsService } from './agents.service';
import type { AgentSessionInfo } from './agents.model';

@Component({
  selector: 'app-agents-page',
  imports: [ChatMessagePaneComponent, ChatSenderBarComponent, ZardAlertComponent],
  templateUrl: './agents.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class AgentsPageComponent implements OnInit {
  private readonly api = inject(AgentsService);
  readonly i18n = inject(I18nService);

  readonly sessions = signal<AgentSessionInfo[]>([]);
  readonly activeSessionId = signal<string | null>(null);
  readonly messages = signal<ChatBubbleMessage[]>([]);
  readonly streamingMessageId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly trace = signal<string[]>([]);
  readonly input = model('');

  private abort: (() => void) | null = null;

  ngOnInit(): void {
    this.refreshSessions();
  }

  newSession(): void {
    this.api.createSession(this.i18n.t().agents.defaultTitle).subscribe({
      next: (session) => {
        this.sessions.update(list => [session, ...list]);
        this.selectSession(session.id);
      },
      error: () => this.error.set(this.i18n.t().agents.errorMessage),
    });
  }

  selectSession(id: string): void {
    if (this.abort) {
      this.abort();
      this.abort = null;
    }
    this.activeSessionId.set(id);
    this.messages.set([]);
    this.trace.set([]);
    this.error.set(null);
    this.streamingMessageId.set(null);
    this.loading.set(false);
  }

  deleteSession(event: Event, id: string): void {
    event.stopPropagation();
    this.api.deleteSession(id).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.id !== id));
        if (this.activeSessionId() === id) {
          this.activeSessionId.set(null);
          this.messages.set([]);
          this.trace.set([]);
        }
      },
    });
  }

  send(): void {
    const text = this.input().trim();
    if (!text || this.loading()) {
      return;
    }
    const ensureSession = this.activeSessionId()
      ? Promise.resolve(this.activeSessionId()!)
      : new Promise<string>((resolve, reject) => {
          this.api.createSession(this.i18n.t().agents.defaultTitle).subscribe({
            next: (session) => {
              this.sessions.update(list => [session, ...list]);
              this.activeSessionId.set(session.id);
              resolve(session.id);
            },
            error: reject,
          });
        });

    ensureSession
      .then(sessionId => this.runInvoke(sessionId, text))
      .catch(() => this.error.set(this.i18n.t().agents.errorMessage));
  }

  private runInvoke(sessionId: string, text: string): void {
    this.input.set('');
    this.error.set(null);
    this.loading.set(true);
    this.trace.set([]);

    const userMsg: ChatBubbleMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: text,
    };
    const assistantId = crypto.randomUUID();
    const assistantMsg: ChatBubbleMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
    };
    this.messages.update(msgs => [...msgs, userMsg, assistantMsg]);
    this.streamingMessageId.set(assistantId);

    const labels = this.i18n.t().agents.trace;
    let raw = '';
    const { abort } = this.api.invokeStream(sessionId, { message: text }, {
      onPlan: data => this.pushTrace(labels.plan, data),
      onReplan: data => this.pushTrace(labels.replan, data),
      onThought: data => this.pushTrace(labels.thought, data),
      onTool: data => this.pushTrace(labels.tool, data),
      onEvaluation: data => this.pushTrace(labels.evaluation, data),
      onChunk: (token) => {
        raw += token;
        this.patchAssistant(assistantId, raw);
      },
      onDone: () => this.finish(assistantId, raw),
      onError: (err) => {
        const fallback = this.i18n.t().agents.errorMessage;
        this.error.set(err.message || fallback);
        this.finish(assistantId, raw || fallback);
      },
    });
    this.abort = abort;
  }

  private pushTrace(label: string, data: string): void {
    const detail = this.parseTraceDetail(data);
    const entry = detail ? `${label}: ${detail}` : label;
    this.trace.update(items => [...items, entry].slice(-12));
  }

  private parseTraceDetail(data: string): string {
    try {
      const parsed = JSON.parse(data) as {
        summary?: string;
        verdict?: string;
        name?: string;
        text?: string;
      };
      return (
        parsed.summary || parsed.verdict || parsed.name || parsed.text || ''
      );
    } catch {
      return data.slice(0, 48);
    }
  }

  private patchAssistant(assistantId: string, content: string): void {
    this.messages.update(msgs => msgs.map((message) => {
      if (message.id !== assistantId) {
        return message;
      }
      return { ...message, content };
    }),
    );
  }

  private finish(assistantId: string, content: string): void {
    this.loading.set(false);
    this.streamingMessageId.set(null);
    this.abort = null;
    this.patchAssistant(assistantId, content);
  }

  private refreshSessions(): void {
    this.api.listSessions().subscribe({
      next: sessions => this.sessions.set(sessions),
      error: () => this.sessions.set([]),
    });
  }
}
