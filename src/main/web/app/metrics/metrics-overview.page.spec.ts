import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { type ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideEchartsCore } from 'ngx-echarts';
import { API_BASE_URL } from '../http/api.constants';
import { MetricsOverviewPageComponent } from './metrics-overview.page';
import type { MetricsOverviewResponse, MetricsRange } from './metrics.service';

const emptyOverview: MetricsOverviewResponse = {
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
    chat: {
      sessionCount: 5,
      activeSessionCount: 1,
      messageCount: 20,
      webSourceReplyCount: 0,
    },
    rag: {
      documentCount: 2,
      documentsByStatus: { READY: 2 },
      chunkCount: 8,
      totalFileBytes: 1024,
    },
    agents: { status: 'UP', agentCount: 4, healthyAgentCount: 4 },
    mcp: { status: 'DISABLED', registeredTools: 0, connectedServers: 0 },
    system: { status: 'UP' },
  },
};

describe('MetricsOverviewPageComponent', () => {
  let fixture: ComponentFixture<MetricsOverviewPageComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    globalThis.ResizeObserver = class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [MetricsOverviewPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideEchartsCore({ echarts: () => Promise.resolve({}) }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MetricsOverviewPageComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.match(() => true).forEach(request => request.flush({}));
    http.verify();
  });

  async function flushOverviewPage(
    range: MetricsRange,
    overview = emptyOverview,
  ): Promise<void> {
    fixture.detectChanges();
    for (const request of http.match(() => true)) {
      const url = request.request.url;
      if (url.startsWith(`${API_BASE_URL}/metrics/overview`)) {
        request.flush({ ...overview, range });
      } else if (url.startsWith(`${API_BASE_URL}/metrics/series`)) {
        request.flush({
          name: 'requests',
          domain: null,
          range,
          points: [{ label: 'Mon', value: 10 }],
        });
      } else if (url.startsWith(`${API_BASE_URL}/metrics/drilldown`)) {
        request.flush({ items: [], total: 0, page: 0, size: 10 });
      }
    }
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should load metrics via http resource when mounted', async () => {
    await flushOverviewPage('7d');

    expect(fixture.componentInstance.overviewResource.value()?.requestCount).toBe(42);
    expect(fixture.componentInstance.domainSeries()).toEqual([{ label: 'chat', value: 30 }]);
    expect(fixture.componentInstance.requestSeries()).toEqual([{ label: 'Mon', value: 10 }]);
  });

  it('should refetch overview when range changes', async () => {
    await flushOverviewPage('7d');

    fixture.componentInstance.setRange('30d');
    await flushOverviewPage('30d', { ...emptyOverview, range: '30d', requestCount: 99 });

    expect(fixture.componentInstance.overviewResource.value()?.requestCount).toBe(99);
  });
});
