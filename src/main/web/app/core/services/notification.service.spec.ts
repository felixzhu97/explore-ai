import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { toast } from 'ngx-sonner';
import { NotificationService } from './notification.service';

vi.mock('ngx-sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
    dismiss: vi.fn(),
  },
}));

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    service = new NotificationService();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('showSuccess', () => {
    it('should call toast.success with default duration', () => {
      service.showSuccess('Operation completed');

      expect(toast.success).toHaveBeenCalledWith('Operation completed', { duration: 3000 });
    });

    it('should call toast.success with custom duration', () => {
      service.showSuccess('Custom duration', 5000);

      expect(toast.success).toHaveBeenCalledWith('Custom duration', { duration: 5000 });
    });
  });

  describe('showError', () => {
    it('should call toast.error with default duration of 5000ms', () => {
      service.showError('Something went wrong');

      expect(toast.error).toHaveBeenCalledWith('Something went wrong', { duration: 5000 });
    });

    it('should allow custom duration for error toast', () => {
      service.showError('Custom error', 3000);

      expect(toast.error).toHaveBeenCalledWith('Custom error', { duration: 3000 });
    });
  });

  describe('showWarning', () => {
    it('should call toast.warning with default duration of 4000ms', () => {
      service.showWarning('Warning message');

      expect(toast.warning).toHaveBeenCalledWith('Warning message', { duration: 4000 });
    });
  });

  describe('showInfo', () => {
    it('should call toast.info with default duration of 3000ms', () => {
      service.showInfo('Info message');

      expect(toast.info).toHaveBeenCalledWith('Info message', { duration: 3000 });
    });
  });

  describe('dismissAll', () => {
    it('should call toast.dismiss', () => {
      service.dismissAll();

      expect(toast.dismiss).toHaveBeenCalled();
    });
  });
});
