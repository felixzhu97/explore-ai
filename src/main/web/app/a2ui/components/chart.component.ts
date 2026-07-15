import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { CatalogComponent } from '@a2ui/angular/v0_9';
import { NgxEchartsDirective } from 'ngx-echarts';
import { ChartApi, type ChartItem, type ChartType } from '../chart.api';
import { buildEchartsOption, toChartItems } from '../chart-option.util';

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

  readonly chartData = computed((): ChartItem[] => toChartItems(this.props()['chartData']?.value()),
  );

  readonly error = computed(() => {
    if (this.chartData().length === 0) {
      return 'Invalid or empty chart data';
    }
    return null;
  });

  readonly chartOption = computed(() => {
    const title = this.titleText() || undefined;
    return buildEchartsOption(this.chartType(), this.chartData(), title);
  });
}
