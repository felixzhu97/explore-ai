import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
  model,
  effect,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChatAssistantMessageComponent,
  ChatEmptyStateComponent,
  ChatUserMessageComponent,
} from '@shared/components/chat-shell';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ChatService } from '../chat.service';

@Component({
  selector: 'app-chat-tab',
  imports: [
    FormsModule,
    NxSenderComponent,
    NzIconModule,
    ChatEmptyStateComponent,
    ChatUserMessageComponent,
    ChatAssistantMessageComponent,
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
  readonly messagesEnd = viewChild<ElementRef>('messagesEnd');

  constructor() {
    effect(() => {
      this.chat.messages();
      this.scrollToBottom();
    });
  }

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

  send() {
    const text = this.input().trim();
    if (!text || this.chat.isLoading()) {
      return;
    }
    this.input.set('');
    this.chat.sendMessage(text);
  }

  private scrollToBottom() {
    const el = this.messagesEnd();
    if (el) {
      el.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }
}
