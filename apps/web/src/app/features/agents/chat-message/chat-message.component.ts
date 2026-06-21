import { Component, ChangeDetectionStrategy, input, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ToolResultComponent } from '@features/agents/tool-result/tool-result.component';
import { MarkdownService } from '@shared/utils/markdown.service';
import type { ChatMessageData } from '../models/agent.model';
export type { ChatMessageData };

@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [ToolResultComponent],
  templateUrl: './chat-message.component.html',
  styleUrl: './chat-message.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatMessageComponent {
  message = input.required<ChatMessageData>();

  private readonly markdownService = inject(MarkdownService);
  private readonly sanitizer = inject(DomSanitizer);

  isUser = computed(() => this.message().role === 'user');

  formattedTime = computed(() => {
    return new Date(this.message().timestamp).toLocaleTimeString();
  });

  renderedContent = computed(() => {
    const content = this.message().content;
    if (!content) return '';

    const trimmed = content.trim();

    if (this.markdownService.isRawJson(trimmed)) {
      try {
        const parsed = JSON.parse(trimmed);
        return this.sanitizer.bypassSecurityTrustHtml(
          this.markdownService.highlightJson(JSON.stringify(parsed, null, 2))
        );
      } catch {
        // Not valid JSON, fall through
      }
    }

    return this.markdownService.processContent(content);
  });
}
