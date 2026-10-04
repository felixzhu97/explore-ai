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
  day?: string;
  outcome?: string;
  model?: string;
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
