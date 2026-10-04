import { describe, it, expect, afterEach, vi } from 'vitest';
import { type ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideEchartsCore } from 'ngx-echarts';
import { BehaviorSubject } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { MetricsDomainPageComponent } from './metrics-domain.page';
import type { MetricsDomainResponse } from './metrics.service';

describe('MetricsDomainPageComponent', () => {
  let fixture: ComponentFixture<MetricsDomainPageComponent>;
  let http: HttpTestingController;

  async function createPage(domain: string, range = '7d'): Promise<void> {
    globalThis.ResizeObserver = class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    };

    TestBed.resetTestingModule();
    const paramMap$ = new BehaviorSubject(convertToParamMap({ domain }));
    const queryParamMap$ = new BehaviorSubject(convertToParamMap({ range }));

    await TestBed.configureTestingModule({
      imports: [MetricsDomainPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideEchartsCore({ echarts: () => Promise.resolve({}) }),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMap$.asObservable(),
            queryParamMap: queryParamMap$.asObservable(),
            snapshot: {
              paramMap: convertToParamMap({ domain }),
              queryParamMap: convertToParamMap({ range }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MetricsDomainPageComponent);
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush({}));
    http.verify();
  });

  async function flushDomainPage(domain: 'chat' | 'rag'): Promise<void> {
    fixture.detectChanges();
    for (const req of http.match(() => true)) {
      const url = req.request.url;
      if (url.startsWith(`${API_BASE_URL}/metrics/domains/${domain}`)) {
        const stats = {
          range: '7d' as const,
          requestCount: 12,
          errorCount: 1,
          errorRate: 0.08,
          latencyP50Ms: null,
          latencyP95Ms: 90,
          promptTokens: null,
          completionTokens: null,
          requestSeries: [{ label: 'Mon', value: 4 }],
          modelSeries: [{ label: 'gpt-4', value: 4 }],
        };
        const response: MetricsDomainResponse = domain === 'chat'
          ? {
              ...stats,
              domain,
              inventory: {
                sessionCount: 3,
                activeSessionCount: 1,
                messageCount: 9,
                webSourceReplyCount: 0,
              },
            }
          : {
              ...stats,
              domain,
              inventory: {
                documentCount: 3,
                documentsByStatus: { READY: 3 },
                chunkCount: 12,
                totalFileBytes: 2048,
              },
            };
        req.flush(response);
      } else if (url.startsWith(`${API_BASE_URL}/metrics/series`)) {
        req.flush({
          name: 'documents_by_status',
          domain: 'rag',
          range: '7d',
          points: [{ label: 'ready', value: 3 }],
        });
      } else if (url.startsWith(`${API_BASE_URL}/metrics/drilldown`)) {
        req.flush({ items: [], total: 0, page: 0, size: 20 });
      }
    }
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should load domain metrics via http resource when mounted', async () => {
    await createPage('chat');
    await flushDomainPage('chat');

    expect(fixture.componentInstance.domainResource.value()?.requestCount).toBe(12);
    expect(fixture.componentInstance.requestSeries()).toEqual([{ label: 'Mon', value: 4 }]);
  });

  it('should fetch rag document series via http resource', async () => {
    await createPage('rag');
    await flushDomainPage('rag');

    expect(fixture.componentInstance.docsSeries()).toEqual([{ label: 'ready', value: 3 }]);
  });

  it('should skip domain requests when domain is invalid', async () => {
    await createPage('unknown');
    fixture.detectChanges();

    http.expectNone(r => r.url.includes('/metrics/domains/'));
    http.expectNone(r => r.url.includes('/metrics/drilldown'));

    expect(fixture.componentInstance.domain()).toBeNull();
  });
});
