import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type BadgeStatus = 'online' | 'offline' | 'busy' | 'error' | 'pending';

const statusLabels: Record<BadgeStatus, string> = {
  online: 'Online',
  offline: 'Offline',
  busy: 'Busy',
  error: 'Error',
  pending: 'Pending',
};

const statusClasses: Record<BadgeStatus, string> = {
  online: 'bg-success-light text-success',
  offline: 'bg-black/6 text-text-tertiary',
  busy: 'bg-warning-light text-warning',
  error: 'bg-error-light text-error',
  pending: 'bg-primary-light text-primary',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span class="
      inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs
      font-medium whitespace-nowrap
    "
      [class]="statusClasses[status()]">
      @if (showDot()) {
        <span class="size-1.5 rounded-full bg-current"></span>
      }
      {{ label() || statusLabels[status()] }}
    </span>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  readonly status = input.required<BadgeStatus>();
  readonly label = input<string>();
  readonly showDot = input<boolean>(true);

  protected readonly statusLabels = statusLabels;
  protected readonly statusClasses = statusClasses;
}
