import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.constants';
import { MetricsService, type MetricsDomain } from './metrics.service';

describe('MetricsService', () => {
  let service: MetricsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MetricsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  const flushEffects = () => TestBed.inject(ApplicationRef).tick();

  it('should request overview with range', () => {
    TestBed.runInInjectionContext(() => service.overview(() => '30d'));
    flushEffects();

    const req = http.expectOne(`${API_BASE_URL}/metrics/overview?range=30d`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should request drilldown without undefined filters', () => {
    const query = { domain: 'chat', day: '2026-07-26', model: undefined, page: 0 };
    TestBed.runInInjectionContext(() => service.drilldown(() => query));
    flushEffects();

    const req = http.expectOne(
      r => r.url === `${API_BASE_URL}/metrics/drilldown` && r.params.get('domain') === 'chat',
    );
    expect(req.request.params.get('day')).toBe('2026-07-26');
    expect(req.request.params.has('model')).toBe(false);
    req.flush({ items: [], total: 0, page: 0, size: 20 });
  });

  it('should skip domain request when domain is unknown', () => {
    const domain = signal<MetricsDomain | null>(null);
    TestBed.runInInjectionContext(() => service.domain(domain, () => '7d'));
    flushEffects();

    http.expectNone(r => r.url.startsWith(`${API_BASE_URL}/metrics/domains`));
  });
});
