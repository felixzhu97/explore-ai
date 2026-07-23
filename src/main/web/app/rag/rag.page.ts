import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  computed,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideImage, lucideListChecks, lucideTrash2, lucideUpload, lucideX } from '@ng-icons/lucide';
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
    provideIcons({ lucideListChecks, lucideX, lucideImage, lucideUpload, lucideTrash2 }),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);

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
      images: message.images,
      sources: message.sources,
      sourcesExpanded: this.ragService.expandedSources().has(message.id),
      assistantIcon: 'document',
    }));
  });

  readonly footerLabels = computed(() => {
    const t = this.i18n.t().ragChat;
    return {
      sources: t.sources,
      similarity: t.similarity,
      basedOn: t.basedOn,
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

  onImageSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      Array.from(files).forEach((file) => {
        if (file.type.startsWith('image/')) {
          this.fileToBase64(file).then((base64) => {
            this.ragService.addImage(base64);
          });
        }
      });
    }
    input.value = '';
  }

  removeImage(index: number): void {
    this.ragService.removeImage(index);
  }

  private async fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  onPromptSelect(label: string): void {
    this.ragService.setInput(label);
  }

  onToggleSources(messageId: string): void {
    this.ragService.toggleSources(messageId);
  }
}
