import { httpResource, type HttpResourceRef } from '@angular/common/http';
import { Service } from '@angular/core';
import { API_BASE_URL } from '../http/api.constants';

export type MetricsDomain = 'chat' | 'rag' | 'agents' | 'tools' | 'vision';

export type MetricsRange = '7d' | '30d';

export interface NamedCount {
  name: string;
  count: number;
}

export interface SeriesPoint {
  label: string;
  value: number;
}

export interface MetricsOverview {
  range: string;
  requestCount: number;
  errorCount: number;
  successRate: number;
  errorRate: number;
  latencyP50Ms: number | null;
  latencyP95Ms: number | null;
  promptTokens: number | null;
  completionTokens: number | null;
  requestsByDomain: NamedCount[];
  domains: Record<string, Record<string, unknown>>;
}

export interface MetricsDomainSnapshot {
  domain: string;
  range: string;
  requestCount: number;
  errorCount: number;
  errorRate: number;
  latencyP50Ms: number | null;
  latencyP95Ms: number | null;
  promptTokens: number | null;
  completionTokens: number | null;
  inventory: Record<string, unknown>;
  requestSeries: SeriesPoint[];
  modelSeries: SeriesPoint[];
}

export interface SeriesResponse {
  name: string;
  domain: string | null;
  range: string;
  points: SeriesPoint[];
}

export interface InvocationEvent {
  id: string;
  occurredAt: string;
  domain: string;
  operation: string;
  outcome: string;
  latencyMs: number;
  provider: string | null;
  model: string | null;
  sessionId: string | null;
  documentId: string | null;
  agentType: string | null;
  toolName: string | null;
  promptTokens: number | null;
  completionTokens: number | null;
  errorCode: string | null;
  errorMessage: string | null;
}

export interface DrilldownPage {
  items: InvocationEvent[];
  total: number;
  page: number;
  size: number;
}

export interface DrilldownQuery {
  domain?: string;
  day?: string | undefined;
  outcome?: string;
  model?: string | undefined;
  agentType?: string;
  toolName?: string;
  page?: number;
  size?: number;
  range?: MetricsRange;
}

export const METRICS_DOMAINS: MetricsDomain[] = ['chat', 'rag', 'agents', 'tools', 'vision'];

export function isMetricsDomain(
  value: string | null | undefined,
): value is MetricsDomain {
  return !!value && (METRICS_DOMAINS as string[]).includes(value);
}

export interface SeriesQuery {
  name: string;
  range: MetricsRange;
  domain?: MetricsDomain;
}

/** Signal-driven metrics resources; create them from an injection context. */
@Service()
export class MetricsService {
  readonly #baseUrl = `${API_BASE_URL}/metrics`;

  overview(range: () => MetricsRange): HttpResourceRef<MetricsOverview | undefined> {
    return httpResource<MetricsOverview>(() => ({
      url: `${this.#baseUrl}/overview`,
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
        ? { url: `${this.#baseUrl}/domains/${value}`, params: { range: range() } }
        : undefined;
    });
  }

  series(
    query: () => SeriesQuery | undefined,
  ): HttpResourceRef<SeriesResponse | undefined> {
    return httpResource<SeriesResponse>(() => {
      const value = query();
      return value ? { url: `${this.#baseUrl}/series`, params: { ...value } } : undefined;
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
      return { url: `${this.#baseUrl}/drilldown`, params };
    });
  }
}
