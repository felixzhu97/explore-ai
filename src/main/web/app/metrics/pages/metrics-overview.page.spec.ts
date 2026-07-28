import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideEchartsCore } from 'ngx-echarts';
import { API_BASE_URL } from '../../core/api.constants';
import { MetricsOverviewPage } from './metrics-overview.page';

const emptyOverview = {
  range: '7d',
  requestCount: 42,
  errorCount: 0,
  successRate: 1,
  errorRate: 0,
  latencyP50Ms: null,
  latencyP95Ms: 120,
  promptTokens: 100,
  completionTokens: 200,
  requestsByDomain: [{ name: 'chat', count: 30 }],
  domains: {
    chat: { sessionCount: 5 },
    rag: { documentCount: 2 },
  },
};

describe('MetricsOverviewPage', () => {
  let fixture: ComponentFixture<MetricsOverviewPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    globalThis.ResizeObserver = class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    } as unknown as typeof ResizeObserver;

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [MetricsOverviewPage, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideEchartsCore({ echarts: () => Promise.resolve({}) }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MetricsOverviewPage);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush({}));
    http.verify();
  });

  async function flushOverviewPage(
    range: string,
    overview = emptyOverview,
  ): Promise<void> {
    fixture.detectChanges();
    for (const req of http.match(() => true)) {
      const url = req.request.url;
      if (url.startsWith(`${API_BASE_URL}/metrics/overview`)) {
        req.flush({ ...overview, range });
      } else if (url.startsWith(`${API_BASE_URL}/metrics/series`)) {
        req.flush({
          name: 'requests',
          domain: null,
          range,
          points: [{ label: 'Mon', value: 10 }],
        });
      } else if (url.startsWith(`${API_BASE_URL}/metrics/drilldown`)) {
        req.flush({ items: [], total: 0, page: 0, size: 10 });
      }
    }
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should_load_metrics_via_httpResource_when_mounted', async () => {
    await flushOverviewPage('7d');

    expect(fixture.componentInstance.overviewResource.value()?.requestCount).toBe(42);
    expect(fixture.componentInstance.domainSeries()).toEqual([{ label: 'chat', value: 30 }]);
    expect(fixture.componentInstance.requestSeries()).toEqual([{ label: 'Mon', value: 10 }]);
  });

  it('should_refetch_overview_when_range_changes', async () => {
    await flushOverviewPage('7d');

    fixture.componentInstance.setRange('30d');
    await flushOverviewPage('30d', { ...emptyOverview, range: '30d', requestCount: 99 });

    expect(fixture.componentInstance.overviewResource.value()?.requestCount).toBe(99);
  });
});
