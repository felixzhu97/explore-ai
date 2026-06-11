import {
  Component,
  ChangeDetectionStrategy,
  inject,
  input,
  output,
} from '@angular/core';
import { NotificationService, Toast } from '@core/services/notification.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toast-container">
      @for (toast of notificationService.toasts(); track toast.id) {
        <div
          class="toast toast--{{ toast.type }}"
          role="alert"
          [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
        >
          <span class="toast__icon">
            @switch (toast.type) {
              @case ('success') { ✓ }
              @case ('error') { ✕ }
              @case ('warning') { ⚠ }
              @case ('info') { ℹ }
            }
          </span>
          <span class="toast__message">{{ toast.message }}</span>
          <button
            class="toast__dismiss"
            (click)="notificationService.dismiss(toast.id)"
            aria-label="Dismiss"
          >
            ×
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 72px;
      right: 16px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 400px;
      pointer-events: none;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      background: #ffffff;
      border-radius: 10px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      animation: toastSlideIn 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      pointer-events: auto;
    }

    @keyframes toastSlideIn {
      from {
        opacity: 0;
        transform: translateX(100%);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .toast__icon {
      font-size: 14px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .toast__message {
      flex: 1;
      font-size: 14px;
      line-height: 1.4;
      color: #1d1d1f;
    }

    .toast__dismiss {
      padding: 0;
      width: 20px;
      height: 20px;
      font-size: 18px;
      line-height: 1;
      color: #86868b;
      background: transparent;
      border: none;
      cursor: pointer;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.15s ease;
      flex-shrink: 0;
    }

    .toast__dismiss:hover {
      background: rgba(0, 0, 0, 0.06);
      color: #1d1d1f;
    }

    /* Type variants */
    .toast--success .toast__icon {
      background: #34c759;
      color: white;
    }

    .toast--error .toast__icon {
      background: #ff3b30;
      color: white;
    }

    .toast--warning .toast__icon {
      background: #ff9500;
      color: white;
    }

    .toast--info .toast__icon {
      background: #007aff;
      color: white;
    }
  `],
})
export class ToastComponent {
  protected notificationService = inject(NotificationService);
}
