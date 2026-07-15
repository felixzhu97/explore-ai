import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideNzConfig } from 'ng-zorro-antd/core/config';
import { provideZard } from '@/shared/core';
import { routes } from './app.routes';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { SESSION_LIST } from './layout/services/session-list.token';
import { ChatSessionListService } from './app/providers/chat-session-list.service';
import { FeatureFlagService } from './core/services/feature-flag.service';
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
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import {
  ChartComponentImplementation,
  EXPLORE_CHAT_CATALOG_ID,
} from './a2ui/explore-chat.catalog';

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  CanvasRenderer,
]);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAppInitializer(() => inject(FeatureFlagService).initialize()),
    provideZard(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpErrorInterceptor])),
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
    provideEchartsCore({ echarts }),
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
