import { inject } from '@angular/core';
import { Instant } from '@js-joda/core';
import {
  type HttpErrorResponse,
  type HttpEvent,
  type HttpInterceptorFn,
} from '@angular/common/http';
import { type Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificationService } from '../ui/notification.service';
import { SKIP_ERROR_NOTIFICATION } from './http-error.context';

export interface AppError {
  code: string;
  message: string;
  status: number;
  timestamp: Instant;
  details?: unknown;
  /** `ErrorResponse.errorCode` from the API, when the body carries one. */
  errorCode?: string;
}

/* eslint-disable no-restricted-syntax --
   Java `ErrorResponse` is `@JsonInclude(NON_NULL)`, so null fields are absent. */
export interface ErrorResponse {
  message?: string;
  errorCode?: string;
  timestamp: string;
  path?: string;
}
/* eslint-enable no-restricted-syntax */

export { SKIP_ERROR_NOTIFICATION };

/** Turns HTTP errors into app errors and notifies the user. */
export const httpErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const notificationService = inject(NotificationService);
  return next(request).pipe(
    catchError((error: HttpErrorResponse): Observable<HttpEvent<unknown>> => {
      const appError = normalizeError(error);
      if (!request.context.get(SKIP_ERROR_NOTIFICATION)) {
        notifyUser(appError, notificationService);
      }
      return throwError(() => appError);
    }),
  );
};

function normalizeError(error: HttpErrorResponse): AppError {
  if (error.error instanceof ErrorEvent) {
    return handleClientError(error);
  }
  const appError = handleServerError(error);
  const errorCode = readErrorResponse(error.error)?.errorCode;
  return errorCode !== undefined ? { ...appError, errorCode } : appError;
}

/** The body as an `ErrorResponse` when it has that shape, otherwise null. */
export function readErrorResponse(body: unknown): ErrorResponse | null {
  if (body === null || typeof body !== 'object') {
    return null;
  }
  const { message, errorCode, timestamp, path } = body as Record<string, unknown>;
  if (typeof timestamp !== 'string') {
    return null;
  }
  return {
    timestamp,
    ...(typeof message === 'string' ? { message } : {}),
    ...(typeof errorCode === 'string' ? { errorCode } : {}),
    ...(typeof path === 'string' ? { path } : {}),
  };
}

function handleClientError(error: HttpErrorResponse): AppError {
  const clientError = error.error as ErrorEvent;
  return {
    code: 'CLIENT_ERROR',
    message: clientError.message !== '' ? clientError.message : 'A client-side error occurred',
    status: 0,
    timestamp: Instant.now(),
  };
}

function handleServerError(error: HttpErrorResponse): AppError {
  switch (error.status) {
    case 400:
      return {
        code: 'BAD_REQUEST',
        message: extractMessage(error) ?? 'Invalid request',
        status: 400,
        timestamp: Instant.now(),
        details: error.error,
      };

    case 401:
      return {
        code: 'UNAUTHORIZED',
        message: 'Authentication required. Please log in again.',
        status: 401,
        timestamp: Instant.now(),
      };

    case 403:
      return {
        code: 'FORBIDDEN',
        message:
          extractMessage(error)
          ?? 'You do not have permission to perform this action.',
        status: 403,
        timestamp: Instant.now(),
      };

    case 404:
      return {
        code: 'NOT_FOUND',
        message:
          extractMessage(error) ?? 'The requested resource was not found.',
        status: 404,
        timestamp: Instant.now(),
        details: error.url,
      };

    case 408:
      return {
        code: 'REQUEST_TIMEOUT',
        message: 'The request took too long. Please try again.',
        status: 408,
        timestamp: Instant.now(),
      };

    case 422:
      return {
        code: 'VALIDATION_ERROR',
        message: extractMessage(error) ?? 'Validation failed',
        status: 422,
        timestamp: Instant.now(),
        details: error.error,
      };

    case 429:
      return {
        code: 'RATE_LIMITED',
        message: 'Too many requests. Please wait a moment and try again.',
        status: 429,
        timestamp: Instant.now(),
      };

    case 500:
      return {
        code: 'INTERNAL_SERVER_ERROR',
        message: 'A server error occurred. Please try again later.',
        status: 500,
        timestamp: Instant.now(),
      };

    case 502:
      return {
        code: 'BAD_GATEWAY',
        message: 'The server is temporarily unavailable. Please try again later.',
        status: 502,
        timestamp: Instant.now(),
      };

    case 503:
      return {
        code: 'SERVICE_UNAVAILABLE',
        message:
          extractMessage(error)
          ?? 'The service is currently unavailable. Please try again later.',
        status: 503,
        timestamp: Instant.now(),
      };

    default:
      return {
        code: 'UNKNOWN_ERROR',
        message: extractMessage(error) ?? 'An unexpected error occurred',
        status: error.status,
        timestamp: Instant.now(),
      };
  }
}

function extractMessage(error: HttpErrorResponse): string | null {
  const errorBody: unknown = error.error;
  if (typeof errorBody === 'string') {
    return errorBody !== '' ? errorBody : null;
  }
  if (errorBody === null || typeof errorBody !== 'object') {
    return null;
  }

  const { message, error: reason, detail } = errorBody as Record<string, unknown>;
  return [message, reason, detail]
    .find((value): value is string => typeof value === 'string' && value.length > 0)
    ?? null;
}

function notifyUser(
  error: AppError,
  notificationService: NotificationService,
): void {
  notificationService.showError(error.message);
}
