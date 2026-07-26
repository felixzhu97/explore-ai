import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface MetricsKpi {
  key: string;
  label: string;
  value: string;
  domain?: string;
}

@Component({
  selector: 'app-metrics-kpi-cards',
  template: `
    <div class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
      @for (kpi of items(); track kpi.key) {
        <button
          type="button"
          class="rounded-xl border border-black/10 bg-card p-3 text-left transition hover:border-black/20"
          (click)="kpiClick.emit(kpi)"
        >
          <p class="text-xs text-muted-foreground">{{ kpi.label }}</p>
          <p class="mt-1 text-xl font-semibold tracking-tight text-foreground">{{ kpi.value }}</p>
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetricsKpiCardsComponent {
  readonly items = input<MetricsKpi[]>([]);
  readonly kpiClick = output<MetricsKpi>();
}
