import { httpResource, type HttpResourceRef } from '@angular/common/http';
import { Service } from '@angular/core';
import { Instant } from '@js-joda/core';
import { API_BASE_URL } from '../http/api.constants';
import { hasText } from '../shared/presence';

export type MetricsDomain = 'chat' | 'rag' | 'agents' | 'tools' | 'vision' | 'workflow';

export type MetricsRange = '7d' | '30d';

export type MetricsOutcome = 'success' | 'error';

/** Mirrors Java `ModuleStatus`. */
export type ModuleStatus = 'UP' | 'DEGRADED' | 'DISABLED';

export interface NamedCountResponse {
  name: string;
  count: number;
}

export interface SeriesPointResponse {
  label: string;
  value: number;
}

export interface ChatInventoryResponse {
  sessionCount: number;
  activeSessionCount: number;
  messageCount: number;
  webSourceReplyCount: number;
}

export interface RagInventoryResponse {
  documentCount: number;
  /** Document count keyed by `DocumentStatus`. */
  documentsByStatus: Record<string, number>;
  chunkCount: number;
  totalFileBytes: number;
}

export interface AgentsInventoryResponse {
  status: ModuleStatus;
  agentCount: number;
  healthyAgentCount: number;
}

export interface ToolsInventoryResponse {
  topTools: NamedCountResponse[];
}

export interface RequestsInventoryResponse {
  requests: number;
  errors: number;
}

export interface McpInventoryResponse {
  status: ModuleStatus;
  registeredTools: number;
  connectedServers: number;
}

export interface SystemInventoryResponse {
  status: ModuleStatus;
}

export interface MetricsDomainsResponse {
  chat: ChatInventoryResponse;
  rag: RagInventoryResponse;
  agents: AgentsInventoryResponse;
  mcp: McpInventoryResponse;
  system: SystemInventoryResponse;
}

export interface MetricsOverviewResponse {
  range: MetricsRange;
  requestCount: number;
  errorCount: number;
  successRate: number;
  errorRate: number;
  latencyP50Ms: number | null;
  latencyP95Ms: number | null;
  promptTokens: number | null;
  completionTokens: number | null;
  requestsByDomain: NamedCountResponse[];
  domains: MetricsDomainsResponse;
}

interface MetricsDomainStats {
  range: MetricsRange;
  requestCount: number;
  errorCount: number;
  errorRate: number;
  latencyP50Ms: number | null;
  latencyP95Ms: number | null;
  promptTokens: number | null;
  completionTokens: number | null;
  requestSeries: SeriesPointResponse[];
  modelSeries: SeriesPointResponse[];
}

/** The inventory shape follows the domain, as built by Java `MetricsService`. */
export type MetricsDomainResponse = MetricsDomainStats & (
  | { domain: 'chat'; inventory: ChatInventoryResponse }
  | { domain: 'rag'; inventory: RagInventoryResponse }
  | { domain: 'agents'; inventory: AgentsInventoryResponse }
  | { domain: 'tools'; inventory: ToolsInventoryResponse }
  | { domain: 'vision' | 'workflow'; inventory: RequestsInventoryResponse }
);

export interface SeriesResponse {
  name: string;
  domain: MetricsDomain | null;
  range: MetricsRange;
  points: SeriesPointResponse[];
}

export interface InvocationEventResponse {
  id: string;
  occurredAt: string;
  domain: MetricsDomain;
  operation: string;
  outcome: MetricsOutcome;
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

export interface InvocationEvent {
  id: string;
  occurredAt: Instant;
  domain: MetricsDomain;
  operation: string;
  outcome: MetricsOutcome;
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

export function toInvocationEvent(response: InvocationEventResponse): InvocationEvent {
  return { ...response, occurredAt: Instant.parse(response.occurredAt) };
}

export interface DrilldownPageResponse {
  items: InvocationEventResponse[];
  total: number;
  page: number;
  size: number;
}

export interface DrilldownPage {
  items: InvocationEvent[];
  total: number;
  page: number;
  size: number;
}

export function toDrilldownPage(response: DrilldownPageResponse): DrilldownPage {
  return { ...response, items: response.items.map(toInvocationEvent) };
}

export interface DrilldownQuery {
  domain?: MetricsDomain;
  day?: string | undefined;
  outcome?: MetricsOutcome;
  model?: string | undefined;
  agentType?: string;
  toolName?: string;
  page?: number;
  size?: number;
  range?: MetricsRange;
}

export const METRICS_DOMAINS: MetricsDomain[] = ['chat', 'rag', 'agents', 'tools', 'vision', 'workflow'];

export function isMetricsDomain(
  value: string | null | undefined,
): value is MetricsDomain {
  return hasText(value) && (METRICS_DOMAINS as string[]).includes(value);
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

  getOverview(
    range: () => MetricsRange,
  ): HttpResourceRef<MetricsOverviewResponse | undefined> {
    return httpResource<MetricsOverviewResponse>(() => ({
      url: `${this.#baseUrl}/overview`,
      params: { range: range() },
    }));
  }

  getDomain(
    domain: () => MetricsDomain | null,
    range: () => MetricsRange,
  ): HttpResourceRef<MetricsDomainResponse | undefined> {
    return httpResource<MetricsDomainResponse>(() => {
      const value = domain();
      return value !== null
        ? { url: `${this.#baseUrl}/domains/${value}`, params: { range: range() } }
        : undefined;
    });
  }

  getSeries(
    query: () => SeriesQuery | undefined,
  ): HttpResourceRef<SeriesResponse | undefined> {
    return httpResource<SeriesResponse>(() => {
      const value = query();
      return value !== undefined ? { url: `${this.#baseUrl}/series`, params: { ...value } } : undefined;
    });
  }

  getDrilldown(
    query: () => DrilldownQuery | undefined,
  ): HttpResourceRef<DrilldownPage | undefined> {
    return httpResource<DrilldownPage>(() => {
      const value = query();
      if (value === undefined) {
        return undefined;
      }
      const params = Object.fromEntries(
        Object.entries(value).filter(([, param]) => param !== undefined),
      ) as Record<string, string | number>;
      return { url: `${this.#baseUrl}/drilldown`, params };
    }, { parse: raw => toDrilldownPage(raw as DrilldownPageResponse) });
  }
}
