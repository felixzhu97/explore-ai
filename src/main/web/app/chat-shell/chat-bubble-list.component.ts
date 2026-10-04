import {
  Component,
  computed,
  input,
  type OnDestroy,
  signal,
  type TemplateRef,
  viewChild,
} from '@angular/core';
import {
  NxBubbleListComponent,
  type NxBubbleListItem,
  type NxBubbleSlotType,
} from 'ng-zorro-x/bubble';
import { MarkdownWithA2uiComponent } from './markdown-with-a2ui.component';
import { ChatSourceCardComponent } from './chat-source-card.component';
import {
  sourceFaviconUrl,
  sourceInitial,
  sourceLabel,
  sourceTitle,
} from './chat-source.util';
import { ChatToolStepsComponent } from './chat-tool-steps.component';
import type { Instant } from '@js-joda/core';
import { InstantPipe } from '../time/instant.pipe';

export interface ChatSourceView {
  text: string;
  score: number;
  url?: string;
  title?: string;
  /** Publisher date when known (e.g. Serper organic `date`). */
  publishedAt?: string | undefined;
  metadata?: Record<string, unknown>;
}

export interface ToolStep {
  name: string;
  label: string;
  status: 'running' | 'success' | 'error';
}

export interface ChatMessageView {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp?: Instant;
  images?: string[];
  streaming?: boolean;
  sources?: ChatSourceView[] | undefined;
  toolSteps?: ToolStep[] | undefined;
  assistantIcon?: 'chat' | 'document';
}

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

const USER_COLLAPSE_CHARS = 160;
const POPOVER_WIDTH = 320;
/** First-paint estimate only; real height is measured and re-applied. */
const POPOVER_EST_HEIGHT = 96;
/** Keep the panel flush against the chip (no floating gap). */
const POPOVER_GAP = 2;
const VIEWPORT_PAD = 12;
/** Brief hover intent delay so quick sweeps across chips do not flash the panel. */
const OPEN_DELAY_MS = 160;
const CLOSE_DELAY_MS = 160;

@Component({
  selector: 'app-chat-bubble-list',
  imports: [
    NxBubbleListComponent,
    MarkdownWithA2uiComponent,
    ChatSourceCardComponent,
    ChatToolStepsComponent,
    InstantPipe,
  ],
  template: `
    <div class="mx-auto max-w-220">
      <nx-bubble-list [items]="bubbleItems()" [roles]="bubbleRoles" [autoScroll]="true" />
    </div>

    @if (openRef(); as ref) {
      @if (sourceAt(ref.messageId, ref.index); as source) {
        <div
          class="fixed z-80 w-80 max-w-[calc(100vw-1.5rem)] animate-in rounded-2xl border border-black/8 bg-white p-3.5 shadow-lg duration-150 fade-in-0 zoom-in-95"
          role="dialog"
          data-source-popover
          [attr.aria-label]="footerLabels().sources"
          [style.left.px]="ref.x"
          [style.top.px]="ref.y"
          (pointerdown)="$event.stopPropagation()"
          (pointerenter)="cancelCloseSourceRef()"
          (pointerleave)="scheduleCloseSourceRef()"
        >
          <app-chat-source-card
            [source]="source"
            [fallbackLabel]="footerLabels().sources"
            [similarityLabel]="footerLabels().similarity"
            (jump)="onJumpClick()"
          />
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
                alt="Uploaded image"
                class="max-h-48 max-w-48 rounded-lg object-contain"
                [src]="img"
              />
            }
          </div>
        }
      }
    </ng-template>

    <ng-template #assistantMessageTpl let-content="content" let-info="info">
      @let message = messageById(messageKey(info));
      <app-chat-tool-steps
        [steps]="message?.toolSteps ?? []"
        [doneLabel]="toolStepDoneLabel()"
        [failedLabel]="toolStepFailedLabel()"
      />
      @if (content) {
        <app-markdown-with-a2ui
          [content]="content"
          [isStreaming]="isStreaming(messageKey(info))"
        />
      } @else if (
        isStreaming(messageKey(info))
        && !message?.toolSteps?.length
      ) {
        <span class="text-text-tertiary">{{ thinkingLabel() }}</span>
      }
      @if (message?.sources?.length) {
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
                  alt=""
                  [class.opacity-90]="isChipHighlighted(message.id, $index)"
                  [src]="icon"
                />
              } @else {
                <span
                  class="flex size-3.5 shrink-0 items-center justify-center rounded-full text-[8px] font-semibold"
                  aria-hidden="true"
                  [class.bg-white/20]="isChipHighlighted(message.id, $index)"
                  [class.text-background]="isChipHighlighted(message.id, $index)"
                  [class.bg-black/10]="!isChipHighlighted(message.id, $index)"
                  [class.text-text-secondary]="!isChipHighlighted(message.id, $index)"
                >
                  {{ sourceInitial(source) }}
                </span>
              }
              <span class="truncate">{{ sourceLabel(source) }}</span>
            </button>
          }
        </div>
      }
    </ng-template>

    <ng-template #assistantFooterTpl let-info="info">
      @let timestamp = messageById(messageKey(info))?.timestamp;
      @if (timestamp) {
        <span class="text-xs text-text-tertiary">{{ timestamp | instant: 'time' }}</span>
      }
    </ng-template>
  `,
  styles: `
    /* ng-zorro-x bubble content is a flex item that shrink-wraps text.
       Charts/diagrams need the content box to stretch to the wrapper width. */
    :host ::ng-deep .ant-bubble-content:has(app-a2ui-chart),
    :host ::ng-deep .ant-bubble-content:has(app-mermaid-diagram) {
      width: 100%;
      max-width: 100%;
    }
  `,
  host: {
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
    '(document:keydown.escape)': 'onEscape()',
    class: 'block w-full',
  },
})
export class ChatBubbleListComponent implements OnDestroy {
  readonly messages = input.required<ChatMessageView[]>();
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

