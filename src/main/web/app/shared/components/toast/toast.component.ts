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
    <div class="fixed top-[72px] right-4 z-[9999] flex flex-col gap-2 max-w-[400px] pointer-events-none">
      @for (toast of notificationService.toasts(); track toast.id) {
        <div
          class="flex items-center gap-2.5 px-4 py-3 bg-surface rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.15)] animate-[toastSlideIn_0.25s_cubic-bezier(0.4,0,0.2,1)] pointer-events-auto"
          role="alert"
          [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
        >
          <span class="w-5 h-5 text-sm flex items-center justify-center rounded-full shrink-0" [class]="toastIconClasses[toast.type]">
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
            class="w-5 h-5 text-lg leading-none text-text-secondary bg-transparent border-none cursor-pointer rounded-full flex items-center justify-center transition-colors duration-150 shrink-0 hover:bg-black/6 hover:text-text"
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
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastComponent {
  protected notificationService = inject(NotificationService);
  protected readonly toastIconClasses = toastIconClasses;
}
