import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ChartPanelComponent, type ChartClickPayload } from './chart-panel.component';
import { I18nService } from '../i18n';
import {
  MetricsKpiCardsComponent,
  type MetricsKpi,
} from './metrics-kpi-cards.component';
import { MetricsDrilldownTableComponent } from './metrics-drilldown-table.component';
import {
  isMetricsDomain,
  MetricsService,
  type InvocationEvent,
  type MetricsDomain,
  type MetricsRange,
} from './metrics.service';

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
export class MetricsDomainPageComponent {
  readonly #route = inject(ActivatedRoute);
  readonly #router = inject(Router);
  protected readonly i18n = inject(I18nService);

  readonly #routeDomain = toSignal(
    this.#route.paramMap.pipe(map(params => params.get('domain') ?? '')),
    { initialValue: this.#route.snapshot.paramMap.get('domain') ?? '' },
  );

  readonly #queryParams = toSignal(this.#route.queryParamMap, {
    initialValue: this.#route.snapshot.queryParamMap,
  });

  readonly domain = computed((): MetricsDomain | null => {
    const value = this.#routeDomain();
    return isMetricsDomain(value) ? value : null;
  });

  readonly range = computed((): MetricsRange => {
    const value = this.#queryParams().get('range');
    return value === '30d' ? '30d' : '7d';
  });

  readonly day = computed(() => this.#queryParams().get('day') ?? undefined);
  readonly model = computed(() => this.#queryParams().get('model') ?? undefined);
  readonly page = signal(0);

  readonly #metrics = inject(MetricsService);

  readonly domainResource = this.#metrics.domain(this.domain, this.range);

  readonly docsSeriesResource = this.#metrics.series(() => {
    if (this.domain() !== 'rag') {
      return undefined;
    }
    return { name: 'documents_by_status', domain: 'rag', range: this.range() };
  });

  readonly drilldownResource = this.#metrics.drilldown(() => {
    const domain = this.domain();
    if (!domain) {
      return undefined;
    }
    return {
      domain,
      range: this.range(),
      page: this.page(),
      size: 20,
      day: this.day(),
      model: this.model(),
    };
  });

  readonly title = computed(() => {
    const domain = this.domain();
    if (!domain) {
      return this.i18n.t().metrics.unknownDomainTitle;
    }
    const health = this.i18n.t().metrics.health;
    const labels: Partial<Record<MetricsDomain, string>> = {
      chat: health.chat,
      rag: health.rag,
      agents: health.agents,
      tools: health.toolsMcp,
      vision: health.vision,
    };
    return labels[domain] ?? domain.charAt(0).toUpperCase() + domain.slice(1);
  });

  readonly domainHeading = computed(() => {
    const template = this.i18n.t().metrics.domainTitle;
    return this.i18n.tReplace(template, { domain: this.title() });
  });

  readonly dayFilterLabel = computed(() => {
    const template = this.i18n.t().metrics.dayFilter;
    return this.i18n.tReplace(template, { day: this.day() ?? '' });
  });

  readonly modelFilterLabel = computed(() => {
    const template = this.i18n.t().metrics.modelFilter;
    return this.i18n.tReplace(template, { model: this.model() ?? '' });
  });

  readonly kpis = computed((): MetricsKpi[] => {
    const snapshot = this.domainResource.value();
    if (!snapshot) {
      return [];
    }
    const kpi = this.i18n.t().metrics.kpi;
    return [
      {
        key: 'requests',
        label: kpi.requests,
        value: formatNumber(snapshot.requestCount),
      },
      {
        key: 'errors',
        label: kpi.errors,
        value: formatNumber(snapshot.errorCount),
      },
      {
        key: 'errorRate',
        label: kpi.errorRate,
        value: formatPercent(snapshot.errorRate),
      },
      {
        key: 'p95',
        label: kpi.p95Latency,
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
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { range, day: this.day(), model: this.model() },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  onRequestClick(payload: ChartClickPayload): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { day: payload.label, model: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  onModelClick(payload: ChartClickPayload): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { model: payload.label, day: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  clearFilters(): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { range: this.range(), day: null, model: null },
    });
    this.page.set(0);
  }

  onRowClick(event: InvocationEvent): void {
    if (event.sessionId) {
      void this.#router.navigate(['/chat', event.sessionId]);
      return;
    }
    if (event.documentId || event.domain === 'rag') {
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
