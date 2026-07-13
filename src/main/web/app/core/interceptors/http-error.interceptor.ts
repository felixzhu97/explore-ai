import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpErrorResponse,
  HttpEvent,
  HttpInterceptorFn,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificationService } from '@core/services/notification.service';
import { DatadogRumService } from '@core/monitoring/datadog-rum.service';

export interface AppError {
  code: string;
  message: string;
  status: number;
  timestamp: Date;
  details?: unknown;
}

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);
  const datadogRum = inject(DatadogRumService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse): Observable<HttpEvent<unknown>> => {
      const appError = normalizeError(error);
      logError(req, appError, datadogRum);
      notifyUser(appError, notificationService);
      return throwError(() => appError);
    }),
  ) as Observable<HttpEvent<unknown>>;
};

function normalizeError(error: HttpErrorResponse): AppError {
  if (error.error instanceof ErrorEvent) {
    return handleClientError(error);
  }
  return handleServerError(error);
}

function handleClientError(error: HttpErrorResponse): AppError {
  const clientError = error.error as ErrorEvent;
  return {
    code: 'CLIENT_ERROR',
    message: clientError.message || 'A client-side error occurred',
    status: 0,
    timestamp: new Date(),
  };
}

function handleServerError(error: HttpErrorResponse): AppError {
  switch (error.status) {
    case 400:
      return {
        code: 'BAD_REQUEST',
        message: extractMessage(error) || 'Invalid request',
        status: 400,
        timestamp: new Date(),
        details: error.error,
      };

    case 401:
      return {
        code: 'UNAUTHORIZED',
        message: 'Authentication required. Please log in again.',
        status: 401,
        timestamp: new Date(),
      };

    case 403:
      return {
        code: 'FORBIDDEN',
        message:
          extractMessage(error)
          || 'You do not have permission to perform this action.',
        status: 403,
        timestamp: new Date(),
      };

    case 404:
      return {
        code: 'NOT_FOUND',
        message:
          extractMessage(error) || 'The requested resource was not found.',
        status: 404,
        timestamp: new Date(),
        details: error.url,
      };

    case 408:
      return {
        code: 'REQUEST_TIMEOUT',
        message: 'The request took too long. Please try again.',
        status: 408,
        timestamp: new Date(),
      };

    case 422:
      return {
        code: 'VALIDATION_ERROR',
        message: extractMessage(error) || 'Validation failed',
        status: 422,
        timestamp: new Date(),
        details: error.error,
      };

    case 429:
      return {
        code: 'RATE_LIMITED',
        message: 'Too many requests. Please wait a moment and try again.',
        status: 429,
        timestamp: new Date(),
      };

    case 500:
      return {
        code: 'INTERNAL_SERVER_ERROR',
        message: 'A server error occurred. Please try again later.',
        status: 500,
        timestamp: new Date(),
      };

    case 502:
      return {
        code: 'BAD_GATEWAY',
        message: 'The server is temporarily unavailable. Please try again later.',
        status: 502,
        timestamp: new Date(),
      };

    case 503:
      return {
        code: 'SERVICE_UNAVAILABLE',
        message: 'The service is currently unavailable. Please try again later.',
        status: 503,
        timestamp: new Date(),
      };

    default:
      return {
        code: 'UNKNOWN_ERROR',
        message: extractMessage(error) || 'An unexpected error occurred',
        status: error.status,
        timestamp: new Date(),
      };
  }
}

function extractMessage(error: HttpErrorResponse): string | null {
  const errorBody = error.error;
  if (!errorBody) {
    return null;
  }

  return (
    errorBody.message
    || errorBody.error
    || errorBody.detail
    || (typeof errorBody === 'string' ? errorBody : null)
  );
}

function logError(
  req: HttpRequest<unknown>,
  error: AppError,
  datadogRum: DatadogRumService,
): void {
  const logEntry = {
    url: req.url,
    method: req.method,
    code: error.code,
    message: error.message,
    status: error.status,
    timestamp: error.timestamp,
  };

  if (error.status >= 500) {
    console.error('[httpErrorInterceptor]', logEntry);
    datadogRum.addError(new Error(error.message), logEntry);
  } else if (error.status >= 400) {
    console.warn('[httpErrorInterceptor]', logEntry);
  } else {
    console.debug('[httpErrorInterceptor]', logEntry);
  }
}

function notifyUser(
  error: AppError,
  notificationService: NotificationService,
): void {
  notificationService.showError(error.message);
}
