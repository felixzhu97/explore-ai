import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  model,
  computed,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideRefreshCw } from '@ng-icons/lucide';
import {
  buildSenderActionGroups,
  ChatBubbleMessage,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
  ToolsCatalogService,
  composeToolAwareQuery,
  type SenderActionGroup,
  type SenderActionItem,
  type ToolCatalogEntryDto,
} from '../shared/components/chat-shell';
import { I18nService } from '../core/i18n';
import { NxPrompt } from 'ng-zorro-x/prompts';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardAlertComponent } from '../shared/components/alert';
import { ZardButtonComponent } from '../shared/components/button';
import { ZardSelectImports } from '../shared/components/select/select.imports';
import { ZardSwitchComponent } from '../shared/components/switch';
import { ChatService } from './chat.service';
import { AgentsService } from '../agents/agents.service';
import type { AgentInfo } from '../agents/agents.model';
import { FeatureFlagService } from '../core/feature-flag.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-chat-page',
  imports: [
    FormsModule,
    RouterLink,
    NgIcon,
    NzIconModule,
    ChatMessagePaneComponent,
    ChatSenderBarComponent,
    ZardAlertComponent,
    ZardButtonComponent,
    ZardSwitchComponent,
    ...ZardSelectImports,
  ],
  templateUrl: './chat.page.html',
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
  providers: [
    provideNzIconsPatch([ArrowUpOutline]),
    provideIcons({ lucideRefreshCw }),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class ChatPage implements OnInit, OnDestroy {
  protected readonly chat = inject(ChatService);
  protected readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly agentsApi = inject(AgentsService);
  private readonly toolsCatalog = inject(ToolsCatalogService);
  private readonly featureFlags = inject(FeatureFlagService);

  readonly input = model('');
  readonly selectedTool = model<SenderActionItem | null>(null);
  private readonly tools = signal<ToolCatalogEntryDto[]>([]);
  private readonly agents = signal<AgentInfo[]>([]);

  readonly actionGroups = computed((): SenderActionGroup[] => {
    return buildSenderActionGroups({
      t: this.i18n.t(),
      tools: this.tools(),
      agents: this.agents(),
      featureFlags: this.featureFlags,
      scope: 'full',
    });
  });

  readonly chatPrompts = computed((): NxPrompt[] => {
    return this.i18n.t().chat.suggestedPrompts.map(prompt => ({
      key: prompt.key,
      label: prompt.label,
      description: prompt.description,
    }));
  });

  readonly bubbleMessages = computed((): ChatBubbleMessage[] => {
    return this.chat.messages().map(message => ({
      id: message.id,
      role: message.role,
      content: message.content,
      timestamp: message.timestamp,
      toolSteps: message.toolSteps,
      sources: message.sources?.map(source => ({
        text: source.snippet,
        score: 1,
        url: source.url,
        title: source.title,
      })),
      sourcesExpanded: Boolean(message.sources?.length),
    }));
  });

  ngOnInit() {
    this.chat.loadProviders();
    forkJoin({
      tools: this.toolsCatalog.listCatalog().pipe(catchError(() => of([]))),
      agents: this.agentsApi.listAgents().pipe(catchError(() => of([]))),
    }).subscribe(({ tools, agents }) => {
      this.tools.set(tools);
      this.agents.set(agents);
    });
  }

  ngOnDestroy() {
    this.chat.abortStream();
  }

  newChat(): void {
    this.chat.createSession();
  }

  onProviderChange(provider: string) {
    this.chat.setProvider(provider);
  }

  setSelectedModel(modelName: string) {
    this.chat.setModel(modelName);
  }

  onPromptSelect(label: string): void {
    this.input.set(label);
  }

  onSenderAction(action: SenderActionItem): void {
    switch (action.kind) {
      case 'tool':
        this.chat.setToolsEnabled(true);
        this.selectedTool.set(action);
        break;
      case 'agent':
        // Only the explicit "open pipeline" action navigates; workers use a chip.
        if (action.id === 'agent:open' && action.path) {
          void this.router.navigateByUrl(action.path);
          break;
        }
        this.selectedTool.set(action);
        break;
      case 'navigate':
        if (action.path) {
          void this.router.navigateByUrl(action.path);
        }
        break;
      case 'session':
        if (action.id === 'session:newChat') {
          this.newChat();
        } else if (action.id === 'session:toggleTools') {
          this.chat.setToolsEnabled(!this.chat.toolsEnabled());
        }
        break;
      default:
        break;
    }
  }

  send() {
    const text = this.input().trim();
    if (!text || this.chat.isLoading()) {
      return;
    }
    const tool = this.selectedTool();
    if (tool?.kind === 'tool') {
      this.chat.setToolsEnabled(true);
    }
    const streamContent = tool?.kind === 'tool'
      ? composeToolAwareQuery(text, tool.label, this.i18n.t().sender.toolIntent)
      : text;
    if (!this.chat.isSelectedProviderAvailable()) {
      this.chat.sendMessage(text, { streamContent });
      return;
    }
    this.input.set('');
    this.selectedTool.set(null);
    this.chat.sendMessage(text, { streamContent });
  }
}
