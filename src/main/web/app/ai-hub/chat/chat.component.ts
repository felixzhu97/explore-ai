import {
  Component,
  signal,
  inject,
  OnInit,
  OnDestroy,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
  model,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { MarkdownService } from '@shared/utils/markdown.service';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import type { ChatMessage, ProviderInfo, ModelInfo, ChatTabState } from '../chat.model';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { SidebarService } from '../../layout/sidebar.service';

@Component({
  selector: 'app-chat-tab',
  imports: [FormsModule, NxSenderComponent, NzIconModule],
  standalone: true,
  templateUrl: './chat.component.html',
  styles: [
    `
      @keyframes fade-in {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex h-screen flex-col' },
})
export class ChatTabComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly markdown = inject(MarkdownService);
  protected readonly i18n = inject(I18nService);

  readonly state = model<ChatTabState>({ provider: 'openai', model: 'gpt-4o-mini' });

  private readonly sessionId = `chat_${Date.now()}`;

  readonly messages = signal<
    {
      id: string;
      role: 'user' | 'assistant';
      content: string;
      timestamp: number;
    }[]
  >([]);

  readonly input = signal('');
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  readonly providers = signal<ProviderInfo[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly selectedProvider = signal('openai');
  readonly selectedModel = signal('gpt-4o-mini');
  readonly isLoadingModels = signal(false);

  private readonly sidebar = inject(SidebarService);

  readonly messagesEnd = viewChild<ElementRef>('messagesEnd');

  newChat(): void {
    this.messages.set([]);
    this.error.set(null);
    this.sidebar.addSession();
  }

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
            data.find(m => m.name.includes('mini') || m.name.includes('3.5')) || data[0];
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
        this.models.set(list.map(name => ({ name, provider })));
        this.selectedModel.set(list[0]);
      },
      complete: () => this.isLoadingModels.set(false),
    });
  }

  private scrollToBottom() {
    const el = this.messagesEnd();
    if (el) {
      el.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }

  onProviderChange(provider: string) {
    this.selectedProvider.set(provider);
    this.loadModels(provider);
  }

  setSelectedModel(model: string) {
    this.selectedModel.set(model);
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

    this.messages.update(msgs => [...msgs, userMsg]);
    this.input.set('');
    this.isLoading.set(true);
    this.error.set(null);

    const assistantId = `assistant_${Date.now()}`;
    this.messages.update(msgs => [
      ...msgs,
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      },
    ]);

    const history: ChatMessage[] = this.messages()
      .filter(m => m.role === 'user' || (m.role === 'assistant' && m.content))
      .map(m => ({ role: m.role, content: m.content }));

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
        this.messages.update((msgs) => {
          return msgs.map((msg) => {
            return msg.id === assistantId
              ? { ...msg, content: fullContent }
              : msg;
          });
        });
        this.scrollToBottom();
      },
      () => {
        this.isLoading.set(false);
        this.abortController = null;
        this.scrollToBottom();
      },
      (err) => {
        let msg = err.message;
        if (msg.includes('Failed to fetch') || msg.includes('NetworkError')) {
          msg =
            'Text Service unavailable. Please ensure the service is running.';
        }
        this.error.set(msg);
        this.messages.update((msgs) => {
          return msgs.map((m) => {
            return m.id === assistantId ? { ...m, content: msg } : m;
          });
        });
        this.isLoading.set(false);
        this.abortController = null;
        this.scrollToBottom();
      },
    );
  }

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }
}
