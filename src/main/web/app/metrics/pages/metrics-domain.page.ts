import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ChartPanelComponent, type ChartClickPayload } from '../../shared/components/charts';
import { API_BASE_URL } from '../../core/api.constants';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from '../components/metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from '../components/metrics-drilldown-table.component';
import {
  isMetricsDomain,
  type DrilldownPage,
  type InvocationEvent,
  type MetricsDomain,
  type MetricsDomainSnapshot,
  type MetricsRange,
  type SeriesResponse,
} from '../metrics.model';

@Component({
  selector: 'app-metrics-domain-page',
  imports: [
    RouterLink,
    ChartPanelComponent,
    MetricsKpiCardsComponent,
    MetricsDrilldownTableComponent,
  ],
  templateUrl: './metrics-domain.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class MetricsDomainPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly routeDomain = toSignal(
    this.route.paramMap.pipe(map(params => params.get('domain') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('domain') ?? '' },
  );

  private readonly queryParams = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly domain = computed((): MetricsDomain | null => {
    const value = this.routeDomain();
    return isMetricsDomain(value) ? value : null;
  });

  readonly range = computed((): MetricsRange => {
    const value = this.queryParams().get('range');
    return value === '30d' ? '30d' : '7d';
  });

  readonly day = computed(() => this.queryParams().get('day') ?? undefined);
  readonly model = computed(() => this.queryParams().get('model') ?? undefined);
  readonly page = signal(0);

  readonly domainResource = httpResource<MetricsDomainSnapshot>(() => {
    const domain = this.domain();
    if (!domain) {
      return undefined;
    }
    return {
      url: `${API_BASE_URL}/metrics/domains/${domain}`,
      params: { range: this.range() },
    };
  });

  readonly docsSeriesResource = httpResource<SeriesResponse>(() => {
    if (this.domain() !== 'rag') {
      return undefined;
    }
    return {
      url: `${API_BASE_URL}/metrics/series`,
      params: {
        name: 'documents_by_status',
        domain: 'rag',
        range: this.range(),
      },
    };
  });

  readonly drilldownResource = httpResource<DrilldownPage>(() => {
    const domain = this.domain();
    if (!domain) {
      return undefined;
    }
    const params: Record<string, string | number> = {
      domain,
      range: this.range(),
      page: this.page(),
      size: 20,
    };
    const day = this.day();
    const model = this.model();
    if (day) {
      params['day'] = day;
    }
    if (model) {
      params['model'] = model;
    }
    return {
      url: `${API_BASE_URL}/metrics/drilldown`,
      params,
    };
  });

  readonly title = computed(() => {
    const domain = this.domain();
    if (!domain) {
      return 'Unknown domain';
    }
    return domain.charAt(0).toUpperCase() + domain.slice(1);
  });

  readonly kpis = computed((): MetricsKpi[] => {
    const snapshot = this.domainResource.value();
    if (!snapshot) {
      return [];
    }
    return [
      {
        key: 'requests',
        label: 'Requests',
        value: formatNumber(snapshot.requestCount),
      },
      {
        key: 'errors',
        label: 'Errors',
        value: formatNumber(snapshot.errorCount),
      },
      {
        key: 'errorRate',
        label: 'Error rate',
        value: formatPercent(snapshot.errorRate),
      },
      {
        key: 'p95',
        label: 'p95 latency',
        value:
          snapshot.latencyP95Ms == null
            ? '—'
            : `${Math.round(snapshot.latencyP95Ms)} ms`,
      },
    ];
  });

  readonly requestSeries = computed(() => {
    const series = this.domainResource.value()?.requestSeries ?? [];
    return series.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly modelSeries = computed(() => {
    const series = this.domainResource.value()?.modelSeries ?? [];
    return series.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly docsSeries = computed(() => {
    const response = this.docsSeriesResource.value();
    if (!response) {
      return [];
    }
    return response.points.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly drilldownItems = computed(
    () => this.drilldownResource.value()?.items ?? [],
  );

  readonly drilldownTotal = computed(
    () => this.drilldownResource.value()?.total ?? 0,
  );

  setRange(range: MetricsRange): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { range, day: this.day(), model: this.model() },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  onRequestClick(payload: ChartClickPayload): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { day: payload.label, model: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  onModelClick(payload: ChartClickPayload): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { model: payload.label, day: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  clearFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { range: this.range(), day: null, model: null },
    });
    this.page.set(0);
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
