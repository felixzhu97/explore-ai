import { Component, computed, inject, signal } from '@angular/core';
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
import { hasText, textOr } from '../shared/presence';

@Component({
  selector: 'app-metrics-domain-page',
  imports: [
    RouterLink,
    ChartPanelComponent,
    MetricsKpiCardsComponent,
    MetricsDrilldownTableComponent,
  ],
  templateUrl: './metrics-domain.page.html',
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class MetricsDomainPageComponent {
  readonly #route = inject(ActivatedRoute);
  readonly #router = inject(Router);
  readonly #metrics = inject(MetricsService);
  protected readonly i18n = inject(I18nService);

  readonly #routeDomain = toSignal(
    this.#route.paramMap.pipe(map(params => params.get('domain') ?? '')),
    { initialValue: this.#route.snapshot.paramMap.get('domain') ?? '' },
  );

  readonly domain = computed((): MetricsDomain | null => {
    const value = this.#routeDomain();
    return isMetricsDomain(value) ? value : null;
  });

  readonly title = computed(() => {
    const domain = this.domain();
    if (domain === null) {
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

  readonly #queryParams = toSignal(this.#route.queryParamMap, {
    initialValue: this.#route.snapshot.queryParamMap,
  });

  readonly day = computed(() => textOr(this.#queryParams().get('day'), undefined));

  readonly dayFilterLabel = computed(() => {
    const template = this.i18n.t().metrics.dayFilter;
    return this.i18n.tReplace(template, { day: this.day() ?? '' });
  });

  readonly model = computed(() => textOr(this.#queryParams().get('model'), undefined));

  readonly modelFilterLabel = computed(() => {
    const template = this.i18n.t().metrics.modelFilter;
    return this.i18n.tReplace(template, { model: this.model() ?? '' });
  });

  readonly range = computed((): MetricsRange => {
    const value = this.#queryParams().get('range');
    return value === '30d' ? '30d' : '7d';
  });

  readonly domainResource = this.#metrics.getDomain(this.domain, this.range);

  readonly kpis = computed((): MetricsKpi[] => {
    const snapshot = this.domainResource.value();
    if (snapshot === undefined) {
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
          snapshot.latencyP95Ms === null
            ? '—'
            : `${String(Math.round(snapshot.latencyP95Ms))} ms`,
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

  readonly docsSeriesResource = this.#metrics.getSeries(() => {
    if (this.domain() !== 'rag') {
      return undefined;
    }
    return { name: 'documents_by_status', domain: 'rag', range: this.range() };
  });

  readonly docsSeries = computed(() => {
    const response = this.docsSeriesResource.value();
    if (response === undefined) {
      return [];
    }
    return response.points.map(point => ({
      label: point.label,
      value: point.value,
    }));
  });

  readonly page = signal(0);

  readonly drilldownResource = this.#metrics.getDrilldown(() => {
    const domain = this.domain();
    if (domain === null) {
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

  readonly drilldownItems = computed(
    () => this.drilldownResource.value()?.items ?? [],
  );

  readonly drilldownTotal = computed(
    () => this.drilldownResource.value()?.total ?? 0,
  );

  /** Clears the day and model filters. */
  clearFilters(): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { range: this.range(), day: null, model: null },
    });
    this.page.set(0);
  }

  /** Switches the time range. */
  setRange(range: MetricsRange): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { range, day: this.day(), model: this.model() },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  /** Filters by the clicked day. */
  onRequestClick(payload: ChartClickPayload): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { day: payload.label, model: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  /** Filters by the clicked model. */
  onModelClick(payload: ChartClickPayload): void {
    void this.#router.navigate([], {
      relativeTo: this.#route,
      queryParams: { model: payload.label, day: null },
      queryParamsHandling: 'merge',
    });
    this.page.set(0);
  }

  /** Opens the chat or document behind the row. */
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
