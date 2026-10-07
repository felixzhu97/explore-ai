import { httpResource, type HttpResourceRef } from '@angular/common/http';
import { Service } from '@angular/core';
import { Instant } from '@js-joda/core';
import { API_BASE_URL } from '../http/api.constants';
import { hasText } from '../shared/presence';

export type MetricsCapability = 'chat' | 'rag' | 'agents' | 'tools' | 'vision' | 'workflow';

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

export interface MetricsCapabilitiesResponse {
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
  requestsByCapability: NamedCountResponse[];
  capabilities: MetricsCapabilitiesResponse;
}

interface MetricsCapabilityStats {
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

/** The inventory shape follows the capability, as built by Java `MetricsService`. */
export type MetricsCapabilityResponse = MetricsCapabilityStats & (
  | { capability: 'chat'; inventory: ChatInventoryResponse }
  | { capability: 'rag'; inventory: RagInventoryResponse }
  | { capability: 'agents'; inventory: AgentsInventoryResponse }
  | { capability: 'tools'; inventory: ToolsInventoryResponse }
  | { capability: 'vision' | 'workflow'; inventory: RequestsInventoryResponse }
);

export interface SeriesResponse {
  name: string;
  capability: MetricsCapability | null;
  range: MetricsRange;
  points: SeriesPointResponse[];
}

export interface InvocationEventResponse {
  id: string;
  occurredAt: string;
  capability: MetricsCapability;
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
  capability: MetricsCapability;
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

/** Maps an API response to an invocation event. */
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

/** Maps an API response to a drilldown page. */
export function toDrilldownPage(response: DrilldownPageResponse): DrilldownPage {
  return { ...response, items: response.items.map(toInvocationEvent) };
}

export interface DrilldownQuery {
  capability?: MetricsCapability;
  day?: string | undefined;
  outcome?: MetricsOutcome;
  model?: string | undefined;
  agentType?: string;
  toolName?: string;
  page?: number;
  size?: number;
  range?: MetricsRange;
}

export const METRICS_CAPABILITIES: MetricsCapability[] = ['chat', 'rag', 'agents', 'tools', 'vision', 'workflow'];

/** Tells whether the value is a known metrics capability. */
export function isMetricsCapability(
  value: string | null | undefined,
): value is MetricsCapability {
  return hasText(value) && (METRICS_CAPABILITIES as string[]).includes(value);
}

export interface SeriesQuery {
  name: string;
  range: MetricsRange;
  capability?: MetricsCapability;
}

/** Signal-driven metrics resources; create them from an injection context. */
@Service()
export class MetricsService {
  readonly #baseUrl = `${API_BASE_URL}/metrics`;

  /** Loads the overview for the range. */
  getOverview(
    range: () => MetricsRange,
  ): HttpResourceRef<MetricsOverviewResponse | undefined> {
    return httpResource<MetricsOverviewResponse>(() => ({
      url: `${this.#baseUrl}/overview`,
      params: { range: range() },
    }));
  }

  /** Loads the metrics of a capability. */
  getCapability(
    capability: () => MetricsCapability | null,
    range: () => MetricsRange,
  ): HttpResourceRef<MetricsCapabilityResponse | undefined> {
    return httpResource<MetricsCapabilityResponse>(() => {
      const value = capability();
      return value !== null
        ? { url: `${this.#baseUrl}/capabilities/${value}`, params: { range: range() } }
        : undefined;
    });
  }

  /** Loads a daily series. */
  getSeries(
    query: () => SeriesQuery | undefined,
  ): HttpResourceRef<SeriesResponse | undefined> {
    return httpResource<SeriesResponse>(() => {
      const value = query();
      return value !== undefined ? { url: `${this.#baseUrl}/series`, params: { ...value } } : undefined;
    });
  }

  /** Loads one page of invocation events. */
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
