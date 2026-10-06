import {
  Component,
  inject,
  type OnInit,
  computed,
  signal,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideListChecks,
  lucidePanelLeft,
  lucidePanelLeftClose,
  lucideTrash2,
  lucideUpload,
  lucideX,
} from '@ng-icons/lucide';
import { RagService, type UploadStatus } from './rag.service';
import {
  type ChatMessageView,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
} from '../chat-shell';
import { I18nService } from '../i18n';
import { type NxPrompt } from 'ng-zorro-x/prompts';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardBadgeComponent } from '../ui/badge';
import { ZardButtonComponent } from '../ui/button';

@Component({
  selector: 'app-rag-page',
  imports: [
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
  host: { class: 'relative flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);

  /** Mobile document rail visibility; desktop rail is always shown. */
  readonly isDocumentPanelOpen = signal(false);

  readonly bubbleMessages = computed((): ChatMessageView[] => {
    return this.ragService.messages().map(message => ({
      id: message.id,
      role: message.role,
      content: message.content,
      timestamp: message.timestamp,
      sources: message.sources?.map(source => ({
        text: source.content,
        score: source.score,
        metadata: source.metadata,
      })),
      assistantIcon: 'document',
    }));
  });

  readonly footerLabels = computed(() => {
    const t = this.i18n.t().rag;
    return {
      sources: t.sources,
      similarity: t.similarity,
      basedOn: t.basedOn,
      openReference: t.openReference,
    };
  });

  readonly ragPrompts = computed((): NxPrompt[] => {
    const t = this.i18n.t().rag;
    return [
      { key: 'what', label: t.whatIsThis, description: t.askQuestion },
      { key: 'summarize', label: t.summarize, description: t.explain },
      { key: 'keyInfo', label: t.keyInfo, description: t.explain },
    ];
  });

  ngOnInit() {
    this.ragService.fetchAvailableDocuments();
  }

  /** Queues the files picked in the file input. */
  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files !== null) {
      this.ragService.onFileSelect(Array.from(files));
    }
    input.value = '';
  }

  /** Returns the upload status of a file. */
  getUploadStatus(name: string): UploadStatus | undefined {
    return this.ragService.getUploadStatus(name);
  }

  /** Removes a queued file. */
  removePendingFile(index: number): void {
    this.ragService.removePendingFile(index);
  }

  /** Uploads the queued files. */
  uploadFiles(): void {
    this.ragService.uploadFiles();
  }

  /** Deletes a document. */
  deleteDocument(documentId: string, event: Event): void {
    event.stopPropagation();
    this.ragService.deleteDocument(documentId);
  }

  /** Puts a suggested prompt in the input. */
  onPromptSelect(label: string): void {
    this.ragService.setInput(label);
  }
}
