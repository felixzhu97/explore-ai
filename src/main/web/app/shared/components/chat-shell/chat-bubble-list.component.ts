import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  TemplateRef,
  viewChild,
} from '@angular/core';
import {
  NxBubbleListComponent,
  NxBubbleListItem,
  NxBubbleSlotType,
} from 'ng-zorro-x/bubble';
import { MarkdownWithA2uiComponent } from '@shared/components/markdown-with-a2ui.component';
import { formatMessageTime } from '@shared/utils/format-time.util';
import { ChatBubbleMessage } from './chat-bubble.model';

export interface ChatBubbleFooterLabels {
  sources: string;
  similarity: string;
  basedOn: string;
}

@Component({
  selector: 'app-chat-bubble-list',
  imports: [NxBubbleListComponent, MarkdownWithA2uiComponent],
  template: `
    <div class="mx-auto max-w-[880px]">
      <nx-bubble-list [items]="bubbleItems()" [roles]="bubbleRoles" [autoScroll]="true" />
    </div>

    <ng-template #userMessageTpl let-info="info">
      @if (messageById(messageKey(info)); as message) {
        @if (message.content) {
          <div class="wrap-break-word whitespace-pre-wrap">{{ message.content }}</div>
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
                  <span>{{ step.label.replace('…', '') }} · 完成</span>
                } @else {
                  <span>{{ step.label.replace('…', '') }} · 失败</span>
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
    </ng-template>

    <ng-template #assistantFooterTpl let-info="info">
      @if (messageById(messageKey(info)); as message) {
        <div class="flex flex-col gap-1">
          @if (message.timestamp) {
            <span class="text-xs text-text-tertiary">{{ formatTime(message.timestamp) }}</span>
          }
          @if (message.sources && message.sources.length > 0) {
            @if (message.sourcesExpanded) {
              <div class="mt-1 rounded-lg bg-surface-secondary p-3">
                <div class="mb-2 text-sm font-semibold text-text">
                  {{ footerLabels().sources }}
                </div>
                @for (source of message.sources.slice(0, 5); track source.url ?? $index) {
                  <div class="border-b border-border-light py-2 last:border-0">
                    @if (source.url) {
                      <a
                        class="mb-1 block text-sm text-text underline-offset-2 hover:underline"
                        [href]="source.url"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {{ source.title || source.url }}
                      </a>
                      @if (source.text) {
                        <div class="text-xs text-text-tertiary">{{ source.text }}</div>
                      }
                    } @else {
                      <div class="mb-1 text-sm text-text">
                        {{
                          source.text.length > 200
                            ? source.text.slice(0, 200) + '...'
                            : source.text
                        }}
                      </div>
                      <div class="text-xs text-text-tertiary">
                        {{ footerLabels().similarity }}:
                        {{ (source.score * 100).toFixed(1) }}%
                      </div>
                    }
                  </div>
                }
              </div>
            }
            <button
              type="button"
              class="mt-1 flex cursor-pointer items-center gap-1 border-none bg-transparent p-0 text-xs text-text-secondary"
              (click)="toggleSources.emit(messageKey(info))"
            >
              {{
                formatBasedOn(message.sources.length)
              }}
              {{ message.sourcesExpanded ? '▲' : '▼' }}
            </button>
          }
        </div>
      }
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatBubbleListComponent {
  readonly messages = input.required<ChatBubbleMessage[]>();
  readonly streamingMessageId = input<string | null>(null);
  readonly streamingMessageIds = input<ReadonlySet<string>>(new Set());
  readonly thinkingLabel = input('Thinking...');
  readonly footerLabels = input<ChatBubbleFooterLabels>({
    sources: 'Sources',
    similarity: 'Similarity',
    basedOn: 'Based on {count} source(s)',
  });

  readonly toggleSources = output<string>();

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
      const hasSources = Boolean(message.sources?.length);
      const hasToolSteps = Boolean(message.toolSteps?.length);

      const item: NxBubbleListItem = {
        key: message.id,
        role: message.role,
        content: message.content,
        loading: isAssistant && isStreaming && !message.content && !hasToolSteps,
        messageRender: isAssistant ? assistantTpl : userTpl,
      };

      if (isAssistant && (message.timestamp || hasSources)) {
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
}
