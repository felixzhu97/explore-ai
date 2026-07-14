import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { CatalogComponent } from '@a2ui/angular/v0_9';
import { NgxEchartsDirective } from 'ngx-echarts';
import { ChartApi, type ChartItem, type ChartType } from '../chart.api';
import { buildEchartsOption } from '../chart-option.util';

@Component({
  selector: 'app-a2ui-chart',
  imports: [NgxEchartsDirective],
  template: `
    @if (error()) {
      <p class="text-sm text-text-secondary">{{ error() }}</p>
    } @else {
      <div echarts [options]="chartOption()" class="h-64 w-full"></div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-full my-2' },
})
export class ChartComponent extends CatalogComponent<typeof ChartApi> {
  readonly chartType = computed((): ChartType => {
    const value = this.props()['type']?.value();
    if (value === 'bar' || value === 'line' || value === 'pie' || value === 'doughnut') {
      return value;
    }
    return 'bar';
  });

  readonly titleText = computed(() => {
    const value = this.props()['title']?.value();
    return typeof value === 'string' ? value : '';
  });

  readonly chartData = computed((): ChartItem[] => {
    const value = this.props()['chartData']?.value();
    if (!Array.isArray(value)) {
      return [];
    }
    return value
      .map((item) => {
        if (!item || typeof item !== 'object') {
          return null;
        }
        const row = item as Record<string, unknown>;
        const label = row['label'];
        const numeric = row['value'];
        if (typeof label !== 'string' || typeof numeric !== 'number') {
          return null;
        }
        return { label, value: numeric };
      })
      .filter((item): item is ChartItem => item !== null);
  });

  readonly error = computed(() => {
    if (this.chartData().length === 0) {
      return '图表数据无效或为空';
    }
    return null;
  });

  readonly chartOption = computed(() => {
    const title = this.titleText() || undefined;
    return buildEchartsOption(this.chartType(), this.chartData(), title);
  });
}
