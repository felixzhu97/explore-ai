import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type {
  DrilldownPage,
  DrilldownQuery,
  MetricsDomainSnapshot,
  MetricsOverview,
  MetricsRange,
  SeriesResponse,
} from './metrics.model';

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/metrics`;

  getOverview(range: MetricsRange = '7d'): Observable<MetricsOverview> {
    return this.http.get<MetricsOverview>(`${this.baseUrl}/overview`, {
      params: { range },
    });
  }

  getDomain(domain: string, range: MetricsRange = '7d'): Observable<MetricsDomainSnapshot> {
    return this.http.get<MetricsDomainSnapshot>(`${this.baseUrl}/domains/${domain}`, {
      params: { range },
    });
  }

  getSeries(name: string, domain?: string, range: MetricsRange = '7d'): Observable<SeriesResponse> {
    let params = new HttpParams().set('name', name).set('range', range);
    if (domain) {
      params = params.set('domain', domain);
    }
    return this.http.get<SeriesResponse>(`${this.baseUrl}/series`, { params });
  }

  getDrilldown(query: DrilldownQuery): Observable<DrilldownPage> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 20));
    if (query.domain) params = params.set('domain', query.domain);
    if (query.day) params = params.set('day', query.day);
    if (query.outcome) params = params.set('outcome', query.outcome);
    if (query.model) params = params.set('model', query.model);
    if (query.agentType) params = params.set('agentType', query.agentType);
    if (query.toolName) params = params.set('toolName', query.toolName);
    if (query.range) params = params.set('range', query.range);
    return this.http.get<DrilldownPage>(`${this.baseUrl}/drilldown`, { params });
  }
}
