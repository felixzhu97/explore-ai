import { Injectable, signal, computed } from '@angular/core';

const DEFAULT_TOAST_DURATION_MS = 3 * 1000;
const ERROR_TOAST_DURATION_MS = 5 * 1000;
const WARNING_TOAST_DURATION_MS = 4 * 1000;

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly toastsSignal = signal<Toast[]>([]);
  private counter = 0;

  readonly toasts = this.toastsSignal.asReadonly();
  readonly hasToasts = computed(() => this.toastsSignal().length > 0);

  showSuccess(message: string, duration = DEFAULT_TOAST_DURATION_MS): void {
    this.addToast(message, 'success', duration);
  }

  showError(message: string, duration = ERROR_TOAST_DURATION_MS): void {
    this.addToast(message, 'error', duration);
  }

  showWarning(message: string, duration = WARNING_TOAST_DURATION_MS): void {
    this.addToast(message, 'warning', duration);
  }

  showInfo(message: string, duration = DEFAULT_TOAST_DURATION_MS): void {
    this.addToast(message, 'info', duration);
  }

  dismiss(id: number): void {
    this.toastsSignal.update(toasts => toasts.filter(t => t.id !== id));
  }

  dismissAll(): void {
    this.toastsSignal.set([]);
  }

  private addToast(message: string, type: Toast['type'], duration: number): void {
    const toast: Toast = {
      id: ++this.counter,
      message,
      type,
      duration,
    };

    this.toastsSignal.update(toasts => [...toasts, toast]);

    if (duration > 0) {
      setTimeout(() => this.dismiss(toast.id), duration);
    }
  }
}
