import { Component, ChangeDetectionStrategy, input, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ToolResultComponent } from './tool-result/tool-result.component';
import { MarkdownService } from '@shared/utils/markdown.service';
import type { ChatMessageData } from '../agent.model';
export type { ChatMessageData };

@Component({
  selector: 'app-chat-message',
  imports: [ToolResultComponent],
  standalone: true,
  template: `
    <div class="
      flex max-w-[80%] animate-fade-in flex-col items-start
      self-start
    " [class]="isUser() ? 'items-end self-end' : 'items-start self-start'">
      <div class="rounded-xl px-4 py-3 text-sm leading-relaxed wrap-break-word"
           [class]="isUser()
           ? 'rounded-br-sm bg-primary text-white'
           : 'rounded-bl-sm border border-border bg-surface text-text'">
        @if (isUser()) {
          <p class="m-0">{{ message().content }}</p>
        } @else {
          <div [innerHTML]="renderedContent()"></div>
        }
      </div>

      @if (message().toolCalls && message().toolCalls!.length > 0) {
        <div class="mt-2 w-full max-w-2xl">
          @for (toolCall of message().toolCalls; track toolCall.id) {
            <app-tool-result [toolCall]="toolCall" />
          }
        </div>
      }

      <div class="mt-1 flex items-center gap-2 px-1">
        <span class="text-xs text-text-tertiary">{{ formattedTime() }}</span>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatMessageComponent {
  readonly message = input.required<ChatMessageData>();

  private readonly markdownService = inject(MarkdownService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly isUser = computed(() => this.message().role === 'user');

  readonly formattedTime = computed(() => {
    return new Date(this.message().timestamp).toLocaleTimeString();
  });

  readonly renderedContent = computed(() => {
    const content = this.message().content;
    if (!content) return '';

    const trimmed = content.trim();

    if (this.markdownService.isRawJson(trimmed)) {
      try {
        const parsed = JSON.parse(trimmed);
        return this.sanitizer.bypassSecurityTrustHtml(
          this.markdownService.highlightJson(JSON.stringify(parsed, null, 2)),
        );
      } catch {
        // Not valid JSON, fall through
      }
    }

    return this.markdownService.processContent(content);
  });
}
