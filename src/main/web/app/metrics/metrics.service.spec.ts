import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../core/api.constants';
import { MetricsService } from './metrics.service';

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

  it('should request overview with range', () => {
    service.getOverview('30d').subscribe((result) => {
      expect(result.requestCount).toBe(0);
    });

    const req = http.expectOne(`${API_BASE_URL}/metrics/overview?range=30d`);
    expect(req.request.method).toBe('GET');
    req.flush({
      range: '30d',
      requestCount: 0,
      errorCount: 0,
      successRate: 1,
      errorRate: 0,
      latencyP50Ms: null,
      latencyP95Ms: null,
      promptTokens: null,
      completionTokens: null,
      requestsByDomain: [],
      domains: {},
    });
  });

  it('should request drilldown with filters', () => {
    service.getDrilldown({ domain: 'chat', day: '2026-07-26', page: 0, size: 20 }).subscribe((result) => {
      expect(result.total).toBe(0);
    });

    const req = http.expectOne(
      r => r.url === `${API_BASE_URL}/metrics/drilldown` && r.params.get('domain') === 'chat',
    );
    expect(req.request.params.get('day')).toBe('2026-07-26');
    req.flush({ items: [], total: 0, page: 0, size: 20 });
  });
});