  readonly #expandedUserIds = signal<ReadonlySet<string>>(new Set());
  /** Immediate chip hover highlight (no delay). */
  readonly #hoveredChip = signal<{
    messageId: string;
    index: number;
  } | null>(null);

  readonly openRef = signal<OpenSourceRef | null>(null);
  #openTimer: ReturnType<typeof setTimeout> | null = null;
  #closeTimer: ReturnType<typeof setTimeout> | null = null;

  readonly userMessageTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('userMessageTpl');

  readonly assistantMessageTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('assistantMessageTpl');

  readonly assistantFooterTpl =
    viewChild<TemplateRef<NxBubbleSlotType>>('assistantFooterTpl');

  readonly messageByIdMap = computed(() => {
    const map = new Map<string, ChatMessageView>();
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

    return this.messages()
      .filter((message) => {
        if (this.isStreaming(message.id)) {
          return true;
        }
        if (message.content.trim()) {
          return true;
        }
        if (
          message.toolSteps?.length
          || message.sources?.length
          || message.images?.length
        ) {
          return true;
        }
        return false;
      })
      .map((message) => {
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

  messageById(id: string): ChatMessageView | undefined {
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
    return this.streamingMessageIds().has(messageId);
  }

  formatBasedOn(count: number): string {
    return this.footerLabels().basedOn.replace('{count}', `${count}`);
  }

  sourceAt(messageId: string, index: number): ChatSourceView | undefined {
    return this.messageById(messageId)?.sources?.[index];
  }

  sourceLabel(source: ChatSourceView): string {
    return sourceLabel(source, this.footerLabels().sources);
  }

  faviconUrl(source: ChatSourceView): string | null {
    return sourceFaviconUrl(source);
  }

  sourceInitial(source: ChatSourceView): string {
    return sourceInitial(source, this.footerLabels().sources);
  }

  isChipOpen(messageId: string, index: number): boolean {
    const ref = this.openRef();
    return ref?.messageId === messageId && ref.index === index;
  }

  isChipHighlighted(messageId: string, index: number): boolean {
    const hover = this.#hoveredChip();
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

  chipAriaLabel(index: number, source: ChatSourceView): string {
    const title = `${index + 1}. ${sourceTitle(source, this.footerLabels().sources)}`;
    if (source.url) {
      return `${title}. ${this.footerLabels().openReference}`;
    }
    return title;
  }

  onJumpClick(): void {
    this.closeSourceRef();
  }

  onChipClick(event: MouseEvent, source: ChatSourceView): void {
    event.stopPropagation();
    const url = source.url?.trim();
    if (!url) {
      return;
    }
    window.open(url, '_blank', 'noopener,noreferrer');
    this.closeSourceRef();
  }

  onChipPointerEnter(event: Event, messageId: string, index: number): void {
    this.#hoveredChip.set({ messageId, index });
    this.scheduleShowSourceRef(event, messageId, index);
  }

  onChipPointerLeave(): void {
    this.#hoveredChip.set(null);
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
      this.#openSourceRefAt(target, messageId, index);
      return;
    }
    this.#openTimer = setTimeout(() => {
      this.#openTimer = null;
      this.#openSourceRefAt(target, messageId, index);
    }, OPEN_DELAY_MS);
  }

  #openSourceRefAt(
    target: HTMLElement,
    messageId: string,
    index: number,
  ): void {
    const rect = target.getBoundingClientRect();
    const x = this.#clampPopoverX(rect.left);
    this.openRef.set({
      messageId,
      index,
      x,
      y: this.#popoverTopForHeight(
        rect.top,
        rect.bottom,
        POPOVER_EST_HEIGHT,
      ),
      anchorTop: rect.top,
      anchorBottom: rect.bottom,
    });
    // Measure after paint so the panel sits flush against the chip.
    requestAnimationFrame(() => {
      requestAnimationFrame(() => this.#refinePopoverPosition());
    });
  }

  #clampPopoverX(left: number): number {
    const pad = VIEWPORT_PAD;
    return Math.min(
      Math.max(pad, left),
      window.innerWidth - POPOVER_WIDTH - pad,
    );
  }

  #popoverTopForHeight(
    anchorTop: number,
    anchorBottom: number,
    height: number,
  ): number {
    const pad = VIEWPORT_PAD;
    const gap = POPOVER_GAP;
    const spaceBelow = window.innerHeight - anchorBottom - pad;
    if (spaceBelow >= height + gap) {
      return anchorBottom + gap;
    }
    return Math.max(pad, anchorTop - height - gap);
  }

  #refinePopoverPosition(): void {
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
    const x = this.#clampPopoverX(current.x);
    const y = this.#popoverTopForHeight(
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
    this.#closeTimer = setTimeout(() => {
      this.#closeTimer = null;
      this.openRef.set(null);
    }, CLOSE_DELAY_MS);
  }

  cancelOpenSourceRef(): void {
    if (this.#openTimer !== null) {
      clearTimeout(this.#openTimer);
      this.#openTimer = null;
    }
  }

  cancelCloseSourceRef(): void {
    if (this.#closeTimer !== null) {
      clearTimeout(this.#closeTimer);
      this.#closeTimer = null;
    }
  }

  closeSourceRef(): void {
    this.cancelOpenSourceRef();
    this.cancelCloseSourceRef();
    this.#hoveredChip.set(null);
    this.openRef.set(null);
  }

  isLongUserMessage(message: ChatMessageView): boolean {
    return (
      this.collapseLongUserMessages()
      && message.content.length > USER_COLLAPSE_CHARS
    );
  }

  isUserExpanded(messageId: string): boolean {
    return this.#expandedUserIds().has(messageId);
  }

  userMessageText(message: ChatMessageView): string {
    if (!this.isLongUserMessage(message) || this.isUserExpanded(message.id)) {
      return message.content;
    }
    return `${message.content.slice(0, USER_COLLAPSE_CHARS).trimEnd()}…`;
  }

  toggleUserExpanded(messageId: string): void {
    this.#expandedUserIds.update((current) => {
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
