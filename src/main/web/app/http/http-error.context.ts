import { HttpContextToken } from '@angular/common/http';

/** When true, {@link httpErrorInterceptor} still normalizes the error but skips toast. */
export const SKIP_ERROR_NOTIFICATION = new HttpContextToken<boolean>(() => false);
