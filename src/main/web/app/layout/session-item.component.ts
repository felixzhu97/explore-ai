import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
} from '@angular/core';
import { I18nService } from '../i18n/i18n.service';
import { ZardButtonComponent } from '../ui/button';
import { ZardSidebarMenuButtonDirective } from '../ui/layout/sidebar-menu-button.directive';
import type { SidebarSession } from './session-list.token';

@Component({
  selector: 'app-session-item',
  imports: [ZardSidebarMenuButtonDirective, ZardButtonComponent],
  template: `
    <div class="group/session relative w-full">
      <button
        type="button"
        z-sidebar-menu-button
        class="cursor-pointer pr-2 group-hover/session:pr-11"
        [zActive]="isActive()"
        [title]="session().title"
        (click)="onSelect()"
      >
        <span class="min-w-0 flex-1 truncate text-left">
          {{ session().title }}
        </span>
      </button>

      @if (!isCollapsed()) {
        <div
          class="
            pointer-events-none absolute inset-y-0 right-1 z-10 flex items-center gap-0.5
            pl-3 opacity-0 transition-opacity duration-150
            group-hover/session:pointer-events-auto group-hover/session:opacity-100
          "
        >
          <div class="flex items-center gap-0.5">
            <button
              type="button"
              z-button
              zType="ghost"
              zSize="icon"
              class="size-5 cursor-pointer border-0 bg-transparent hover:border-transparent hover:bg-transparent focus-visible:border-transparent focus-visible:bg-transparent focus-visible:ring-0"
              [title]="session().pinned ? i18n.t().sidebar.unpinChat : i18n.t().sidebar.pinChat"
              (click)="onPin($event)"
            >
              <svg
                class="size-3.5 -rotate-45"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M12 17v5" />
                <path
                  d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1z"
                  [attr.fill]="session().pinned ? 'currentColor' : 'none'"
                />
              </svg>
            </button>

            <button
              type="button"
              z-button
              zType="ghost"
              zSize="icon"
              class="size-5 cursor-pointer border-0 bg-transparent hover:border-transparent hover:bg-transparent focus-visible:border-transparent focus-visible:bg-transparent focus-visible:ring-0"
              [title]="i18n.t().sidebar.deleteChat"
              (click)="onDelete($event)"
            >
              <svg
                class="size-3.5"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </button>
          </div>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block',
  },
})
export class SessionItemComponent {
  protected readonly i18n = inject(I18nService);

  readonly session = input.required<SidebarSession>();
  readonly isActive = input(false);
  readonly isCollapsed = input(false);

  readonly pinToggleRequested = output<void>();
  readonly deleteRequested = output<void>();
  readonly selected = output<void>();

  onSelect(): void {
    this.selected.emit();
    (document.activeElement as HTMLElement | null)?.blur();
  }

  onPin(event: MouseEvent): void {
    event.stopPropagation();
    this.pinToggleRequested.emit();
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.deleteRequested.emit();
  }
}
