import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { provideNzConfig } from 'ng-zorro-antd/core/config';
import { en_US, provideNzI18n } from 'ng-zorro-antd/i18n';
import { provideZard } from './shared/zard';
import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { credentialsInterceptor } from './core/interceptors/credentials.interceptor';
import { SESSION_LIST } from './layout/services/session-list.token';
import { ChatSessionListService } from './chat/chat-session-list.service';
import { FeatureFlagService } from './core/feature-flag.service';
import { DatadogErrorHandler } from './core/config/datadog-rum.config';
import {
  A2UI_RENDERER_CONFIG,
  A2uiRendererService,
  BASIC_CATALOG_OPTIONS,
  BasicCatalog,
  provideMarkdownRenderer,
} from '@a2ui/angular/v0_9';
import { marked } from 'marked';
import { provideEchartsCore } from 'ngx-echarts';
import {
  ChartComponentImplementation,
  EXPLORE_CHAT_CATALOG_ID,
} from './a2ui/explore-chat.catalog';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimationsAsync(),
    provideNzI18n(en_US),
    provideAppInitializer(() => inject(FeatureFlagService).initialize()),
    provideZard(),
    provideRouter(routes),
    // AI-249: Angular 22 defaults to FetchBackend (no upload progress).
    // Keep XHR for RAG document upload progress; do not switch to withFetch().
    provideHttpClient(
      withXhr(),
      withInterceptors([credentialsInterceptor, httpErrorInterceptor]),
    ),
    { provide: ErrorHandler, useClass: DatadogErrorHandler },
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
    // Lazy-load treeshaken ECharts so it stays out of the initial bundle budget.
    provideEchartsCore({
      echarts: () => import('./shared/components/charts/echarts.bundle').then(m => m.default),
    }),
    provideMarkdownRenderer(async markdown => String(await marked.parse(String(markdown ?? '')))),
    {
      provide: BASIC_CATALOG_OPTIONS,
      useValue: {
        id: EXPLORE_CHAT_CATALOG_ID,
        extraComponents: [ChartComponentImplementation],
      },
    },
    {
      provide: A2UI_RENDERER_CONFIG,
      useFactory: () => ({
        catalogs: [inject(BasicCatalog)],
        actionHandler: (action: unknown) => {
          console.debug('[A2UI] action', action);
        },
      }),
    },
    A2uiRendererService,
  ],
};
