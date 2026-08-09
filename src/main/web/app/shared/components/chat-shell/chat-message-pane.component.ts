import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  input,
  output,
  untracked,
  viewChild,
} from '@angular/core';
import { NxPrompt } from 'ng-zorro-x/prompts';
import {
  ChatBubbleFooterLabels,
  ChatBubbleListComponent,
} from './chat-bubble-list.component';
import { ChatBubbleMessage } from './chat-bubble.model';
import { ChatWelcomePanelComponent } from './chat-welcome-panel.component';

@Component({
  selector: 'app-chat-message-pane',
  imports: [ChatWelcomePanelComponent, ChatBubbleListComponent],
  template: `
    <div
      #messageScroll
      [class]="
        compact()
          ? 'min-h-0 flex-1 overflow-y-auto px-3 py-3'
          : 'min-h-0 flex-1 overflow-y-auto bg-surface px-4 py-3 max-md:pt-2 md:py-6'
      "
    >
      @if (loading()) {
        <div
          class="mx-auto flex w-full max-w-3xl animate-in flex-col gap-4 px-1 py-2 duration-150 fade-in-0"
          aria-busy="true"
          aria-live="polite"
        >
          <div class="flex justify-end">
            <div class="h-10 w-2/5 max-w-xs animate-pulse rounded-2xl bg-black/6"></div>
          </div>
          <div class="flex justify-start">
            <div class="h-24 w-3/4 max-w-md animate-pulse rounded-2xl bg-black/6"></div>
          </div>
          <div class="flex justify-start">
            <div class="h-14 w-1/2 max-w-sm animate-pulse rounded-2xl bg-black/6"></div>
          </div>
        </div>
      } @else if (messages().length === 0) {
        @if (emptyText(); as text) {
          <p class="text-xs text-text-secondary">{{ text }}</p>
        } @else {
          <app-chat-welcome-panel
            class="h-full"
            [title]="welcomeTitle()"
            [description]="welcomeDescription()"
            [promptsTitle]="promptsTitle()"
            [prompts]="prompts()"
            (promptSelect)="promptSelect.emit($event)"
          />
        }
      } @else {
        <div class="animate-in duration-150 fade-in-0">
          <app-chat-bubble-list
            [messages]="messages()"
            [streamingMessageId]="streamingMessageId()"
            [streamingMessageIds]="streamingMessageIds()"
            [thinkingLabel]="thinkingLabel()"
            [footerLabels]="footerLabels()"
            [collapseLongUserMessages]="collapseLongUserMessages()"
            [expandLabel]="expandLabel()"
            [collapseLabel]="collapseLabel()"
            [toolStepDoneLabel]="toolStepDoneLabel()"
            [toolStepFailedLabel]="toolStepFailedLabel()"
          />
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex min-h-0 flex-1 flex-col overflow-hidden' },
})
export class ChatMessagePaneComponent {
  readonly messages = input.required<ChatBubbleMessage[]>();
  /** Session history loading — show skeleton instead of welcome flash. */
  readonly loading = input(false);
  readonly streamingMessageId = input<string | null>(null);
  readonly streamingMessageIds = input<ReadonlySet<string>>(new Set());
  readonly thinkingLabel = input('Thinking...');
  readonly footerLabels = input<ChatBubbleFooterLabels>({
    sources: 'Sources',
    similarity: 'Similarity',
    basedOn: 'Based on {count} source(s)',
    openReference: 'Open',
  });

  readonly collapseLongUserMessages = input(false);
  readonly expandLabel = input('Show more');
  readonly collapseLabel = input('Show less');
  readonly toolStepDoneLabel = input('Done');
  readonly toolStepFailedLabel = input('Failed');

  readonly welcomeTitle = input('');
  readonly welcomeDescription = input('');
  readonly promptsTitle = input('');
  readonly prompts = input<NxPrompt[]>([]);
  /** When set, empty state shows this text instead of the welcome panel. */
  readonly emptyText = input<string | null>(null);
  readonly autoScroll = input(false);
  readonly compact = input(false);

  readonly promptSelect = output<string>();

  private readonly messageScrollEl =
    viewChild<ElementRef<HTMLElement>>('messageScroll');

  constructor() {
    effect(() => {
      if (!this.autoScroll()) {
        return;
      }
      const messageCount = this.messages().length;
      untracked(() => {
        if (messageCount === 0) {
          return;
        }
        requestAnimationFrame(() => this.scrollMessagesToBottom());
      });
    });
  }

  private scrollMessagesToBottom(): void {
    const element = this.messageScrollEl()?.nativeElement;
    if (!element) {
      return;
    }
    element.scrollTop = element.scrollHeight;
    requestAnimationFrame(() => {
      element.scrollTop = element.scrollHeight;
    });
  }
}
