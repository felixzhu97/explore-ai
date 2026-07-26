import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { ChartPanelComponent, type ChartClickPayload } from '../../shared/components/charts';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from '../components/metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from '../components/metrics-drilldown-table.component';
import {
  isMetricsDomain,
  type InvocationEvent,
  type MetricsDomain,
  type MetricsRange,
} from '../metrics.model';
import { MetricsService } from '../metrics.service';

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
  private readonly metricsService = inject(MetricsService);
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

  readonly domainResource = resource({
    params: () => ({ domain: this.domain(), range: this.range() }),
    loader: ({ params }) => {
      if (!params.domain) {
        return Promise.resolve(null);
      }
      return firstValueFrom(this.metricsService.getDomain(params.domain, params.range));
    },
  });

  readonly docsSeriesResource = resource({
    params: () => ({ domain: this.domain(), range: this.range() }),
    loader: ({ params }) => {
      if (params.domain !== 'rag') {
        return Promise.resolve([]);
      }
      return firstValueFrom(
        this.metricsService.getSeries(
          'documents_by_status',
          'rag',
          params.range,
        ),
      ).then(response => response.points.map(point => ({
        label: point.label,
        value: point.value,
      })),
      );
    },
  });

  readonly drilldownResource = resource({
    params: () => ({
      domain: this.domain(),
      range: this.range(),
      day: this.day(),
      model: this.model(),
      page: this.page(),
    }),
    loader: ({ params }) => {
      if (!params.domain) {
        return Promise.resolve({ items: [], total: 0, page: 0, size: 20 });
      }
      return firstValueFrom(
        this.metricsService.getDrilldown({
          domain: params.domain,
          range: params.range,
          day: params.day,
          model: params.model,
          page: params.page,
          size: 20,
        }),
      );
    },
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

  readonly docsSeries = computed(
    () => this.docsSeriesResource.value() ?? [],
  );

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
