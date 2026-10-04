import { httpResource, type HttpResourceRef } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_BASE_URL } from '../../../core/api.constants';
import type {
  DrilldownPage,
  DrilldownQuery,
  MetricsDomain,
  MetricsDomainSnapshot,
  MetricsOverview,
  MetricsRange,
  SeriesResponse,
} from '../metrics.model';

export interface SeriesQuery {
  name: string;
  range: MetricsRange;
  domain?: MetricsDomain;
}

/** Signal-driven metrics resources; create them from an injection context. */
@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly baseUrl = `${API_BASE_URL}/metrics`;

  overview(range: () => MetricsRange): HttpResourceRef<MetricsOverview | undefined> {
    return httpResource<MetricsOverview>(() => ({
      url: `${this.baseUrl}/overview`,
      params: { range: range() },
    }));
  }

  domain(
    domain: () => MetricsDomain | null,
    range: () => MetricsRange,
  ): HttpResourceRef<MetricsDomainSnapshot | undefined> {
    return httpResource<MetricsDomainSnapshot>(() => {
      const value = domain();
      return value
        ? { url: `${this.baseUrl}/domains/${value}`, params: { range: range() } }
        : undefined;
    });
  }

  series(
    query: () => SeriesQuery | undefined,
  ): HttpResourceRef<SeriesResponse | undefined> {
    return httpResource<SeriesResponse>(() => {
      const value = query();
      return value ? { url: `${this.baseUrl}/series`, params: { ...value } } : undefined;
    });
  }

  drilldown(
    query: () => DrilldownQuery | undefined,
  ): HttpResourceRef<DrilldownPage | undefined> {
    return httpResource<DrilldownPage>(() => {
      const value = query();
      if (!value) {
        return undefined;
      }
      const params = Object.fromEntries(
        Object.entries(value).filter(([, param]) => param !== undefined),
      ) as Record<string, string | number>;
      return { url: `${this.baseUrl}/drilldown`, params };
    });
  }
}
