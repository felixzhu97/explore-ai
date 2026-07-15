import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  model,
  computed,
  effect,
  ElementRef,
  viewChild,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideRefreshCw } from '@ng-icons/lucide';
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
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardSelectImports } from '@/shared/components/select/select.imports';
import { ZardSwitchComponent } from '@/shared/components/switch';
import { ChatService } from '../chat.service';

@Component({
  selector: 'app-chat-tab',
  imports: [
    FormsModule,
    NgIcon,
    NxSenderComponent,
    NzIconModule,
    ChatWelcomePanelComponent,
    ChatBubbleListComponent,
    ZardAlertComponent,
    ZardButtonComponent,
    ZardSwitchComponent,
    ...ZardSelectImports,
  ],
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
  providers: [
    provideNzIconsPatch([ArrowUpOutline]),
    provideIcons({ lucideRefreshCw }),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class ChatTabComponent implements OnInit, OnDestroy {
  protected readonly chat = inject(ChatService);
  protected readonly i18n = inject(I18nService);

  readonly input = model('');
  private readonly messageScrollEl = viewChild<ElementRef<HTMLElement>>('messageScroll');

  constructor() {
    effect(() => {
      const sessionId = this.chat.activeSessionId();
      const messageCount = this.chat.messages().length;

      untracked(() => {
        if (!sessionId && messageCount === 0) {
          return;
        }
        requestAnimationFrame(() => this.scrollMessagesToBottom());
      });
    });
  }

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
      streaming: this.chat.streamingMessageId() === message.id,
    }));
  });

  ngOnInit() {
    this.chat.loadProviders();
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

  private scrollMessagesToBottom(): void {
    const element = this.messageScrollEl()?.nativeElement;
    if (!element) {
      return;
    }
    element.scrollTop = element.scrollHeight;
    requestAnimationFrame(() => {
      element.scrollTop = element.scrollHeight;
    });
  }
}
