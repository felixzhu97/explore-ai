import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  computed,
  model,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideImage,
  lucideListChecks,
  lucidePanelLeft,
  lucidePanelLeftClose,
  lucideTrash2,
  lucideUpload,
  lucideX,
} from '@ng-icons/lucide';
import { RagService, UploadStatus } from './rag.service';
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
import { ZardBadgeComponent } from '../shared/components/badge';
import { ZardButtonComponent } from '../shared/components/button';
import { Router } from '@angular/router';
import { FeatureFlagService } from '../core/feature-flag.service';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-rag-page',
  imports: [
    FormsModule,
    RouterLink,
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
      lucideImage,
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
  private readonly router = inject(Router);
  private readonly toolsCatalog = inject(ToolsCatalogService);
  private readonly featureFlags = inject(FeatureFlagService);
  /** Mobile document rail visibility; desktop rail is always shown. */
  readonly docsOpen = signal(false);
  readonly selectedTool = model<SenderActionItem | null>(null);
  private readonly tools = signal<ToolCatalogEntryDto[]>([]);

  readonly actionGroups = computed((): SenderActionGroup[] => {
    return buildSenderActionGroups({
      t: this.i18n.t(),
      tools: this.tools(),
      agents: [],
      featureFlags: this.featureFlags,
      scope: 'rag',
    });
  });

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
    this.toolsCatalog.listCatalog().pipe(catchError(() => of([]))).subscribe((tools) => {
      this.tools.set(tools);
    });
  }

  onSenderAction(action: SenderActionItem): void {
    if (action.kind === 'tool') {
      this.selectedTool.set(action);
      return;
    }
    if (action.kind === 'agent') {
      if (action.id === 'agent:open' && action.path) {
        void this.router.navigateByUrl(action.path);
        return;
      }
      this.selectedTool.set(action);
      return;
    }
    if (action.kind === 'navigate' && action.path) {
      void this.router.navigateByUrl(action.path);
    }
  }

  sendWithSelectedTool(): void {
    const text = this.ragService.input().trim();
    if (!text && this.ragService.pendingImages().length === 0) {
      return;
    }
    if (this.ragService.isLoading()) {
      return;
    }
    const tool = this.selectedTool();
    const streamQuery = tool?.kind === 'tool'
      ? composeToolAwareQuery(text, tool.label, this.i18n.t().sender.toolIntent)
      : undefined;
    this.selectedTool.set(null);
    void this.ragService.sendMessage(streamQuery ? { streamQuery } : undefined);
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
