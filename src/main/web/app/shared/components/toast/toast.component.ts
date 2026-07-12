import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-toast',
  template: `
    <div class="
      pointer-events-none fixed top-16 right-4 z-[9999] flex
      flex-col gap-2
    ">
      @for (toast of notificationService.toasts(); track toast.id) {
        <div
          class="
            pointer-events-auto flex
            animate-toast-slide-in items-center
            gap-2.5 rounded-xl border border-gray-300 bg-white px-4 py-3
            shadow-lg
          "
          role="alert"
          [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
        >
          <span
            class="flex size-6 shrink-0 items-center justify-center rounded-full text-sm font-bold"
            [class]="getIconBgClass(toast.type)"
            style="color: white;"
          >
            @switch (toast.type) {
              @case ('success') { ✓ }
              @case ('error') { ✕ }
              @case ('warning') { ⚠ }
              @case ('info') { ℹ }
            }
          </span>
          <span class="flex-1 text-sm leading-relaxed text-gray-900">{{ toast.message }}</span>
          <button
            type="button"
            class="
              flex size-6 shrink-0 cursor-pointer items-center justify-center
              rounded-full border-none bg-transparent text-gray-500 transition-colors duration-150
              hover:bg-gray-100 hover:text-gray-900
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

  getIconBgClass(type: string): string {
    const bgClasses: Record<string, string> = {
      success: 'bg-green-500',
      error: 'bg-red-500',
      warning: 'bg-yellow-500',
      info: 'bg-blue-500',
    };
    return bgClasses[type] || 'bg-blue-500';
  }
}
