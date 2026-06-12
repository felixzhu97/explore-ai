import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
} from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { httpErrorInterceptor, AppError } from './http-error.interceptor';
import { NotificationService } from '@core/services/notification.service';

describe('httpErrorInterceptor', () => {
  let notificationService: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([httpErrorInterceptor]))],
    });
    notificationService = TestBed.inject(NotificationService);
  });

  describe('error handling', () => {
    it('should handle 401 unauthorized errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('UNAUTHORIZED');
              expect(error.status).toBe(401);
              expect(error.message).toBe('Authentication required. Please log in again.');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 404 not found errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 404,
        statusText: 'Not Found',
        url: '/api/test',
        error: { message: 'Resource not found' },
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('NOT_FOUND');
              expect(error.status).toBe(404);
              expect(error.message).toBe('Resource not found');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 500 server errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 500,
        statusText: 'Internal Server Error',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('INTERNAL_SERVER_ERROR');
              expect(error.status).toBe(500);
              expect(error.message).toBe('A server error occurred. Please try again later.');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle network errors', async () => {
      const networkError = new ErrorEvent('Error', {
        message: 'Network connection failed',
      });

      const mockErrorResponse = new HttpErrorResponse({
        status: 0,
        statusText: 'Unknown Error',
        url: '/api/test',
        error: networkError,
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('CLIENT_ERROR');
              expect(error.status).toBe(0);
              expect(error.message).toBe('Network connection failed');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 422 validation errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 422,
        statusText: 'Unprocessable Entity',
        url: '/api/test',
        error: { message: 'Validation failed: email is required' },
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('POST', '/api/test', { test: 'data' });

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('VALIDATION_ERROR');
              expect(error.status).toBe(422);
              expect(error.message).toBe('Validation failed: email is required');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 429 rate limit errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 429,
        statusText: 'Too Many Requests',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('RATE_LIMITED');
              expect(error.status).toBe(429);
              expect(error.message).toBe('Too many requests. Please wait a moment and try again.');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 502 bad gateway errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 502,
        statusText: 'Bad Gateway',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('BAD_GATEWAY');
              expect(error.status).toBe(502);
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 503 service unavailable errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 503,
        statusText: 'Service Unavailable',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('SERVICE_UNAVAILABLE');
              expect(error.status).toBe(503);
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 403 forbidden errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 403,
        statusText: 'Forbidden',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('FORBIDDEN');
              expect(error.status).toBe(403);
              expect(error.message).toBe('You do not have permission to perform this action.');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should handle 408 timeout errors', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 408,
        statusText: 'Request Timeout',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.code).toBe('REQUEST_TIMEOUT');
              expect(error.status).toBe(408);
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });
  });

  describe('error message extraction', () => {
    it('should extract message from error.message field', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        url: '/api/test',
        error: { message: 'Custom error message' },
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.message).toBe('Custom error message');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should extract message from error.error field', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        url: '/api/test',
        error: { error: 'Error field message' },
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.message).toBe('Error field message');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should extract message from error.detail field', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        url: '/api/test',
        error: { detail: 'Detail message' },
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.message).toBe('Detail message');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });

    it('should use default message when no error message available', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: (error: AppError) => {
            try {
              expect(error.message).toBe('Invalid request');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });
  });

  describe('notification', () => {
    it('should show error notification', async () => {
      const mockErrorResponse = new HttpErrorResponse({
        status: 500,
        statusText: 'Internal Server Error',
        url: '/api/test',
      });

      const mockNext = vi.fn().mockReturnValue(throwError(() => mockErrorResponse));
      const req = new HttpRequest('GET', '/api/test');

      const interceptor = TestBed.runInInjectionContext(() => {
        return httpErrorInterceptor(req, mockNext as unknown as HttpHandlerFn);
      });

      await new Promise<void>((resolve, reject) => {
        interceptor.subscribe({
          error: () => {
            try {
              expect(notificationService.toasts().length).toBeGreaterThan(0);
              const errorToast = notificationService.toasts().find((t) => t.type === 'error');
              expect(errorToast).toBeDefined();
              expect(errorToast?.message).toBe('A server error occurred. Please try again later.');
              resolve();
            } catch (e) {
              reject(e);
            }
          },
        });
      });
    });
  });
});
