import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideNzConfig } from 'ng-zorro-antd/core/config';
import { provideZard } from '@/shared/core';
import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { SESSION_LIST } from './layout/services/session-list.token';
import { ChatSessionListService } from './app/providers/chat-session-list.service';
import { FeatureFlagService } from './core/services/feature-flag.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAppInitializer(() => inject(FeatureFlagService).initialize()),
    provideZard(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpErrorInterceptor])),
    { provide: SESSION_LIST, useClass: ChatSessionListService },
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
