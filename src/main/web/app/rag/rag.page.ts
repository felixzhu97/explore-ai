import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  computed,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideListChecks,
  lucidePanelLeft,
  lucidePanelLeftClose,
  lucideTrash2,
  lucideUpload,
  lucideX,
} from '@ng-icons/lucide';
import { RagService, UploadStatus } from './rag.service';
import {
  ChatBubbleMessage,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
} from '../shared/components/chat-shell';
import { I18nService } from '../core/i18n';
import { NxPrompt } from 'ng-zorro-x/prompts';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardBadgeComponent } from '../shared/components/badge';
import { ZardButtonComponent } from '../shared/components/button';

@Component({
  selector: 'app-rag-page',
  imports: [
    FormsModule,
    NgIcon,
    NzIconModule,
    ChatMessagePaneComponent,
    ChatSenderBarComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
  ],
  templateUrl: './rag.page.html',
  providers: [
    provideNzIconsPatch([ArrowUpOutline]),
    provideIcons({
      lucideListChecks,
      lucideX,
      lucideUpload,
      lucideTrash2,
      lucidePanelLeft,
      lucidePanelLeftClose,
    }),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'relative flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);
  /** Mobile document rail visibility; desktop rail is always shown. */
  readonly docsOpen = signal(false);

  readonly ragPrompts = computed((): NxPrompt[] => {
    const t = this.i18n.t().ragChat;
    return [
      { key: 'what', label: t.whatIsThis, description: t.askQuestion },
      { key: 'summarize', label: t.summarize, description: t.explain },
      { key: 'keyInfo', label: t.keyInfo, description: t.explain },
    ];
  });

  readonly bubbleMessages = computed((): ChatBubbleMessage[] => {
    return this.ragService.messages().map(message => ({
      id: message.id,
      role: message.role,
      content: message.content,
      timestamp: message.timestamp,
      sources: message.sources,
      assistantIcon: 'document',
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

  ngOnInit() {
    this.ragService.fetchAvailableDocs();
  }

  deleteDocument(docId: string, event: Event): void {
    event.stopPropagation();
    this.ragService.deleteDocument(docId);
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      this.ragService.onFileSelect(Array.from(files));
    }
    input.value = '';
  }

  removePendingFile(index: number): void {
    this.ragService.removePendingFile(index);
  }

  uploadFiles(): void {
    this.ragService.uploadFiles();
  }

  getUploadStatus(name: string): UploadStatus | undefined {
    return this.ragService.getUploadStatus(name);
  }

  onPromptSelect(label: string): void {
    this.ragService.setInput(label);
  }
}
