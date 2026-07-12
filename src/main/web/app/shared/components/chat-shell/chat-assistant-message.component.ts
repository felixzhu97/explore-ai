import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MarkdownContentComponent } from '@shared/components/markdown-content.component';
import { formatMessageTime } from '@shared/utils/format-time.util';

@Component({
  selector: 'app-chat-assistant-message',
  imports: [MarkdownContentComponent],
  template: `
    <div class="flex flex-row items-start gap-3">
      <div class="flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-secondary text-text-secondary">
        @if (icon() === 'document') {
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 12-2V8z" />
            <polyline points="14,2 14,8 20,8" />
          </svg>
        } @else {
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 8V4H8" />
            <rect width="16" height="12" x="4" y="8" rx="2" />
            <path d="M2 14h2" />
            <path d="M20 14h2" />
            <path d="M15 13v2" />
            <path d="M9 13v2" />
          </svg>
        }
      </div>
      <div class="flex max-w-[85%] flex-col items-start gap-1">
        <div class="rounded-2xl bg-white px-4 py-3 text-base leading-relaxed wrap-break-word text-text shadow-sm">
          @if (content()) {
            <app-markdown-content
              [content]="content()"
              [streaming]="streaming()"
            />
          } @else {
            <span class="text-text-tertiary">{{ thinkingLabel() }}</span>
          }
        </div>
        <span class="text-xs text-text-tertiary">{{ formattedTime() }}</span>
        <ng-content />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatAssistantMessageComponent {
  readonly content = input.required<string>();
  readonly timestamp = input.required<number>();
  readonly streaming = input(false);
  readonly thinkingLabel = input('Thinking...');
  readonly icon = input<'chat' | 'document'>('chat');

  readonly formattedTime = computed(() => formatMessageTime(this.timestamp()));
}
