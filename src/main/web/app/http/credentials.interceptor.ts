import { HttpInterceptorFn } from '@angular/common/http';

/** Include cookies + CSRF custom header on API calls (OWASP). */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(
    req.clone({
      withCredentials: true,
      setHeaders: { 'X-Requested-With': 'XMLHttpRequest' },
    }),
  );
};
