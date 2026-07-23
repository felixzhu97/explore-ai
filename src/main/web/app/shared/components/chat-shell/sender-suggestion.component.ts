import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  model,
  output,
  signal,
} from '@angular/core';
import { filterSenderGroups } from './sender-action.catalog';
import type { SenderActionGroup, SenderActionItem } from './sender-action.model';

@Component({
  selector: 'app-sender-suggestion',
  template: `
    @if (open()) {
      <div
        class="
          absolute inset-x-0 bottom-full z-30 mb-2 max-h-72 overflow-hidden rounded-xl
          border border-black/10 bg-white shadow-lg
        "
        role="listbox"
        [attr.aria-label]="filterPlaceholder()"
      >
        <div class="border-b border-black/8 px-3 py-2">
          <input
            type="search"
            class="
              w-full rounded-lg border border-black/10 bg-surface px-3 py-1.5 text-sm
              text-foreground outline-none focus:border-black/20
            "
            [placeholder]="filterPlaceholder()"
            [value]="query()"
            (input)="onQueryInput($event)"
            (keydown)="onKeydown($event)"
          />
        </div>
        <div class="max-h-56 overflow-y-auto py-1">
          @for (group of visibleGroups(); track group.id) {
            <div class="px-2 pt-2 pb-1">
              <p class="px-2 text-[11px] font-medium tracking-wide text-foreground/45 uppercase">
                {{ group.label }}
              </p>
              <ul class="mt-1 flex flex-col gap-0.5" role="group">
                @for (item of group.items; track item.id) {
                  <li>
                    <button
                      type="button"
                      class="
                        flex w-full flex-col items-start rounded-lg px-2 py-1.5 text-left
                        hover:bg-black/4
                      "
                      [class.bg-black/6]="isActive(item)"
                      role="option"
                      [attr.aria-selected]="isActive(item)"
                      (click)="select(item)"
                      (mouseenter)="setActive(item)"
                    >
                      <span class="text-sm font-medium text-foreground">{{ item.label }}</span>
                      @if (item.description) {
                        <span class="mt-0.5 line-clamp-2 text-xs text-foreground/55">
                          {{ item.description }}
                        </span>
                      }
                    </button>
                  </li>
                }
              </ul>
            </div>
          } @empty {
            <p class="px-3 py-4 text-center text-sm text-foreground/50">{{ emptyLabel() }}</p>
          }
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'contents' },
})
export class SenderSuggestionComponent {
  readonly open = model(false);
  readonly groups = input<SenderActionGroup[]>([]);
  readonly filterPlaceholder = input('Filter…');
  readonly emptyLabel = input('No matches');

  readonly itemSelect = output<SenderActionItem>();

  readonly query = signal('');
  readonly activeId = signal<string | null>(null);

  readonly visibleGroups = computed(
    () => filterSenderGroups(this.groups(), this.query()),
  );

  readonly flatItems = computed(() => this.visibleGroups().flatMap(group => group.items));

  onQueryInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);
    const first = this.flatItems()[0];
    this.activeId.set(first?.id ?? null);
  }

  onKeydown(event: KeyboardEvent): void {
    const items = this.flatItems();
    if (items.length === 0) {
      if (event.key === 'Escape') {
        this.open.set(false);
      }
      return;
    }
    const current = this.activeId();
    const index = Math.max(0, items.findIndex(item => item.id === current));

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      const next = items[(index + 1) % items.length];
      this.activeId.set(next.id);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      const prev = items[(index - 1 + items.length) % items.length];
      this.activeId.set(prev.id);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const item = items[index] ?? items[0];
      this.select(item);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.open.set(false);
    }
  }

  isActive(item: SenderActionItem): boolean {
    return this.activeId() === item.id;
  }

  setActive(item: SenderActionItem): void {
    this.activeId.set(item.id);
  }

  select(item: SenderActionItem): void {
    this.itemSelect.emit(item);
    this.open.set(false);
    this.query.set('');
  }
}
