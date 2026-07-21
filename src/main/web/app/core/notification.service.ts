import { Injectable } from '@angular/core';
import { toast } from 'ngx-sonner';

const DEFAULT_TOAST_DURATION_MS = 3 * 1000;
const ERROR_TOAST_DURATION_MS = 5 * 1000;
const WARNING_TOAST_DURATION_MS = 4 * 1000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  showSuccess(message: string, duration = DEFAULT_TOAST_DURATION_MS): void {
    toast.success(message, { duration });
  }

  showError(message: string, duration = ERROR_TOAST_DURATION_MS): void {
    toast.error(message, { duration });
  }

  showWarning(message: string, duration = WARNING_TOAST_DURATION_MS): void {
    toast.warning(message, { duration });
  }

  showInfo(message: string, duration = DEFAULT_TOAST_DURATION_MS): void {
    toast.info(message, { duration });
  }

  dismissAll(): void {
    toast.dismiss();
  }
}
