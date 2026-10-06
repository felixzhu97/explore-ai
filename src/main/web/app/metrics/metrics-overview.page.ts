import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ChartPanelComponent } from './chart-panel.component';
import { I18nService } from '../i18n';
import {
  MetricsDomainHealthComponent,
  type DomainHealthItem,
} from './metrics-domain-health.component';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from './metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from './metrics-drilldown-table.component';
import {
  MetricsService,
  type InvocationEvent,
  type MetricsRange,
} from './metrics.service';
import { hasText } from '../shared/presence';

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
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class MetricsOverviewPageComponent {
  readonly #router = inject(Router);
  readonly #metrics = inject(MetricsService);
  protected readonly i18n = inject(I18nService);

  readonly range = signal<MetricsRange>('7d');
  readonly overviewResource = this.#metrics.getOverview(this.range);

  readonly kpis = computed((): MetricsKpi[] => {
    const overview = this.overviewResource.value();
    if (overview === undefined) {
      return [];
    }
    const kpi = this.i18n.t().metrics.kpi;
    const { chat, rag } = overview.domains;
    const tokenTotal = (overview.promptTokens ?? 0) + (overview.completionTokens ?? 0);
    const hasTokens =
      overview.promptTokens !== null || overview.completionTokens !== null;
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
          overview.latencyP95Ms === null
            ? '—'
            : `${String(Math.round(overview.latencyP95Ms))} ms`,
      },
      {
        key: 'tokens',
        label: kpi.tokens,
        value: hasTokens ? formatNumber(tokenTotal) : '—',
      },
      {
        key: 'sessions',
        label: kpi.sessions,
        value: formatNumber(chat.sessionCount),
        domain: 'chat',
      },
      {
        key: 'documents',
        label: kpi.documents,
        value: formatNumber(rag.documentCount),
        domain: 'rag',
      },
    ];
  });

  readonly seriesResource = this.#metrics.getSeries(() => ({ name: 'requests', range: this.range() }));

  readonly requestSeries = computed(() => {
    const points = this.seriesResource.value()?.points ?? [];
    return points.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly domainSeries = computed(() => {
    const overview = this.overviewResource.value();
    if (overview === undefined) {
      return [];
    }
    return overview.requestsByDomain.map(item => ({
      label: item.name,
      value: item.count,
    }));
  });

  readonly healthItems = computed((): DomainHealthItem[] => {
    const overview = this.overviewResource.value();
    if (overview === undefined) {
      return [];
    }
    const health = this.i18n.t().metrics.health;
    const { chat, rag, agents, mcp, system } = overview.domains;
    return [
      {
        domain: 'chat',
        label: health.chat,
        status: 'UP',
        detail: this.i18n.tReplace(health.sessionsDetail, { count: chat.sessionCount }),
      },
      {
        domain: 'rag',
        label: health.rag,
        status: 'UP',
        detail: this.i18n.tReplace(health.documentsDetail, { count: rag.documentCount }),
      },
      {
        domain: 'agents',
        label: health.agents,
        status: agents.status,
        detail: this.i18n.tReplace(health.healthyDetail, {
          healthy: agents.healthyAgentCount,
          total: agents.agentCount,
        }),
      },
      {
        domain: 'tools',
        label: health.toolsMcp,
        status: mcp.status,
        detail: this.i18n.tReplace(health.toolsDetail, {
          tools: mcp.registeredTools,
          servers: mcp.connectedServers,
        }),
      },
      {
        domain: 'vision',
        label: health.vision,
        status: system.status,
        detail: health.visionDetail,
      },
    ];
  });

  readonly drilldownResource = this.#metrics.getDrilldown(() => ({
    page: 0,
    size: 10,
    range: this.range(),
  }));

  readonly drilldownItems = computed(
    () => this.drilldownResource.value()?.items ?? [],
  );

  readonly drilldownTotal = computed(
    () => this.drilldownResource.value()?.total ?? 0,
  );

  setRange(range: MetricsRange): void {
    this.range.set(range);
  }

  onKpiClick(kpi: MetricsKpi): void {
    if (hasText(kpi.domain)) {
      this.openDomain(kpi.domain);
    }
  }

  openDomain(domain: string): void {
    void this.#router.navigate(['/metrics', domain], {
      queryParams: { range: this.range() },
    });
  }

  onRowClick(event: InvocationEvent): void {
    if (hasText(event.sessionId)) {
      void this.#router.navigate(['/chat', event.sessionId]);
      return;
    }
    if (hasText(event.documentId) || event.domain === 'rag') {
      void this.#router.navigate(['/rag']);
    }
  }
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat().format(value);
}

function formatPercent(value: number): string {
  return `${(value * 100).toFixed(1)}%`;
}
