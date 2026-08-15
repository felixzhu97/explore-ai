import { Component, computed } from '@angular/core';
import { CatalogComponent } from '@a2ui/angular/v0_9';
import { NgxEchartsDirective } from 'ngx-echarts';
import { ChartApi, CHART_TYPES, type ChartType } from '../chart.api';
import {
  buildChartOption,
  toChartItems,
  toChartSeries,
  toHeatmapCells,
  toRadarIndicators,
  toScatterPoints,
  toStringList,
} from '../chart-option.util';

@Component({
  selector: 'app-a2ui-chart',
  imports: [NgxEchartsDirective],
  template: `
    @if (error()) {
      <p class="text-sm text-text-secondary">{{ error() }}</p>
    } @else if (chartOption(); as option) {
      <div echarts [options]="option" class="h-72 w-full min-w-0"></div>
    }
  `,
  host: { class: 'block w-full min-w-0 my-2' },
})
export class ChartComponent extends CatalogComponent<typeof ChartApi> {
  readonly chartType = computed((): ChartType | null => {
    const value = this.props()['type']?.value();
    if (typeof value === 'string' && (CHART_TYPES as readonly string[]).includes(value)) {
      return value as ChartType;
    }
    return null;
  });

  readonly titleText = computed(() => {
    const value = this.props()['title']?.value();
    return typeof value === 'string' ? value : '';
  });

  readonly chartData = computed(() => toChartItems(this.props()['chartData']?.value()));

  readonly categories = computed(() => toStringList(this.props()['categories']?.value()));

  readonly series = computed(() => toChartSeries(this.props()['series']?.value()));

  readonly points = computed(() => toScatterPoints(this.props()['points']?.value()));

  readonly indicators = computed(() => toRadarIndicators(this.props()['indicators']?.value()));

  readonly xLabels = computed(() => toStringList(this.props()['xLabels']?.value()));

  readonly yLabels = computed(() => toStringList(this.props()['yLabels']?.value()));

  readonly cells = computed(() => toHeatmapCells(this.props()['cells']?.value()));

  readonly gaugeValue = computed(() => {
    const value = this.props()['value']?.value();
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
  });

  readonly gaugeMax = computed(() => {
    const value = this.props()['max']?.value();
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
  });

  readonly chartOption = computed(() => {
    const type = this.chartType();
    if (!type) {
      return null;
    }
    return buildChartOption({
      type,
      title: this.titleText() || undefined,
      chartData: this.chartData(),
      categories: this.categories(),
      series: this.series(),
      points: this.points(),
      indicators: this.indicators(),
      xLabels: this.xLabels(),
      yLabels: this.yLabels(),
      cells: this.cells(),
      value: this.gaugeValue(),
      max: this.gaugeMax(),
    });
  });

  readonly error = computed(() => {
    if (!this.chartType()) {
      return 'Unsupported or missing chart type';
    }
    if (!this.chartOption()) {
      return 'Invalid or empty chart data';
    }
    return null;
  });
}
