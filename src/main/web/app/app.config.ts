import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideNzConfig } from 'ng-zorro-antd/core/config';
import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpErrorInterceptor])),
    provideNzConfig({
      theme: {
        primaryColor: '#000000',
        primaryColorHover: '#434343',
        primaryColorActive: '#000000',
        primaryColorOutline: 'rgba(0, 0, 0, 0.06)',
        borderRadius: '6px',
      },
    }),
  ],
};
