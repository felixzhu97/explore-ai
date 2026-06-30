import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RagService } from './rag.service';
import { MarkdownService } from '@shared/utils/markdown.service';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';

@Component({
  selector: 'app-rag-page',
  imports: [FormsModule, NxSenderComponent, NzIconModule],
  standalone: true,
  templateUrl: './rag.page.html',
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'h-full w-full flex h-screen flex-col' },
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);
  protected readonly markdown = inject(MarkdownService);

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

  setInput(text: string): void {
    this.ragService.setInput(text);
  }

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  renderMarkdown(content: string): string {
    return this.markdown.renderToString(content);
  }
}
