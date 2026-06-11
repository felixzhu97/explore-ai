import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type BadgeStatus = 'online' | 'offline' | 'busy' | 'error' | 'pending';

const statusLabels: Record<BadgeStatus, string> = {
  online: 'Online',
  offline: 'Offline',
  busy: 'Busy',
  error: 'Error',
  pending: 'Pending',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span class="badge" [class]="'badge--' + status()">
      @if (showDot()) {
        <span class="badge__dot"></span>
      }
      {{ label() || statusLabels[status()] }}
    </span>
  `,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
      border-radius: var(--radius-full);
      white-space: nowrap;

      &--online {
        background: var(--color-success-light);
        color: var(--color-success);
      }

      &--offline {
        background: rgba(0, 0, 0, 0.06);
        color: var(--color-text-tertiary);
      }

      &--busy {
        background: var(--color-warning-light);
        color: var(--color-warning);
      }

      &--error {
        background: var(--color-error-light);
        color: var(--color-error);
      }

      &--pending {
        background: var(--color-primary-light);
        color: var(--color-primary);
      }
    }

    .badge__dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  status = input.required<BadgeStatus>();
  label = input<string>();
  showDot = input<boolean>(true);

  protected readonly statusLabels = statusLabels;
}
