import { HttpInterceptorFn } from '@angular/common/http';

/** Include cookies on API calls (OWASP HttpOnly client identity). */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
