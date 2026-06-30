import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { Session } from '../../sidebar.service';

@Component({
  selector: 'app-session-item',
  imports: [],
  standalone: true,
  template: `
    <button
      type="button"
      class="session-item group relative flex w-full cursor-pointer items-center
             gap-2 rounded-md px-2 py-1.5 text-left text-[13px] transition-all
             duration-150 hover:bg-surface-secondary"
      [class.bg-primary-light]="isActive()"
      [class.text-primary]="isActive()"
      [class.text-text-secondary]="!isActive()"
      [title]="session().title"
      (click)="onSelect()"
    >
      <!-- Chat Icon -->
      <svg
        class="size-4 shrink-0 opacity-60"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>

      <!-- Title -->
      <span class="flex-1 truncate">
        {{ session().title }}
      </span>

      <!-- Action Buttons (shown on hover) -->
      @if (!collapsed()) {
        <div
          class="absolute right-1 flex items-center gap-0.5 opacity-0
                 transition-opacity duration-150 group-hover:opacity-100"
        >
          <!-- Pin Button -->
          <button
            type="button"
            class="size-5 rounded p-0.5 transition-colors duration-150
                   hover:bg-black/10"
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
                      d="M12 17v5M5 17h14v-1.76a2 2 0 0 0-1.11-1.79
                 l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79
                 l-1.78.9A2 2 0 0 0 5 15.24V17z"
              />
            </svg>
          </button>

          <!-- Delete Button -->
          <button
            type="button"
            class="size-5 rounded p-0.5 text-red-500 opacity-0
                   transition-all duration-150 group-hover:opacity-100 hover:bg-red-50"
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
              <path
                d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
              />
            </svg>
          </button>
        </div>
      }
    </button>
  `,
  styles: [`
    :host {
      display: block;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionItemComponent {
  readonly session = input.required<Session>();
  readonly isActive = input(false);
  readonly collapsed = input(false);

  readonly pin = output<void>();
  readonly delete = output<void>();
  readonly sessionSelect = output<void>();

  onSelect(): void {
    this.sessionSelect.emit();
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
