import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  model,
  computed,
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
import { ChatService } from '../chat.service';

@Component({
  selector: 'app-chat-tab',
  imports: [
    FormsModule,
    NxSenderComponent,
    NzIconModule,
    ChatWelcomePanelComponent,
    ChatBubbleListComponent,
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
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class ChatTabComponent implements OnInit, OnDestroy {
  protected readonly chat = inject(ChatService);
  protected readonly i18n = inject(I18nService);

  readonly input = model('');

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

  setSelectedModel(model: string) {
    this.chat.setModel(model);
  }

  onPromptSelect(label: string): void {
    this.input.set(label);
  }

  send() {
    const text = this.input().trim();
    if (!text || this.chat.isLoading()) {
      return;
    }
    this.input.set('');
    this.chat.sendMessage(text);
  }
}
