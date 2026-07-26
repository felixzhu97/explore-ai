import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import type { ECElementEvent } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import {
  buildSharedChartOption,
  type SharedChartItem,
  type SharedChartType,
} from './chart-option.util';

export interface ChartClickPayload {
  label: string;
  value: number;
}

@Component({
  selector: 'app-chart-panel',
  imports: [NgxEchartsDirective],
  template: `
    <div class="flex min-h-0 flex-col gap-2">
      @if (heading()) {
        <h3 class="text-sm font-medium text-foreground">{{ heading() }}</h3>
      }
      @if (data().length === 0) {
        <p class="py-10 text-center text-sm text-muted-foreground">{{ emptyText() }}</p>
      } @else {
        <div
          echarts
          [options]="chartOption()"
          class="h-64 w-full"
          (chartClick)="onChartClick($event)"
        ></div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-full' },
})
export class ChartPanelComponent {
  readonly type = input<SharedChartType>('line');
  readonly data = input<SharedChartItem[]>([]);
  readonly heading = input('');
  readonly title = input('');
  readonly emptyText = input('No data yet');
  readonly chartClick = output<ChartClickPayload>();

  readonly chartOption = computed(() => {
    const title = this.title() || undefined;
    return buildSharedChartOption(this.type(), this.data(), title);
  });

  onChartClick(event: ECElementEvent): void {
    const label = String(event.name ?? '');
    const raw = Array.isArray(event.value) ? event.value[1] : event.value;
    const value = typeof raw === 'number' ? raw : Number(raw);
    if (!label || !Number.isFinite(value)) {
      return;
    }
    this.chartClick.emit({ label, value });
  }
}
