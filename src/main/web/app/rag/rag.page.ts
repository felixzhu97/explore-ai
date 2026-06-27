import {
  Component,
  inject,
  OnInit,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService } from './rag.service';
import { MarkdownService } from '@shared/utils/markdown.service';
import { I18nService } from '@core/i18n';

interface UploadedDocument {
  id: string;
  title: string;
  status: 'uploading' | 'success' | 'error';
  progress?: number;
  error?: string;
}

@Component({
  selector: 'app-rag-page',
  imports: [CommonModule, FormsModule],
  standalone: true,
  templateUrl: './rag.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);
  protected readonly markdown = inject(MarkdownService);

  readonly fileInput = viewChild<ElementRef>('fileInput');
  readonly chatContainer = viewChild<ElementRef>('chatContainer');

  ngOnInit() {
    this.ragService.fetchAvailableDocs();
  }

  // ==================== Documents ====================

  deleteDocument(docId: string, event: Event) {
    event.stopPropagation();
    this.ragService.deleteDocument(docId);
  }

  onDocKeyDown(event: KeyboardEvent, docId: string) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.ragService.toggleDocSelection(docId);
    }
  }

  // ==================== File Upload ====================

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      this.ragService.onFileSelect(Array.from(files));
    }
    input.value = '';
  }

  getUploadStatus(filename: string): UploadedDocument | undefined {
    return this.ragService.uploadStatuses().get(filename) as UploadedDocument | undefined;
  }

  uploadFiles() {
    this.ragService.uploadFiles(() => {
      // Upload complete callback
    });
  }

  // ==================== Chat ====================

  setInput(text: string) {
    this.ragService.setInput(text);
  }

  onInputKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.ragService.sendMessage();
    }
  }

  // ==================== Utilities ====================

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  renderMarkdown(content: string): string {
    return this.markdown.renderToString(content);
  }
}
