import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  model,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChatBubbleListComponent,
  ChatBubbleMessage,
  ChatWelcomePanelComponent,
} from '@shared/components/chat-shell';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import { NxPrompt } from 'ng-zorro-x/prompts';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardSelectImports } from '@/shared/components/select/select.imports';
import { AgentsService } from './agents.service';
import type { AgentInfo, AgentQuickPromptKey } from './agents.model';

@Component({
  selector: 'app-agents-page',
  imports: [
    FormsModule,
    NxSenderComponent,
    NzIconModule,
    ChatWelcomePanelComponent,
    ChatBubbleListComponent,
    ZardAlertComponent,
    ...ZardSelectImports,
  ],
  templateUrl: './agents.page.html',
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AgentsPageComponent implements OnInit, OnDestroy {
  private readonly agentsApi = inject(AgentsService);
  readonly i18n = inject(I18nService);

  readonly agents = signal<AgentInfo[]>([]);
  readonly selectedAgentType = signal('supervisor');
  readonly messages = signal<ChatBubbleMessage[]>([]);
  readonly streamingMessageId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly input = model('');

  private streamAbort: (() => void) | null = null;
  private messageSeq = 0;

  readonly selectedAgent = computed(() => {
    const type = this.selectedAgentType();
    return this.agents().find(agent => agent.type === type) ?? null;
  });

  readonly quickPrompts = computed((): NxPrompt[] => {
    const type = this.selectedAgentType() as AgentQuickPromptKey;
    const prompts = this.i18n.t().agents.quickPrompts[type]
      ?? this.i18n.t().agents.quickPrompts.supervisor;
    return prompts.map((label, index) => ({
      key: `${type}-${index}`,
      label,
      description: '',
    }));
  });

  ngOnInit(): void {
    this.agentsApi.listAgents().subscribe({
      next: (agents) => {
        this.agents.set(agents);
        const selected = this.selectedAgentType();
        if (!agents.some(a => a.type === selected) && agents.length > 0) {
          this.selectedAgentType.set(agents[0].type);
        }
      },
      error: () => this.error.set(this.i18n.t().agents.errorMessage),
    });
  }

  ngOnDestroy(): void {
    this.streamAbort?.();
  }

  onAgentChange(type: string): void {
    this.selectedAgentType.set(type);
    this.messages.set([]);
    this.error.set(null);
  }

  onPromptSelect(prompt: NxPrompt): void {
    this.input.set(prompt.label ?? '');
    this.send();
  }

  send(): void {
    const message = this.input().trim();
    if (!message || this.loading()) {
      return;
    }

    this.streamAbort?.();
    this.error.set(null);
    this.input.set('');
    this.loading.set(true);

    const userId = this.nextId('user');
    const assistantId = this.nextId('assistant');

    this.messages.update(msgs => [
      ...msgs,
      { id: userId, role: 'user', content: message, timestamp: Date.now() },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        streaming: true,
      },
    ]);
    this.streamingMessageId.set(assistantId);

    let fullContent = '';
    const { abort } = this.agentsApi.invokeStream(
      this.selectedAgentType(),
      { message, agentType: this.selectedAgentType() },
      (chunk) => {
        fullContent += chunk;
        this.patchAssistant(assistantId, fullContent, true);
      },
      (handoff) => {
        try {
          const parsed = JSON.parse(handoff) as { agentType?: string; reason?: string };
          if (parsed.agentType) {
            const note = `\n_Delegated to **${parsed.agentType}**_\n\n`;
            fullContent += note;
            this.patchAssistant(assistantId, fullContent, true);
          }
        } catch {
          // ignore malformed handoff payloads
        }
      },
      () => {
        const content = fullContent || this.i18n.t().agents.thinking;
        this.patchAssistant(assistantId, content, false);
        this.streamingMessageId.set(null);
        this.loading.set(false);
        this.streamAbort = null;
      },
      (err) => {
        this.error.set(err.message || this.i18n.t().agents.errorMessage);
        this.patchAssistant(
          assistantId,
          fullContent || this.i18n.t().agents.errorMessage,
          false,
        );
        this.streamingMessageId.set(null);
        this.loading.set(false);
        this.streamAbort = null;
      },
    );
    this.streamAbort = abort;
  }

  private patchAssistant(
    id: string,
    content: string,
    streaming: boolean,
  ): void {
    this.messages.update((msgs) => {
      return msgs.map((msg) => {
        if (msg.id !== id) {
          return msg;
        }
        return { ...msg, content, streaming };
      });
    });
  }

  private nextId(prefix: string): string {
    this.messageSeq += 1;
    return `${prefix}-${this.messageSeq}-${Date.now()}`;
  }
}
