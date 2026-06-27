import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { NotificationService } from '@core/services/notification.service';

const toastIconClasses: Record<string, string> = {
  success: 'bg-success text-white',
  error: 'bg-error text-white',
  warning: 'bg-warning text-white',
  info: 'bg-primary text-white',
};

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="
      pointer-events-none fixed top-16 right-4 z-9999 flex max-w-md
      flex-col gap-2
    ">
      @for (toast of notificationService.toasts(); track toast.id) {
        <div
          class="
            pointer-events-auto flex
            animate-toast-slide-in items-center
            gap-2.5 rounded-xl bg-surface px-4 py-3
            shadow-lg
          "
          role="alert"
          [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
        >
          <span class="
            flex size-5 shrink-0 items-center justify-center rounded-full
            text-sm
          " [class]="toastIconClasses[toast.type]">
            @switch (toast.type) {
              @case ('success') {
                ✓
              }
              @case ('error') {
                ✕
              }
              @case ('warning') {
                ⚠
              }
              @case ('info') {
                ℹ
              }
            }
          </span>
          <span class="flex-1 text-sm leading-relaxed text-text">{{ toast.message }}</span>
          <button
            type="button"
            class="
              flex size-5 shrink-0 cursor-pointer items-center justify-center
              rounded-full border-none bg-transparent text-lg leading-none
              text-text-secondary transition-colors duration-150
              hover:bg-black/6 hover:text-text
            "
            (click)="notificationService.dismiss(toast.id)"
            aria-label="Dismiss"
          >
            ×
          </button>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastComponent {
  protected notificationService = inject(NotificationService);
  protected readonly toastIconClasses = toastIconClasses;
}
