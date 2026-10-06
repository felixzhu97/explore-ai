import { type HttpInterceptorFn } from '@angular/common/http';

/** Include cookies + CSRF custom header on API calls (OWASP). */
export const credentialsInterceptor: HttpInterceptorFn = (request, next) => {
  return next(
    request.clone({
      withCredentials: true,
      setHeaders: { 'X-Requested-With': 'XMLHttpRequest' },
    }),
  );
};
