import { Component, ChangeDetectionStrategy, input, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ToolResultComponent } from '../tool-result/tool-result.component';
import { MarkdownService } from '@shared/utils/markdown.service';
import type { ChatMessageData } from '../models/agent.model';
export type { ChatMessageData };

@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [ToolResultComponent],
  template: `
    <div class="flex flex-col max-w-[80%] self-start items-start animate-[fadeIn_0.2s_ease]" [class]="isUser() ? 'self-end items-end' : 'self-start items-start'">
      <div class="px-4 py-3 text-sm leading-relaxed break-words rounded-xl" [class]="isUser() ? 'bg-primary text-white rounded-br-sm' : 'bg-surface border border-[--color-border] text-text rounded-bl-sm'">
        @if (isUser()) {
          <p class="m-0">{{ message().content }}</p>
        } @else {
          <div [innerHTML]="renderedContent()"></div>
        }
      </div>

      @if (message().toolCalls && message().toolCalls!.length > 0) {
        <div class="mt-2 w-full max-w-[500px]">
          @for (toolCall of message().toolCalls; track toolCall.id) {
            <app-tool-result [toolCall]="toolCall" />
          }
        </div>
      }

      <div class="flex items-center gap-2 mt-1 px-1">
        <span class="text-[11px] text-text-tertiary">{{ formattedTime() }}</span>
      </div>
    </div>
  `,
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
