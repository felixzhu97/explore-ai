import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  OnDestroy,
  signal,
  TemplateRef,
  viewChild,
} from '@angular/core';
import {
  NxBubbleListComponent,
  NxBubbleListItem,
  NxBubbleSlotType,
} from 'ng-zorro-x/bubble';
import { MarkdownWithA2uiComponent } from '../markdown-with-a2ui.component';
import { formatMessageTime } from '../../utils/format-time.util';
import { ChatBubbleMessage, ChatBubbleSource } from './chat-bubble.model';

export interface ChatBubbleFooterLabels {
  sources: string;
  similarity: string;
  basedOn: string;
  openReference: string;
}

interface OpenSourceRef {
  messageId: string;
  index: number;
  x: number;
  y: number;
  /** Chip top / bottom in viewport — used to re-anchor after measuring panel height. */
  anchorTop: number;
  anchorBottom: number;
}

@Component({
  selector: 'app-chat-bubble-list',
  imports: [NxBubbleListComponent, MarkdownWithA2uiComponent],
  template: `
    <div class="mx-auto max-w-220">
      <nx-bubble-list [items]="bubbleItems()" [roles]="bubbleRoles" [autoScroll]="true" />
    </div>

    @if (openRef(); as ref) {
      @if (sourceAt(ref.messageId, ref.index); as source) {
        <div
          class="fixed z-80 w-80 max-w-[calc(100vw-1.5rem)] animate-in rounded-2xl border border-black/8 bg-white p-3.5 shadow-lg duration-150 fade-in-0 zoom-in-95"
          role="dialog"
          [attr.aria-label]="footerLabels().sources"
          [style.left.px]="ref.x"
          [style.top.px]="ref.y"
          data-source-popover
          (pointerdown)="$event.stopPropagation()"
          (pointerenter)="cancelCloseSourceRef()"
          (pointerleave)="scheduleCloseSourceRef()"
        >
          <div class="mb-2 flex items-center gap-1.5">
            @if (faviconUrl(source); as icon) {
              <img class="size-4 shrink-0 rounded-sm" [src]="icon" alt="" />
            } @else {
              <span
                class="flex size-4 shrink-0 items-center justify-center rounded-full bg-black/10 text-[9px] font-semibold text-text-secondary"
                aria-hidden="true"
              >
                {{ sourceInitial(source) }}
              </span>
            }
            <span class="truncate text-xs text-text-secondary">
              {{ sourceHostname(source) || sourceLabel(source) }}
            </span>
          </div>

          @if (source.url) {
            <a
              class="block text-sm leading-snug font-semibold text-text underline-offset-2 hover:underline"
              [href]="source.url"
              target="_blank"
              rel="noopener noreferrer"
              (click)="onJumpClick()"
            >
              {{ sourceTitle(source) }}
            </a>
          } @else {
            <div class="text-sm leading-snug font-semibold text-text">
              {{ sourceTitle(source) }}
            </div>
          }

          @if (sourcePublishedAt(source); as publishedAt) {
            <p class="mt-1.5 text-xs text-text-tertiary">{{ publishedAt }}</p>
          } @else if (!source.url) {
            <p class="mt-1.5 text-xs text-text-secondary">
              {{ footerLabels().similarity }}: {{ (source.score * 100).toFixed(1) }}%
            </p>
          }
        </div>
      }
    }

    <ng-template #userMessageTpl let-info="info">
      @if (messageById(messageKey(info)); as message) {
        @if (message.content) {
          <div class="wrap-break-word whitespace-pre-wrap">
            {{ userMessageText(message) }}
          </div>
          @if (isLongUserMessage(message)) {
            <button
              type="button"
              class="mt-1 border-none bg-transparent p-0 text-xs text-text-secondary underline-offset-2 hover:underline"
              (click)="toggleUserExpanded(message.id)"
            >
              {{
                isUserExpanded(message.id)
                  ? collapseLabel()
                  : expandLabel()
              }}
            </button>
          }
        }
        @if (message.images?.length) {
          <div class="mt-2 flex flex-wrap gap-2">
            @for (img of message.images; track $index) {
              <img
                [src]="img"
                alt="Uploaded image"
                class="max-h-48 max-w-48 rounded-lg object-contain"
              />
            }
          </div>
        }
      }
    </ng-template>

    <ng-template #assistantMessageTpl let-content="content" let-info="info">
      @if (messageById(messageKey(info)); as message) {
        @if (message.toolSteps?.length) {
          <div class="mb-2 flex flex-col gap-1">
            @for (step of message.toolSteps; track step.name + step.label + $index) {
              <div class="text-xs text-text-secondary">
                @if (step.status === 'running') {
                  <span>{{ step.label }}</span>
                } @else if (step.status === 'success') {
                  <span>{{ step.label }} · {{ toolStepDoneLabel() }}</span>
                } @else {
                  <span>{{ step.label }} · {{ toolStepFailedLabel() }}</span>
                }
              </div>
            }
          </div>
        }
      }
      @if (content) {
        <app-markdown-with-a2ui
          [content]="content"
          [streaming]="isStreaming(messageKey(info))"
        />
      } @else if (!messageById(messageKey(info))?.toolSteps?.length) {
        <span class="text-text-tertiary">{{ thinkingLabel() }}</span>
      }
      @if (messageById(messageKey(info)); as message) {
        @if (message.sources?.length) {
          <div
            class="mt-2 flex flex-wrap items-center gap-1.5"
            data-source-chips
            [attr.aria-label]="formatBasedOn(message.sources.length)"
          >
            @for (source of message.sources.slice(0, 5); track source.url ?? $index) {
              <button
                type="button"
                [class]="chipClass(message.id, $index)"
                [attr.aria-expanded]="isChipOpen(message.id, $index)"
                [attr.aria-label]="chipAriaLabel($index, source)"
                (pointerenter)="onChipPointerEnter($event, message.id, $index)"
                (pointerleave)="onChipPointerLeave()"
                (click)="onChipClick($event, source)"
              >
                @if (faviconUrl(source); as icon) {
                  <img
                    class="size-3.5 shrink-0 rounded-sm"
                    [class.opacity-90]="isChipHighlighted(message.id, $index)"
                    [src]="icon"
                    alt=""
                  />
                } @else {
                  <span
                    class="flex size-3.5 shrink-0 items-center justify-center rounded-full text-[8px] font-semibold"
                    [class.bg-white/20]="isChipHighlighted(message.id, $index)"
                    [class.text-background]="isChipHighlighted(message.id, $index)"
                    [class.bg-black/10]="!isChipHighlighted(message.id, $index)"
                    [class.text-text-secondary]="!isChipHighlighted(message.id, $index)"
                    aria-hidden="true"
                  >
                    {{ sourceInitial(source) }}
                  </span>
                }
                <span class="truncate">{{ sourceLabel(source) }}</span>
              </button>
            }
          </div>
        }
      }
    </ng-template>

    <ng-template #assistantFooterTpl let-info="info">
      @if (messageById(messageKey(info)); as message) {
        @if (message.timestamp) {
          <span class="text-xs text-text-tertiary">{{ formatTime(message.timestamp) }}</span>
        }
      }
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class ChatBubbleListComponent implements OnDestroy {
  readonly messages = input.required<ChatBubbleMessage[]>();
  readonly streamingMessageId = input<string | null>(null);
  readonly streamingMessageIds = input<ReadonlySet<string>>(new Set());
  readonly thinkingLabel = input('Thinking...');
  readonly collapseLongUserMessages = input(false);
  readonly expandLabel = input('Show more');
  readonly collapseLabel = input('Show less');
  readonly toolStepDoneLabel = input('Done');
  readonly toolStepFailedLabel = input('Failed');
  readonly footerLabels = input<ChatBubbleFooterLabels>({
    sources: 'Sources',
    similarity: 'Similarity',
    basedOn: 'Based on {count} source(s)',
    openReference: 'Open',
  });

  private readonly expandedUserIds = signal<ReadonlySet<string>>(new Set());
  /** Immediate chip hover highlight (no delay). */
  private readonly hoveredChip = signal<{
    messageId: string;
    index: number;
  } | null>(null);

  readonly openRef = signal<OpenSourceRef | null>(null);
  private openTimer: ReturnType<typeof setTimeout> | null = null;
  private closeTimer: ReturnType<typeof setTimeout> | null = null;

  private static readonly USER_COLLAPSE_CHARS = 160;
  private static readonly LABEL_MAX_CHARS = 14;
  private static readonly POPOVER_WIDTH = 320;
  /** First-paint estimate only; real height is measured and re-applied. */
  private static readonly POPOVER_EST_HEIGHT = 96;
  /** Keep the panel flush against the chip (no floating gap). */
  private static readonly POPOVER_GAP = 2;
  private static readonly VIEWPORT_PAD = 12;
  /** Brief hover intent delay so quick sweeps across chips do not flash the panel. */
  private static readonly OPEN_DELAY_MS = 160;
  private static readonly CLOSE_DELAY_MS = 160;

  readonly userMessageTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('userMessageTpl');

  readonly assistantMessageTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('assistantMessageTpl');

  readonly assistantFooterTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('assistantFooterTpl');

  readonly messageByIdMap = computed(() => {
    const map = new Map<string, ChatBubbleMessage>();
    for (const message of this.messages()) {
      map.set(message.id, message);
    }
    return map;
  });

  readonly bubbleItems = computed((): NxBubbleListItem[] => {
    const userTpl = this.userMessageTpl();
    const assistantTpl = this.assistantMessageTpl();
    const footerTpl = this.assistantFooterTpl();

    if (!userTpl || !assistantTpl || !footerTpl) {
      return [];
    }

    return this.messages().map((message) => {
      const isAssistant = message.role === 'assistant';
      const isStreaming = this.isStreaming(message.id);
      const hasToolSteps = Boolean(message.toolSteps?.length);

      const item: NxBubbleListItem = {
        key: message.id,
        role: message.role,
        content: message.content,
        loading: isAssistant && isStreaming && !message.content && !hasToolSteps,
        messageRender: isAssistant ? assistantTpl : userTpl,
      };

      if (isAssistant && message.timestamp) {
        item.footerRender = footerTpl;
      }

      return item;
    });
  });

  readonly bubbleRoles = {
    user: {
      placement: 'end' as const,
      variant: 'filled' as const,
      shape: 'round' as const,
      avatar: { text: 'U' },
    },
    assistant: {
      placement: 'start' as const,
      variant: 'shadow' as const,
      shape: 'round' as const,
      avatar: { text: 'AI' },
    },
  };

  ngOnDestroy(): void {
    this.cancelOpenSourceRef();
    this.cancelCloseSourceRef();
  }

  onDocumentPointerDown(event: PointerEvent): void {
    if (!this.openRef()) {
      return;
    }
    const target = event.target;
    if (!(target instanceof Element)) {
      this.closeSourceRef();
      return;
    }
    if (target.closest('[data-source-popover]') || target.closest('[data-source-chips]')) {
      return;
    }
    this.closeSourceRef();
  }

  onEscape(): void {
    this.closeSourceRef();
  }

  messageById(id: string): ChatBubbleMessage | undefined {
    return this.messageByIdMap().get(id);
  }

  messageKey(info?: { key?: string | number }): string {
    return String(info?.key ?? '');
  }

  isStreaming(messageId: string): boolean {
    const singleId = this.streamingMessageId();
    if (singleId === messageId) {
      return true;
    }
    return this.streamingMessageIds()?.has(messageId) ?? false;
  }

  formatTime(timestamp: number): string {
    return formatMessageTime(timestamp);
  }

  formatBasedOn(count: number): string {
    return this.footerLabels().basedOn.replace('{count}', `${count}`);
  }

  sourceAt(messageId: string, index: number): ChatBubbleSource | undefined {
    return this.messageById(messageId)?.sources?.[index];
  }

  sourceHostname(source: ChatBubbleSource): string {
    const raw = source.url?.trim();
    if (!raw) {
      return '';
    }
    try {
      const host = new URL(raw).hostname.toLowerCase();
      return host.startsWith('www.') ? host.slice(4) : host;
    } catch {
      return '';
    }
  }

  sourceLabel(source: ChatBubbleSource): string {
    const host = this.sourceHostname(source);
    if (host) {
      return this.truncate(host, ChatBubbleListComponent.LABEL_MAX_CHARS);
    }
    if (source.title?.trim()) {
      return this.truncate(source.title.trim(), ChatBubbleListComponent.LABEL_MAX_CHARS);
    }
    return this.truncate(source.text, ChatBubbleListComponent.LABEL_MAX_CHARS)
      || this.footerLabels().sources;
  }

  faviconUrl(source: ChatBubbleSource): string | null {
    const host = this.sourceHostname(source);
    if (!host) {
      return null;
    }
    return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=32`;
  }

  sourceInitial(source: ChatBubbleSource): string {
    const label = this.sourceLabel(source).trim();
    return (label.charAt(0) || '?').toUpperCase();
  }

  sourceTitle(source: ChatBubbleSource): string {
    if (source.title?.trim()) {
      return source.title.trim();
    }
    const host = this.sourceHostname(source);
    if (host) {
      return host;
    }
    return this.truncate(source.text, 80) || this.footerLabels().sources;
  }

  sourcePublishedAt(source: ChatBubbleSource): string {
    const direct = source.publishedAt?.trim();
    if (direct) {
      return direct;
    }
    const meta = source.metadata;
    if (!meta) {
      return '';
    }
    for (const key of ['publishedAt', 'date', 'published', 'published_at']) {
      const value = meta[key];
      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }
    return '';
  }

  truncate(text: string, max: number): string {
    if (!text) {
      return '';
    }
    return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
  }

  isChipOpen(messageId: string, index: number): boolean {
    const ref = this.openRef();
    return ref?.messageId === messageId && ref.index === index;
  }

  isChipHighlighted(messageId: string, index: number): boolean {
    const hover = this.hoveredChip();
    if (hover?.messageId === messageId && hover.index === index) {
      return true;
    }
    // Keep highlight while the delayed panel is open (e.g. pointer moved onto it).
    return this.isChipOpen(messageId, index);
  }

  chipClass(messageId: string, index: number): string {
    const base =
      'inline-flex max-w-40 cursor-pointer items-center gap-1 rounded-full px-1.5 py-0.5 text-xs transition-colors';
    if (this.isChipHighlighted(messageId, index)) {
      return `${base} bg-foreground text-background`;
    }
    return `${base} bg-black/5 text-text-secondary`;
  }

  chipAriaLabel(index: number, source: ChatBubbleSource): string {
    const title = `${index + 1}. ${this.sourceTitle(source)}`;
    if (source.url) {
      return `${title}. ${this.footerLabels().openReference}`;
    }
    return title;
  }

  onJumpClick(): void {
    this.closeSourceRef();
  }

  onChipClick(event: MouseEvent, source: ChatBubbleSource): void {
    event.stopPropagation();
    const url = source.url?.trim();
    if (!url) {
      return;
    }
    window.open(url, '_blank', 'noopener,noreferrer');
    this.closeSourceRef();
  }

  onChipPointerEnter(event: Event, messageId: string, index: number): void {
    this.hoveredChip.set({ messageId, index });
    this.scheduleShowSourceRef(event, messageId, index);
  }

  onChipPointerLeave(): void {
    this.hoveredChip.set(null);
    this.scheduleCloseSourceRef();
  }

  scheduleShowSourceRef(event: Event, messageId: string, index: number): void {
    this.cancelCloseSourceRef();
    this.cancelOpenSourceRef();
    const target = event.currentTarget;
    if (!(target instanceof HTMLElement)) {
      return;
    }
    // Switching between chips while open should feel instant; first open waits briefly.
    if (this.openRef()) {
      this.openSourceRefAt(target, messageId, index);
      return;
    }
    this.openTimer = setTimeout(() => {
      this.openTimer = null;
      this.openSourceRefAt(target, messageId, index);
    }, ChatBubbleListComponent.OPEN_DELAY_MS);
  }

  private openSourceRefAt(
    target: HTMLElement,
    messageId: string,
    index: number,
  ): void {
    const rect = target.getBoundingClientRect();
    const x = this.clampPopoverX(rect.left);
    this.openRef.set({
      messageId,
      index,
      x,
      y: this.popoverTopForHeight(
        rect.top,
        rect.bottom,
        ChatBubbleListComponent.POPOVER_EST_HEIGHT,
      ),
      anchorTop: rect.top,
      anchorBottom: rect.bottom,
    });
    // Measure after paint so the panel sits flush against the chip.
    requestAnimationFrame(() => {
      requestAnimationFrame(() => this.refinePopoverPosition());
    });
  }

  private clampPopoverX(left: number): number {
    const pad = ChatBubbleListComponent.VIEWPORT_PAD;
    return Math.min(
      Math.max(pad, left),
      window.innerWidth - ChatBubbleListComponent.POPOVER_WIDTH - pad,
    );
  }

  private popoverTopForHeight(
    anchorTop: number,
    anchorBottom: number,
    height: number,
  ): number {
    const pad = ChatBubbleListComponent.VIEWPORT_PAD;
    const gap = ChatBubbleListComponent.POPOVER_GAP;
    const spaceBelow = window.innerHeight - anchorBottom - pad;
    if (spaceBelow >= height + gap) {
      return anchorBottom + gap;
    }
    return Math.max(pad, anchorTop - height - gap);
  }

  private refinePopoverPosition(): void {
    const current = this.openRef();
    if (!current) {
      return;
    }
    const el = document.querySelector('[data-source-popover]');
    if (!(el instanceof HTMLElement)) {
      return;
    }
    const height = el.getBoundingClientRect().height;
    if (height <= 0) {
      return;
    }
    const x = this.clampPopoverX(current.x);
    const y = this.popoverTopForHeight(
      current.anchorTop,
      current.anchorBottom,
      height,
    );
    if (x !== current.x || y !== current.y) {
      this.openRef.set({ ...current, x, y });
    }
  }

  scheduleCloseSourceRef(): void {
    this.cancelOpenSourceRef();
    this.cancelCloseSourceRef();
    this.closeTimer = setTimeout(() => {
      this.closeTimer = null;
      this.openRef.set(null);
    }, ChatBubbleListComponent.CLOSE_DELAY_MS);
  }

  cancelOpenSourceRef(): void {
    if (this.openTimer !== null) {
      clearTimeout(this.openTimer);
      this.openTimer = null;
    }
  }

  cancelCloseSourceRef(): void {
    if (this.closeTimer !== null) {
      clearTimeout(this.closeTimer);
      this.closeTimer = null;
    }
  }

  closeSourceRef(): void {
    this.cancelOpenSourceRef();
    this.cancelCloseSourceRef();
    this.hoveredChip.set(null);
    this.openRef.set(null);
  }

  isLongUserMessage(message: ChatBubbleMessage): boolean {
    return (
      this.collapseLongUserMessages()
      && message.content.length > ChatBubbleListComponent.USER_COLLAPSE_CHARS
    );
  }

  isUserExpanded(messageId: string): boolean {
    return this.expandedUserIds().has(messageId);
  }

  userMessageText(message: ChatBubbleMessage): string {
    if (!this.isLongUserMessage(message) || this.isUserExpanded(message.id)) {
      return message.content;
    }
    return `${message.content.slice(0, ChatBubbleListComponent.USER_COLLAPSE_CHARS).trimEnd()}…`;
  }

  toggleUserExpanded(messageId: string): void {
    this.expandedUserIds.update((current) => {
      const next = new Set(current);
      if (next.has(messageId)) {
        next.delete(messageId);
      } else {
        next.add(messageId);
      }
      return next;
    });
  }
}
