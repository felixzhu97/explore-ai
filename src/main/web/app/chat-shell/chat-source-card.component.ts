import { Component, computed, input, output } from '@angular/core';
import type { ChatSourceView } from './chat-bubble-list.component';
import {
  sourceFaviconUrl,
  sourceHostname,
  sourceInitial,
  sourceLabel,
  sourcePublishedAt,
  sourceTitle,
} from './chat-source.util';

@Component({
  selector: 'app-chat-source-card',
  template: `
    <div class="mb-2 flex items-center gap-1.5">
      @let icon = faviconUrl();
      @if (icon !== null) {
        <img class="size-4 shrink-0 rounded-sm" alt="" [src]="icon" />
      } @else {
        <span
          class="flex size-4 shrink-0 items-center justify-center rounded-full bg-black/10 text-[9px] font-semibold text-text-secondary"
          aria-hidden="true"
        >
          {{ initial() }}
        </span>
      }
      <span class="truncate text-xs text-text-secondary">
        {{ hostname() !== '' ? hostname() : label() }}
      </span>
    </div>

    @let url = source().url;
    @if (url !== undefined && url !== '') {
      <a
        class="block text-sm leading-snug font-semibold text-text underline-offset-2 hover:underline"
        target="_blank"
        rel="noopener noreferrer"
        [href]="url"
        (click)="jump.emit()"
      >
        {{ title() }}
      </a>
    } @else {
      <div class="text-sm leading-snug font-semibold text-text">
        {{ title() }}
      </div>
    }

    @let date = publishedAt();
    @if (date !== '') {
      <p class="mt-1.5 text-xs text-text-tertiary">{{ date }}</p>
    } @else if (url === undefined || url === '') {
      <p class="mt-1.5 text-xs text-text-secondary">
        {{ similarityLabel() }}: {{ (source().score * 100).toFixed(1) }}%
      </p>
    }
  `,
  host: { class: 'block' },
})
export class ChatSourceCardComponent {
  readonly source = input.required<ChatSourceView>();

  /** Shown when the source has no hostname, title or text. */
  readonly fallbackLabel = input('Sources');

  readonly similarityLabel = input('Similarity');

  /** Emits when the reader follows the source link. */
  readonly jump = output<void>();

  readonly faviconUrl = computed(() => sourceFaviconUrl(this.source()));
  readonly initial = computed(() => sourceInitial(this.source(), this.fallbackLabel()));
  readonly hostname = computed(() => sourceHostname(this.source()));
  readonly label = computed(() => sourceLabel(this.source(), this.fallbackLabel()));

  readonly title = computed(() => sourceTitle(this.source(), this.fallbackLabel()));

  readonly publishedAt = computed(() => sourcePublishedAt(this.source()));
}
