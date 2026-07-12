import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  ElementRef,
  viewChild,
  effect,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RagService } from './rag.service';
import {
  ChatAssistantMessageComponent,
  ChatEmptyStateComponent,
  ChatUserMessageComponent,
} from '@shared/components/chat-shell';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';

@Component({
  selector: 'app-rag-page',
  imports: [
    FormsModule,
    NxSenderComponent,
    NzIconModule,
    ChatEmptyStateComponent,
    ChatUserMessageComponent,
    ChatAssistantMessageComponent,
  ],
  templateUrl: './rag.page.html',
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);

  readonly messagesEnd = viewChild<ElementRef>('messagesEnd');

  constructor() {
    effect(() => {
      this.ragService.messages();
      this.scrollToBottom();
    });
  }

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

  setInput(text: string): void {
    this.ragService.setInput(text);
  }

  private scrollToBottom(): void {
    const end = this.messagesEnd()?.nativeElement;
    end?.scrollIntoView({ behavior: 'smooth' });
  }
}
