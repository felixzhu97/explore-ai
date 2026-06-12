import { Injectable, signal, computed } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private toastsSignal = signal<Toast[]>([]);
  private counter = 0;

  toasts = this.toastsSignal.asReadonly();

  hasToasts = computed(() => this.toastsSignal().length > 0);

  showSuccess(message: string, duration = 3000): void {
    this.addToast(message, 'success', duration);
  }

  showError(message: string, duration = 5000): void {
    this.addToast(message, 'error', duration);
  }

  showWarning(message: string, duration = 4000): void {
    this.addToast(message, 'warning', duration);
  }

  showInfo(message: string, duration = 3000): void {
    this.addToast(message, 'info', duration);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((toasts) => toasts.filter((t) => t.id !== id));
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

    this.toastsSignal.update((toasts) => [...toasts, toast]);

    if (duration > 0) {
      setTimeout(() => this.dismiss(toast.id), duration);
    }
  }
}
