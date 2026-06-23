import {
  Component,
  signal,
  inject,
  OnInit,
  OnDestroy,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
  output,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { MarkdownService } from '@shared/utils/markdown.service';
import { I18nService } from '@core/i18n';
import { NxSenderModule } from 'ng-zorro-x/sender';
import type { ChatMessage, ProviderInfo, ModelInfo, ChatTabState } from './chat.model';

@Component({
  selector: 'app-chat-tab',
  standalone: true,
  imports: [CommonModule, FormsModule, NxSenderModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ i18n.t().aiHub.chat.title }}</h3>
          <p class="panel-description">{{ i18n.t().aiHub.chat.description }}</p>
        </div>
      </div>
      <div class="panel-content">
        <!-- Model Selector -->
        @if (providers().length > 0) {
          <div class="model-selector">
            <span class="model-label">{{ i18n.t().aiHub.chat.provider }}:</span>
            <select
              class="model-select"
              [ngModel]="selectedProvider()"
              (ngModelChange)="onProviderChange($event)"
            >
              @for (provider of providers(); track provider.name) {
                <option [value]="provider.name">
                  {{ provider.display_name }}
                </option>
              }
            </select>

            <span class="model-label">{{ i18n.t().aiHub.chat.model }}:</span>
            <select
              class="model-select"
              [ngModel]="selectedModel()"
              (ngModelChange)="setSelectedModel($event)"
              [disabled]="isLoadingModels()"
            >
              @if (isLoadingModels()) {
                <option>Loading...</option>
              } @else {
                @for (model of models(); track model.name) {
                  <option [value]="model.name">{{ model.name }}</option>
                }
              }
            </select>

            @if (selectedModel()) {
              <span class="model-badge"> {{ selectedProvider() }}/{{ selectedModel() }} </span>
            }
          </div>
        }

        <!-- Chat Messages -->
        <div class="chat-container" #chatContainer>
          @if (messages().length === 0) {
            <div class="empty-state">
              <div class="empty-icon">
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <p class="empty-title">{{ i18n.t().agents.startConversation }}</p>
              <p class="empty-subtitle">
                {{ i18n.t().aiHub.chat.description }}
              </p>
              <div class="quick-actions">
                <button
                  class="quick-action"
                  (click)="setInput(i18n.t().aiHub.quickPrompts.greeting)"
                >
                  {{ i18n.t().aiHub.quickPrompts.greeting }}
                </button>
                <button class="quick-action" (click)="setInput(i18n.t().aiHub.quickPrompts.help)">
                  {{ i18n.t().aiHub.quickPrompts.help }}
                </button>
                <button
                  class="quick-action"
                  (click)="setInput(i18n.t().aiHub.quickPrompts.creative)"
                >
                  {{ i18n.t().aiHub.quickPrompts.creative }}
                </button>
              </div>
            </div>
          } @else {
            @for (msg of messages(); track msg.id) {
              <div class="message-bubble" [class.user]="msg.role === 'user'">
                <div class="message-content" [class.user]="msg.role === 'user'">
                  @if (msg.role === 'user') {
                    {{ msg.content }}
                  } @else {
                    <div class="markdown-content" [innerHTML]="markdown.render(msg.content)"></div>
                  }
                </div>
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
            }
            @if (isLoading() && messages()[messages().length - 1]?.content === '') {
              <div class="message-bubble">
                <div class="message-content">
                  <span class="btn-spinner"></span>
                  {{ i18n.t().aiHub.chat.thinking }}
                </div>
              </div>
            }
            <div #messagesEnd></div>
          }
        </div>

        @if (error()) {
          <div class="error-message">{{ error() }}</div>
        }

        <!-- Input Area -->
        <nx-sender
          [value]="input()"
          [loading]="isLoading()"
          [disabled]="isLoading()"
          [placeholder]="i18n.t().aiHub.chat.inputPlaceholder"
          (valueChange)="setInput($event)"
          (submitSend)="send()"
        />
      </div>
    </div>
  `,
  styles: [
    `
      .panel {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px;
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
      }

      .panel-title {
        font-size: 17px;
        font-weight: 600;
        color: #1d1d1f;
        margin: 0;
      }

      .panel-description {
        font-size: 14px;
        color: #86868b;
        margin: 4px 0 0 0;
      }

      .panel-content {
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
        padding: 24px;
      }

      .model-selector {
        display: flex;
        gap: 16px;
        align-items: center;
        flex-wrap: wrap;
        padding: 16px;
        background: #f5f5f7;
        border-radius: 8px;
        margin-bottom: 16px;
      }

      .model-label {
        font-size: 14px;
        font-weight: 500;
        color: #6e6e73;
      }

      .model-select {
        padding: 8px 16px;
        font-size: 14px;
        border: 1px solid #e5e5e5;
        border-radius: 8px;
        background: #ffffff;
        color: #1d1d1f;
        cursor: pointer;
        min-width: 120px;
      }

      .model-select:focus {
        outline: none;
        border-color: #0071e3;
      }

      .model-select:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .model-badge {
        font-size: 12px;
        color: #86868b;
        padding: 2px 8px;
        background: #e5e5e5;
        border-radius: 4px;
      }

      .chat-container {
        display: flex;
        flex-direction: column;
        gap: 12px;
        max-height: 400px;
        min-height: 200px;
        overflow-y: auto;
        padding: 16px;
        background: #ffffff;
        border-radius: 14px;
        border: 1px solid var(--color-border);
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px;
        color: #6e6e73;
        text-align: center;
        gap: 8px;
      }

      .empty-icon {
        font-size: 48px;
        opacity: 0.5;
      }

      .empty-title {
        font-size: 16px;
        font-weight: 500;
        color: #1d1d1f;
        margin: 0;
      }

      .empty-subtitle {
        font-size: 14px;
        color: #86868b;
        margin: 0;
      }

      .quick-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        justify-content: center;
        margin-top: 8px;
      }

      .quick-action {
        padding: 6px 12px;
        font-size: 14px;
        background: #ffffff;
        border: 1px solid #e5e5e5;
        border-radius: 20px;
        color: #0071e3;
        cursor: pointer;
        transition: all 0.15s ease;
      }

      .quick-action:hover {
        background: #f5f5f7;
      }

      .message-bubble {
        display: flex;
        flex-direction: column;
        max-width: 75%;
        animation: fadeIn 0.25s ease;
        align-self: flex-start;
        align-items: flex-start;
      }

      .message-bubble.user {
        align-self: flex-end;
        align-items: flex-end;
      }

      .message-content {
        padding: 16px;
        border-radius: 14px;
        font-size: 15px;
        line-height: 1.5;
        word-break: break-word;
        background: #f5f5f7;
        color: #1d1d1f;
        border: 1px solid var(--color-border);
      }

      .message-content.user {
        background: #0071e3;
        color: white;
        border: none;
      }

      .markdown-content {
        line-height: 1.6;
      }

      .markdown-content h1,
      .markdown-content h2,
      .markdown-content h3 {
        margin: 0.5em 0 0.25em;
        font-weight: 600;
      }

      .markdown-content p {
        margin: 0.5em 0;
      }

      .markdown-content code {
        font-family: 'SF Mono', Monaco, monospace;
        font-size: 0.9em;
        padding: 0.15em 0.4em;
        border-radius: 4px;
        background: #e5e5e5;
      }

      .markdown-content pre {
        margin: 0.5em 0;
        padding: 12px;
        border-radius: 8px;
        background: #e5e5e5;
        overflow-x: auto;
      }

      .markdown-content blockquote {
        margin: 0.5em 0;
        padding-left: 1em;
        border-left: 3px solid #0071e3;
        color: #6e6e73;
      }

      .message-time {
        font-size: 11px;
        color: #86868b;
        margin-top: 4px;
        padding: 0 4px;
      }

      .error-message {
        padding: 12px;
        background: #ffebee;
        color: #c62828;
        border-radius: 8px;
        font-size: 14px;
        animation: fadeIn 0.2s ease;
        border: 1px solid #ffcdd2;
      }

      .btn-spinner {
        display: inline-block;
        width: 16px;
        height: 16px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 0.7s linear infinite;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class ChatTabComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly markdown = inject(MarkdownService);
  protected readonly i18n = inject(I18nService);

  state = input<ChatTabState>({ provider: 'openai', model: 'gpt-4o-mini' });
  stateChange = output<ChatTabState>();

  private readonly sessionId = `chat_${Date.now()}`;

  messages = signal<
    {
      id: string;
      role: 'user' | 'assistant';
      content: string;
      timestamp: number;
    }[]
  >([]);
  input = signal('');
  isLoading = signal(false);
  error = signal<string | null>(null);

  providers = signal<ProviderInfo[]>([]);
  models = signal<ModelInfo[]>([]);
  selectedProvider = signal('openai');
  selectedModel = signal('gpt-4o-mini');
  isLoadingModels = signal(false);

  messagesEnd = viewChild<ElementRef>('messagesEnd');
  private abortController: AbortController | null = null;

  ngOnInit() {
    this.loadProviders();
  }

  ngOnDestroy() {
    if (this.abortController) {
      this.abortController.abort();
    }
  }

  loadProviders() {
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
          {
            name: 'anthropic',
            display_name: 'Anthropic Claude',
            models: ['claude-sonnet-4-20250514'],
            status: 'available',
          },
        ]);
      },
    });
  }

  loadModels(provider: string) {
    this.isLoadingModels.set(true);
    this.api.getModels(provider).subscribe({
      next: (data) => {
        this.models.set(data);
        if (data.length > 0) {
          const defaultModel =
            data.find((m) => m.name.includes('mini') || m.name.includes('3.5')) || data[0];
          this.selectedModel.set(defaultModel.name);
        }
      },
      error: () => {
        const fallback: Record<string, string[]> = {
          openai: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
          anthropic: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514'],
          ollama: ['qwen2.5:7b', 'llama3.2:3b'],
        };
        const list = fallback[provider] || fallback['openai'];
        this.models.set(list.map((name) => ({ name, provider })));
        this.selectedModel.set(list[0]);
      },
      complete: () => this.isLoadingModels.set(false),
    });
  }

  onProviderChange(provider: string) {
    this.selectedProvider.set(provider);
    this.stateChange.emit({ provider, model: this.selectedModel() });
    this.loadModels(provider);
  }

  setSelectedModel(model: string) {
    this.selectedModel.set(model);
    this.stateChange.emit({ provider: this.selectedProvider(), model });
  }

  setInput(text: string) {
    this.input.set(text);
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  send() {
    if (!this.input().trim() || this.isLoading()) return;

    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();

    const userMsg = {
      id: `user_${Date.now()}`,
      role: 'user' as const,
      content: this.input().trim(),
      timestamp: Date.now(),
    };

    this.messages.update((msgs) => [...msgs, userMsg]);
    this.input.set('');
    this.isLoading.set(true);
    this.error.set(null);

    const assistantId = `assistant_${Date.now()}`;
    this.messages.update((msgs) => [
      ...msgs,
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      },
    ]);

    const history: ChatMessage[] = this.messages()
      .filter((m) => m.role === 'user' || (m.role === 'assistant' && m.content))
      .map((m) => ({ role: m.role, content: m.content }));

    let fullContent = '';

    this.api.chatStream(
      {
        messages: history,
        session_id: this.sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
      },
      (chunk) => {
        fullContent += chunk;
        this.messages.update((msgs) =>
          msgs.map((msg) => (msg.id === assistantId ? { ...msg, content: fullContent } : msg))
        );
      },
      () => {
        this.isLoading.set(false);
        this.abortController = null;
      },
      (err) => {
        let msg = err.message;
        if (msg.includes('Failed to fetch') || msg.includes('NetworkError')) {
          msg = 'Text Service unavailable. Please ensure the service is running.';
        }
        this.error.set(msg);
        this.messages.update((msgs) =>
          msgs.map((m) => (m.id === assistantId ? { ...m, content: msg } : m))
        );
        this.isLoading.set(false);
        this.abortController = null;
      }
    );
  }

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }
}
