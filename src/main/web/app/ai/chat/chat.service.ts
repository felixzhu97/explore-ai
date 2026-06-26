import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '@core/services/api.service';
import type {
  ChatMessage,
  ProviderInfo,
  ModelInfo,
} from './chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly api = inject(ApiService);

  providers = signal<ProviderInfo[]>([]);
  models = signal<ModelInfo[]>([]);
  selectedProvider = signal('openai');
  selectedModel = signal('gpt-4o-mini');
  isLoadingModels = signal(false);

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

  loadModels(provider: string): void {
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

  setProvider(provider: string): void {
    this.selectedProvider.set(provider);
    this.loadModels(provider);
  }

  setModel(model: string): void {
    this.selectedModel.set(model);
  }

  chatStream(
    messages: ChatMessage[],
    sessionId: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): void {
    this.api.chatStream(
      {
        messages,
        session_id: sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
      },
      onChunk,
      onComplete,
      onError
    );
  }
}
