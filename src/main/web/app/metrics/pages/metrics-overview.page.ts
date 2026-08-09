import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ChartPanelComponent } from '../../shared/components/charts';
import { API_BASE_URL } from '../../core/api.constants';
import { I18nService } from '../../core/i18n';
import {
  MetricsDomainHealthComponent,
  type DomainHealthItem,
} from '../components/metrics-domain-health.component';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from '../components/metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from '../components/metrics-drilldown-table.component';
import type {
  DrilldownPage,
  InvocationEvent,
  MetricsOverview,
  MetricsRange,
  SeriesResponse,
} from '../metrics.model';

@Component({
  selector: 'app-metrics-overview-page',
  imports: [
    RouterLink,
    ChartPanelComponent,
    MetricsKpiCardsComponent,
    MetricsDomainHealthComponent,
    MetricsDrilldownTableComponent,
  ],
  templateUrl: './metrics-overview.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class MetricsOverviewPage {
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nService);

  readonly range = signal<MetricsRange>('7d');

  readonly overviewResource = httpResource<MetricsOverview>(() => ({
    url: `${API_BASE_URL}/metrics/overview`,
    params: { range: this.range() },
  }));

  readonly seriesResource = httpResource<SeriesResponse>(() => ({
    url: `${API_BASE_URL}/metrics/series`,
    params: { name: 'requests', range: this.range() },
  }));

  readonly drilldownResource = httpResource<DrilldownPage>(() => ({
    url: `${API_BASE_URL}/metrics/drilldown`,
    params: { page: 0, size: 10, range: this.range() },
  }));

  readonly kpis = computed((): MetricsKpi[] => {
    const overview = this.overviewResource.value();
    if (!overview) {
      return [];
    }
    const kpi = this.i18n.t().metricsPage.kpi;
    const chat = overview.domains['chat'] as { sessionCount?: number } | undefined;
    const rag = overview.domains['rag'] as { documentCount?: number } | undefined;
    const tokenTotal = (overview.promptTokens ?? 0) + (overview.completionTokens ?? 0);
    const hasTokens =
      overview.promptTokens != null || overview.completionTokens != null;
    return [
      {
        key: 'requests',
        label: kpi.aiRequests,
        value: formatNumber(overview.requestCount),
      },
      {
        key: 'errorRate',
        label: kpi.errorRate,
        value: formatPercent(overview.errorRate),
      },
      {
        key: 'p95',
        label: kpi.p95Latency,
        value:
          overview.latencyP95Ms == null
            ? '—'
            : `${Math.round(overview.latencyP95Ms)} ms`,
      },
      {
        key: 'tokens',
        label: kpi.tokens,
        value: hasTokens ? formatNumber(tokenTotal) : '—',
      },
      {
        key: 'sessions',
        label: kpi.sessions,
        value: formatNumber(Number(chat?.sessionCount ?? 0)),
        domain: 'chat',
      },
      {
        key: 'documents',
        label: kpi.documents,
        value: formatNumber(Number(rag?.documentCount ?? 0)),
        domain: 'rag',
      },
    ];
  });

  readonly healthItems = computed((): DomainHealthItem[] => {
    const overview = this.overviewResource.value();
    if (!overview) {
      return [];
    }
    const health = this.i18n.t().metricsPage.health;
    const agents = overview.domains['agents'] as {
      status?: string;
      agentCount?: number;
      healthyAgentCount?: number;
    } | undefined;
    const mcp = overview.domains['mcp'] as {
      status?: string;
      registeredTools?: number;
      connectedServers?: number;
    } | undefined;
    const system = overview.domains['system'] as { status?: string } | undefined;
    const chatSessions = Number(
      (overview.domains['chat'] as { sessionCount?: number })?.sessionCount ?? 0,
    );
    const ragDocuments = Number(
      (overview.domains['rag'] as { documentCount?: number })?.documentCount ?? 0,
    );
    return [
      {
        domain: 'chat',
        label: health.chat,
        status: 'UP',
        detail: this.i18n.tReplace(health.sessionsDetail, { count: chatSessions }),
      },
      {
        domain: 'rag',
        label: health.rag,
        status: 'UP',
        detail: this.i18n.tReplace(health.documentsDetail, { count: ragDocuments }),
      },
      {
        domain: 'agents',
        label: health.agents,
        status: String(agents?.status ?? 'UP'),
        detail: this.i18n.tReplace(health.healthyDetail, {
          healthy: agents?.healthyAgentCount ?? 0,
          total: agents?.agentCount ?? 0,
        }),
      },
      {
        domain: 'tools',
        label: health.toolsMcp,
        status: String(mcp?.status ?? 'UP'),
        detail: this.i18n.tReplace(health.toolsDetail, {
          tools: mcp?.registeredTools ?? 0,
          servers: mcp?.connectedServers ?? 0,
        }),
      },
      {
        domain: 'vision',
        label: health.vision,
        status: String(system?.status ?? 'UP'),
        detail: health.visionDetail,
      },
    ];
  });

  readonly requestSeries = computed(() => {
    const points = this.seriesResource.value()?.points ?? [];
    return points.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly domainSeries = computed(() => {
    const overview = this.overviewResource.value();
    if (!overview) {
      return [];
    }
    return overview.requestsByDomain.map(item => ({
      label: item.name,
      value: item.count,
    }));
  });

  readonly drilldownItems = computed(
    () => this.drilldownResource.value()?.items ?? [],
  );

  readonly drilldownTotal = computed(
    () => this.drilldownResource.value()?.total ?? 0,
  );

  setRange(range: MetricsRange): void {
    this.range.set(range);
  }

  openDomain(domain: string): void {
    void this.router.navigate(['/metrics', domain], {
      queryParams: { range: this.range() },
    });
  }

  onKpiClick(kpi: MetricsKpi): void {
    if (kpi.domain) {
      this.openDomain(kpi.domain);
    }
  }

  onRowClick(event: InvocationEvent): void {
    if (event.sessionId) {
      void this.router.navigate(['/chat', event.sessionId]);
      return;
    }
    if (event.documentId || event.domain === 'rag') {
      void this.router.navigate(['/rag']);
    }
  }
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat().format(value);
}

function formatPercent(value: number): string {
  return `${(value * 100).toFixed(1)}%`;
}
