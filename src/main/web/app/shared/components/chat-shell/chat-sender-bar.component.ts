import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  model,
  output,
} from '@angular/core';
import { NxSenderComponent, NxSenderExtendActionDirective } from 'ng-zorro-x/sender';
import { SenderSuggestionComponent } from './sender-suggestion.component';
import {
  appendUniqueSenderAction,
  removeSenderActionById,
  type SenderActionGroup,
  type SenderActionItem,
} from './sender-action.model';

@Component({
  selector: 'app-chat-sender-bar',
  imports: [NxSenderComponent, NxSenderExtendActionDirective, SenderSuggestionComponent],
  template: `
    <div
      class="relative shrink-0 border-t border-black/8 bg-white px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]"
      data-sender-bar
    >
      <ng-content select="[chatSenderPrelude]" />
      <div class="relative flex items-center gap-2">
        <ng-content select="[chatSenderActions]" />
        <div class="relative min-w-0 flex-1">
          <app-sender-suggestion
            [(open)]="suggestionOpen"
            [groups]="actionGroups()"
            [filterPlaceholder]="filterPlaceholder()"
            [emptyLabel]="emptyLabel()"
            (itemSelect)="onActionSelect($event)"
          />
          @if (selectedActions().length > 0) {
            <div class="mb-2 flex flex-wrap items-center gap-1.5" data-selected-actions>
              @for (action of selectedActions(); track action.id) {
                <span
                  class="
                    inline-flex max-w-full items-center gap-1.5 rounded-full bg-foreground
                    py-1 pr-1 pl-2.5 text-sm font-semibold text-white shadow-sm
                  "
                  data-selected-action
                >
                  <span
                    class="shrink-0 text-[10px] font-medium tracking-wide text-white/65 uppercase"
                  >
                    {{ action.kind === 'agent' ? agentChipLabel() : toolChipLabel() }}
                  </span>
                  <span class="min-w-0 truncate">{{ action.label }}</span>
                  <button
                    type="button"
                    class="
                      flex size-5 shrink-0 items-center justify-center rounded-full
                      bg-white/15 text-base leading-none text-white hover:bg-white/25
                    "
                    [attr.title]="removeToolLabel()"
                    [attr.aria-label]="removeToolLabel()"
                    (click)="removeSelectedAction($event, action.id)"
                  >
                    ×
                  </button>
                </span>
              }
            </div>
          }
          <nx-sender
            class="w-full min-w-0"
            [placeholder]="placeholder()"
            [(value)]="value"
            [loading]="loading()"
            (submitSend)="submitSend.emit()"
            (valueChange)="onValueChange($event)"
          >
            @if (actionGroups().length > 0) {
              <button
                nx-sender-extend-action
                type="button"
                class="
                  flex size-8 items-center justify-center rounded-lg text-sm font-medium
                  text-foreground/60 hover:bg-black/5 hover:text-foreground
                "
                [attr.title]="openActionsLabel()"
                [attr.aria-label]="openActionsLabel()"
                (click)="toggleSuggestion($event)"
              >
                /
              </button>
            }
          </nx-sender>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block shrink-0',
    '(document:keydown.escape)': 'onEscape()',
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class ChatSenderBarComponent {
  readonly placeholder = input.required<string>();
  readonly loading = input(false);
  readonly value = model('');
  readonly selectedActions = model<SenderActionItem[]>([]);
  readonly actionGroups = input<SenderActionGroup[]>([]);
  readonly filterPlaceholder = input('Filter…');
  readonly emptyLabel = input('No matches');
  readonly openActionsLabel = input('Open actions');
  readonly removeToolLabel = input('Remove tool');
  readonly toolChipLabel = input('Tool');
  readonly agentChipLabel = input('Agent');

  readonly submitSend = output<void>();
  readonly actionSelect = output<SenderActionItem>();

  readonly suggestionOpen = model(false);

  constructor() {
    effect(() => {
      if (this.actionGroups().length === 0) {
        this.suggestionOpen.set(false);
      }
    });
  }

  onEscape(): void {
    if (this.suggestionOpen()) {
      this.suggestionOpen.set(false);
    }
  }

  onDocumentPointerDown(event: PointerEvent): void {
    if (!this.suggestionOpen()) {
      return;
    }
    const target = event.target as Element | null;
    if (target?.closest?.('[data-sender-bar]')) {
      return;
    }
    this.suggestionOpen.set(false);
  }

  onValueChange(next: string): void {
    if (this.actionGroups().length === 0) {
      return;
    }
    if (next.endsWith('/')) {
      this.value.set(next.slice(0, -1));
      this.suggestionOpen.set(true);
    }
  }

  toggleSuggestion(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.actionGroups().length === 0) {
      return;
    }
    this.suggestionOpen.update(open => !open);
  }

  onActionSelect(item: SenderActionItem): void {
    if (item.kind === 'tool' || (item.kind === 'agent' && item.id !== 'agent:open')) {
      this.selectedActions.set(appendUniqueSenderAction(this.selectedActions(), item));
    }
    this.actionSelect.emit(item);
    this.suggestionOpen.set(false);
  }

  removeSelectedAction(event: Event, id: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.selectedActions.set(removeSenderActionById(this.selectedActions(), id));
  }
}
