import {
  Component,
  ElementRef,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  model,
  computed,
  signal,
  viewChild,
  effect,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import {
  ChatBubbleMessage,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
} from '../shared/components/chat-shell';
import { I18nService } from '../core/i18n';
import { FEATURE_FLAG_KEYS } from '../core/config/feature-flag-keys';
import { FeatureFlagService } from '../core/feature-flag.service';
import { SkillsService } from '../skills/skills.service';
import { NxPrompt } from 'ng-zorro-x/prompts';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardAlertComponent } from '../shared/components/alert';
import { ZardButtonComponent } from '../shared/components/button';
import { ZardSelectImports } from '../shared/components/select/select.imports';
import { ZardSwitchComponent } from '../shared/components/switch';
import { ChatService } from './chat.service';

@Component({
  selector: 'app-chat-page',
  imports: [
    FormsModule,
    RouterLink,
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
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative flex flex-1 min-h-0 w-full flex-col overflow-hidden',
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class ChatPage implements OnInit, OnDestroy {
  protected readonly chat = inject(ChatService);
  protected readonly i18n = inject(I18nService);
  private readonly featureFlags = inject(FeatureFlagService);
  private readonly skillsApi = inject(SkillsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly skillsPicker = viewChild<ElementRef<HTMLElement>>('skillsPicker');

  readonly input = model('');
  readonly skillsEnabled = signal(false);
  readonly skillsMenuOpen = signal(false);
  readonly skillsMenuStyle = signal<{ top: string; right: string } | null>(null);

  private readonly routeSessionId = toSignal(
    this.route.paramMap.pipe(map(params => params.get('sessionId'))),
    { initialValue: this.route.snapshot.paramMap.get('sessionId') },
  );

  readonly selectedSkillCount = computed(() => this.chat.selectedSkillIds().length);

  readonly skillsTriggerLabel = computed(() => {
    const count = this.selectedSkillCount();
    if (count === 0) {
      return this.i18n.t().chat.skills;
    }
    return this.i18n.tReplace(this.i18n.t().chat.skillsSelected, { count });
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
        publishedAt: source.publishedAt,
      })),
    }));
  });

  readonly footerLabels = computed(() => {
    const t = this.i18n.t().ragChat;
    return {
      sources: t.sources,
      similarity: t.similarity,
      basedOn: t.basedOn,
      openReference: t.openReference,
    };
  });

  constructor() {
    effect(() => {
      const sessionId = this.routeSessionId();
      if (!sessionId || !this.chat.sessionsReady()) {
        return;
      }
      // Missing session → ChatService redirects to `/chat` (no toast).
      this.chat.selectSession(sessionId);
    });
  }

  ngOnInit() {
    const legacySession = this.route.snapshot.queryParamMap.get('session');
    if (legacySession) {
      void this.router.navigate(['/chat', legacySession], { replaceUrl: true });
    }

    this.chat.loadProviders();
    const skillsOn = this.featureFlags.isEnabled(FEATURE_FLAG_KEYS.MODULE_SKILLS);
    this.skillsEnabled.set(skillsOn);
    if (skillsOn) {
      this.skillsApi.listEnabled().subscribe({
        next: (skills) => {
          const options = skills.map(skill => ({
            id: skill.id,
            name: skill.name,
          }));
          this.chat.setAvailableSkills(options);
        },
        error: () => this.chat.setAvailableSkills([]),
      });
    }
  }

  ngOnDestroy() {
    this.chat.abortStream();
  }

  toggleSkillsMenu(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const willOpen = !this.skillsMenuOpen();
    if (willOpen) {
      this.positionSkillsMenu();
    } else {
      this.skillsMenuStyle.set(null);
    }
    this.skillsMenuOpen.set(willOpen);
  }

  onDocumentClick(event: MouseEvent): void {
    const root = this.skillsPicker()?.nativeElement;
    if (root?.contains(event.target as Node)) {
      return;
    }
    this.skillsMenuOpen.set(false);
    this.skillsMenuStyle.set(null);
  }

  onSkillToggle(event: MouseEvent, skillId: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.chat.toggleSkillId(skillId);
  }

  private positionSkillsMenu(): void {
    const root = this.skillsPicker()?.nativeElement;
    if (!root) {
      return;
    }
    const rect = root.getBoundingClientRect();
    this.skillsMenuStyle.set({
      top: `${Math.round(rect.bottom + 4)}px`,
      right: `${Math.round(window.innerWidth - rect.right)}px`,
    });
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

  send() {
    const text = this.input().trim();
    if (!text || this.chat.isLoading()) {
      return;
    }
    if (!this.chat.isSelectedProviderAvailable()) {
      this.chat.sendMessage(text);
      return;
    }
    this.input.set('');
    this.chat.sendMessage(text);
  }
}
