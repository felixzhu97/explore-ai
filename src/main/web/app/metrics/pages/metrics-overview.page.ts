import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ChartPanelComponent } from '../../shared/components/charts';
import {
  MetricsDomainHealthComponent,
  type DomainHealthItem,
} from '../components/metrics-domain-health.component';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from '../components/metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from '../components/metrics-drilldown-table.component';
import type { InvocationEvent, MetricsRange } from '../metrics.model';
import { MetricsService } from '../metrics.service';
import { firstValueFrom } from 'rxjs';

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
  private readonly metricsService = inject(MetricsService);
  private readonly router = inject(Router);

  readonly range = signal<MetricsRange>('7d');

  readonly overviewResource = resource({
    params: () => ({ range: this.range() }),
    loader: ({ params }) => firstValueFrom(this.metricsService.getOverview(params.range)),
  });

  readonly seriesResource = resource({
    params: () => ({ range: this.range() }),
    loader: ({ params }) => firstValueFrom(
      this.metricsService.getSeries('requests', undefined, params.range),
    ),
  });

  readonly domainSeriesResource = resource({
    params: () => ({ range: this.range() }),
    loader: async ({ params }) => {
      const overview = await firstValueFrom(
        this.metricsService.getOverview(params.range),
      );
      return overview.requestsByDomain.map(item => ({
        label: item.name,
        value: item.count,
      }));
    },
  });

  readonly drilldownResource = resource({
    params: () => ({ range: this.range() }),
    loader: ({ params }) => firstValueFrom(
      this.metricsService.getDrilldown({
        page: 0,
        size: 10,
        range: params.range,
      }),
    ),
  });

  readonly kpis = computed((): MetricsKpi[] => {
    const overview = this.overviewResource.value();
    if (!overview) {
      return [];
    }
    const chat = overview.domains['chat'] as { sessionCount?: number } | undefined;
    const rag = overview.domains['rag'] as { documentCount?: number } | undefined;
    const tokenTotal = (overview.promptTokens ?? 0) + (overview.completionTokens ?? 0);
    const hasTokens =
      overview.promptTokens != null || overview.completionTokens != null;
    return [
      {
        key: 'requests',
        label: 'AI requests',
        value: formatNumber(overview.requestCount),
      },
      {
        key: 'errorRate',
        label: 'Error rate',
        value: formatPercent(overview.errorRate),
      },
      {
        key: 'p95',
        label: 'p95 latency',
        value:
          overview.latencyP95Ms == null
            ? '—'
            : `${Math.round(overview.latencyP95Ms)} ms`,
      },
      {
        key: 'tokens',
        label: 'Tokens',
        value: hasTokens ? formatNumber(tokenTotal) : '—',
      },
      {
        key: 'sessions',
        label: 'Sessions',
        value: formatNumber(Number(chat?.sessionCount ?? 0)),
        domain: 'chat',
      },
      {
        key: 'documents',
        label: 'Documents',
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
    return [
      {
        domain: 'chat',
        label: 'Chat',
        status: 'UP',
        detail: `${Number((overview.domains['chat'] as { sessionCount?: number })?.sessionCount ?? 0)} sessions`,
      },
      {
        domain: 'rag',
        label: 'RAG',
        status: 'UP',
        detail: `${Number((overview.domains['rag'] as { documentCount?: number })?.documentCount ?? 0)} documents`,
      },
      {
        domain: 'agents',
        label: 'Agents',
        status: String(agents?.status ?? 'UP'),
        detail: `${agents?.healthyAgentCount ?? 0}/${agents?.agentCount ?? 0} healthy`,
      },
      {
        domain: 'tools',
        label: 'Tools / MCP',
        status: String(mcp?.status ?? 'UP'),
        detail: `${mcp?.registeredTools ?? 0} tools · ${mcp?.connectedServers ?? 0} servers`,
      },
      {
        domain: 'vision',
        label: 'Vision',
        status: String(system?.status ?? 'UP'),
        detail: 'Caption · Detect · OCR',
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

  readonly domainSeries = computed(() => this.domainSeriesResource.value() ?? []);

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
      void this.router.navigate(['/chat'], { queryParams: { session: event.sessionId } });
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
