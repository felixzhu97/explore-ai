import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { ZardButtonComponent } from '../../../shared/components/button';
import { ZardSidebarMenuButtonDirective } from '../../../shared/components/layout/sidebar-menu-button.directive';
import type { SidebarSession } from '../../sidebar-session.model';

@Component({
  selector: 'app-session-item',
  imports: [ZardSidebarMenuButtonDirective, ZardButtonComponent],
  template: `
    <div class="group/session relative w-full">
      <button
        type="button"
        z-sidebar-menu-button
        class="pr-2 group-hover/session:pr-11"
        [zActive]="isActive()"
        [title]="session().title"
        (click)="onSelect()"
      >
        <span class="size-4 shrink-0 opacity-60">
          <svg
            class="size-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </span>

        <span class="min-w-0 flex-1 truncate text-left">
          {{ session().title }}
        </span>
      </button>

      @if (!collapsed()) {
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
              class="size-5 border-0 bg-transparent hover:border-transparent hover:bg-transparent focus-visible:border-transparent focus-visible:bg-transparent focus-visible:ring-0"
              [title]="session().pinned ? 'Unpin' : 'Pin'"
              (click)="onPin($event)"
            >
              <svg
                class="size-3.5"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  [attr.fill]="session().pinned ? 'currentColor' : 'none'"
                  d="M12 17v5M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17z"
                />
              </svg>
            </button>

            <button
              type="button"
              z-button
              zType="ghost"
              zSize="icon"
              class="size-5 border-0 bg-transparent hover:border-transparent hover:bg-transparent focus-visible:border-transparent focus-visible:bg-transparent focus-visible:ring-0"
              title="Delete"
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
  readonly session = input.required<SidebarSession>();
  readonly isActive = input(false);
  readonly collapsed = input(false);

  readonly pin = output<void>();
  readonly delete = output<void>();
  readonly sessionSelect = output<void>();

  onSelect(): void {
    this.sessionSelect.emit();
    (document.activeElement as HTMLElement | null)?.blur();
  }

  onPin(event: MouseEvent): void {
    event.stopPropagation();
    this.pin.emit();
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
