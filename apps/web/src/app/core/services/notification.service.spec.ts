import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { NotificationService, Toast } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    service = new NotificationService();
  });

  afterEach(() => {
    service.dismissAll();
    vi.useFakeTimers();
  });

  describe('toasts signal', () => {
    it('should initialize with empty toasts', () => {
      expect(service.toasts()).toEqual([]);
    });

    it('should have hasToasts as false when no toasts', () => {
      expect(service.hasToasts()).toBe(false);
    });

    it('should have hasToasts as true when there are toasts', () => {
      service.showSuccess('Test message');
      expect(service.hasToasts()).toBe(true);
    });
  });

  describe('showSuccess', () => {
    it('should add a success toast with default duration', async () => {
      vi.useFakeTimers();
      
      service.showSuccess('Operation completed');
      
      const toasts = service.toasts();
      expect(toasts.length).toBe(1);
      expect(toasts[0].message).toBe('Operation completed');
      expect(toasts[0].type).toBe('success');
      expect(toasts[0].duration).toBe(3000);
    });

    it('should add a success toast with custom duration', () => {
      service.showSuccess('Custom duration', 5000);
      const toast = service.toasts()[0];
      expect(toast.duration).toBe(5000);
    });
  });

  describe('showError', () => {
    it('should add an error toast with default duration of 5000ms', () => {
      service.showError('Something went wrong');
      const toast = service.toasts()[0];
      expect(toast.type).toBe('error');
      expect(toast.duration).toBe(5000);
    });

    it('should allow custom duration for error toast', () => {
      service.showError('Custom error', 3000);
      expect(service.toasts()[0].duration).toBe(3000);
    });
  });

  describe('showWarning', () => {
    it('should add a warning toast with default duration of 4000ms', () => {
      service.showWarning('Warning message');
      const toast = service.toasts()[0];
      expect(toast.type).toBe('warning');
      expect(toast.duration).toBe(4000);
    });
  });

  describe('showInfo', () => {
    it('should add an info toast with default duration of 3000ms', () => {
      service.showInfo('Info message');
      const toast = service.toasts()[0];
      expect(toast.type).toBe('info');
      expect(toast.duration).toBe(3000);
    });
  });

  describe('dismiss', () => {
    it('should remove a toast by id', () => {
      service.showSuccess('Toast 1');
      service.showError('Toast 2');
      const toasts = service.toasts();
      const firstId = toasts[0].id;

      service.dismiss(firstId);

      const remaining = service.toasts();
      expect(remaining.length).toBe(1);
      expect(remaining[0].message).toBe('Toast 2');
    });

    it('should not throw when dismissing non-existent id', () => {
      expect(() => service.dismiss(9999)).not.toThrow();
    });
  });

  describe('dismissAll', () => {
    it('should remove all toasts', () => {
      service.showSuccess('Toast 1');
      service.showError('Toast 2');
      service.showWarning('Toast 3');

      service.dismissAll();

      expect(service.toasts()).toEqual([]);
      expect(service.hasToasts()).toBe(false);
    });
  });

  describe('toast counter', () => {
    it('should generate unique ids for each toast', () => {
      service.showSuccess('Toast 1');
      service.showError('Toast 2');
      service.showWarning('Toast 3');

      const toasts = service.toasts();
      const ids = toasts.map(t => t.id);

      expect(new Set(ids).size).toBe(ids.length);
    });
  });

  describe('auto-dismiss', () => {
    it('should auto-dismiss toast after duration', () => {
      vi.useFakeTimers();

      service.showSuccess('Test', 3000);
      expect(service.toasts().length).toBe(1);

      vi.advanceTimersByTime(3000);

      expect(service.toasts().length).toBe(0);
    });
  });
});
