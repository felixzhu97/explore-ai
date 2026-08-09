import { describe, it, expect, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { provideEchartsCore } from 'ngx-echarts';
import { BehaviorSubject } from 'rxjs';
import { API_BASE_URL } from '../../core/api.constants';
import { MetricsDomainPage } from './metrics-domain.page';

describe('MetricsDomainPage', () => {
  let fixture: ComponentFixture<MetricsDomainPage>;
  let http: HttpTestingController;

  async function createPage(domain: string, range = '7d'): Promise<void> {
    globalThis.ResizeObserver = class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    } as unknown as typeof ResizeObserver;

    TestBed.resetTestingModule();
    const paramMap$ = new BehaviorSubject(convertToParamMap({ domain }));
    const queryParamMap$ = new BehaviorSubject(convertToParamMap({ range }));

    await TestBed.configureTestingModule({
      imports: [MetricsDomainPage, RouterTestingModule],
      providers: [
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

    fixture = TestBed.createComponent(MetricsDomainPage);
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush({}));
    http.verify();
  });

  async function flushDomainPage(domain: string): Promise<void> {
    fixture.detectChanges();
    for (const req of http.match(() => true)) {
      const url = req.request.url;
      if (url.startsWith(`${API_BASE_URL}/metrics/domains/${domain}`)) {
        req.flush({
          domain,
          range: '7d',
          requestCount: 12,
          errorCount: 1,
          errorRate: 0.08,
          latencyP50Ms: null,
          latencyP95Ms: 90,
          promptTokens: null,
          completionTokens: null,
          inventory: {},
          requestSeries: [{ label: 'Mon', value: 4 }],
          modelSeries: [{ label: 'gpt-4', value: 4 }],
        });
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
