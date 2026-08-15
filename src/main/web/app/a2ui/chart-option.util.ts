import type { EChartsCoreOption } from 'echarts/core';
import type {
  ChartHeatmapCell,
  ChartItem,
  ChartRadarIndicator,
  ChartScatterPoint,
  ChartSeriesItem,
  ChartType,
} from './chart.api';

export interface ChartBuildInput {
  type: ChartType;
  title?: string;
  chartData?: ChartItem[];
  categories?: string[];
  series?: ChartSeriesItem[];
  points?: ChartScatterPoint[];
  indicators?: ChartRadarIndicator[];
  xLabels?: string[];
  yLabels?: string[];
  cells?: ChartHeatmapCell[];
  value?: number;
  max?: number;
}

/** Align with `styles.css` tokens (SF Pro / Apple HIG). */
const CHART_FONT =
  '"SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';

const COLOR = {
  text: '#1d1d1f',
  secondary: '#86868b',
  tertiary: '#6e6e73',
  border: 'rgba(0, 0, 0, 0.08)',
  surfaceSecondary: '#f5f5f7',
  /** `--chart-1` */
  accent: '#007aff',
  series: ['#007aff', '#34c759', '#af52de', '#ff9500', '#5856d6', '#ff2d55'],
} as const;

function textStyle(size = 12, weight: number | string = 400, color: string = COLOR.text) {
  return {
    fontFamily: CHART_FONT,
    fontSize: size,
    fontWeight: weight,
    color,
  };
}

function titleBlock(title?: string) {
  if (!title) {
    return undefined;
  }
  return {
    text: title,
    left: 'center' as const,
    top: 4,
    textStyle: textStyle(14, 600, COLOR.text),
    padding: [0, 8, 8, 8],
  };
}

function legendBlock(opts: { top?: number | string; bottom?: number | string } = {}) {
  return {
    left: 'center' as const,
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16,
    textStyle: textStyle(12, 400, COLOR.secondary),
    ...opts,
  };
}

function axisLabelStyle() {
  return textStyle(11, 400, COLOR.secondary);
}

function splitLineStyle() {
  return {
    lineStyle: {
      color: COLOR.border,
      type: 'solid' as const,
      width: 1,
    },
  };
}

function categoryAxis(data: string[]) {
  return {
    type: 'category' as const,
    data,
    axisLine: { lineStyle: { color: COLOR.border } },
    axisTick: { show: false },
    axisLabel: { ...axisLabelStyle(), margin: 10 },
  };
}

function valueAxis(extra: Record<string, unknown> = {}) {
  return {
    type: 'value' as const,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { ...axisLabelStyle(), margin: 8 },
    splitLine: splitLineStyle(),
    ...extra,
  };
}

function seriesColor(index: number): string {
  return COLOR.series[index % COLOR.series.length]!;
}

function simpleCategoryOption(
  type: 'bar' | 'line',
  data: ChartItem[],
  title?: string,
): EChartsCoreOption {
  return {
    color: [...COLOR.series],
    title: titleBlock(title),
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: title ? 52 : 28, bottom: 36, containLabel: false },
    xAxis: categoryAxis(data.map(item => item.label)),
    yAxis: valueAxis(),
    series: [
      {
        type,
        data: data.map(item => item.value),
        itemStyle: { color: COLOR.accent, borderRadius: type === 'bar' ? [4, 4, 0, 0] : 0 },
        ...(type === 'line'
          ? { smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { width: 2.5 } }
          : { barMaxWidth: 36 }),
      },
    ],
  };
}

function multiSeriesOption(
  defaultKind: 'bar' | 'line',
  categories: string[],
  series: ChartSeriesItem[],
  title?: string,
  dualAxis = false,
): EChartsCoreOption {
  const useDualAxis =
    dualAxis
    && series.some(item => (item.kind ?? defaultKind) === 'bar')
    && series.some(item => (item.kind ?? defaultKind) === 'line');

  return {
    color: [...COLOR.series],
    title: titleBlock(title),
    tooltip: { trigger: 'axis' },
    legend: legendBlock({ top: title ? 30 : 4 }),
    grid: {
      left: 44,
      right: useDualAxis ? 52 : 16,
      top: title ? 68 : 44,
      bottom: 36,
    },
    xAxis: categoryAxis(categories),
    yAxis: useDualAxis
      ? [
          valueAxis(),
          valueAxis({ splitLine: { show: false } }),
        ]
      : valueAxis(),
    series: series.map((item, index) => {
      const kind = item.kind ?? defaultKind;
      const yAxisIndex = useDualAxis && kind === 'line' ? 1 : 0;
      const color = seriesColor(index);
      return {
        name: item.name,
        type: kind,
        data: item.values,
        itemStyle: {
          color,
          ...(kind === 'bar' ? { borderRadius: [4, 4, 0, 0] } : {}),
        },
        ...(useDualAxis ? { yAxisIndex } : {}),
        ...(kind === 'line'
          ? { smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { width: 2.5, color } }
          : { barMaxWidth: 28 }),
      };
    }),
  };
}

/**
 * Map A2UI Chart props to an ECharts option (no executable code).
 * Returns null when required data is missing or empty.
 */
export function buildChartOption(input: ChartBuildInput): EChartsCoreOption | null {
  const { type, title } = input;

  switch (type) {
    case 'bar':
    case 'line': {
      if (input.categories?.length && input.series?.length) {
        return multiSeriesOption(type, input.categories, input.series, title);
      }
      if (input.chartData?.length) {
        return simpleCategoryOption(type, input.chartData, title);
      }
      return null;
    }
    case 'pie':
    case 'doughnut': {
      if (!input.chartData?.length) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: titleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: type === 'doughnut' ? ['42%', '68%'] : '62%',
            center: ['50%', title ? '56%' : '52%'],
            itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
            label: {
              ...textStyle(11, 500, COLOR.text),
              formatter: '{b}',
            },
            labelLine: { length: 10, length2: 8, lineStyle: { color: COLOR.border } },
            data: input.chartData.map(item => ({
              name: item.label,
              value: item.value,
            })),
          },
        ],
      };
    }
    case 'funnel': {
      if (!input.chartData?.length) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: titleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'funnel',
            left: '12%',
            width: '76%',
            top: title ? 48 : 28,
            bottom: 16,
            minSize: '18%',
            maxSize: '100%',
            sort: 'descending',
            gap: 6,
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: {
              show: true,
              position: 'inside',
              ...textStyle(12, 600, '#fff'),
              formatter: '{b}',
            },
            data: input.chartData.map((item, index) => ({
              name: item.label,
              value: item.value,
              itemStyle: { color: seriesColor(index) },
            })),
          },
        ],
      };
    }
    case 'combo': {
      if (!input.categories?.length || !input.series || input.series.length < 2) {
        return null;
      }
      return multiSeriesOption('bar', input.categories, input.series, title, true);
    }
    case 'scatter': {
      if (!input.points?.length) {
        return null;
      }
      return {
        color: [COLOR.accent],
        title: titleBlock(title),
        tooltip: { trigger: 'item' },
        grid: { left: 48, right: 20, top: title ? 52 : 28, bottom: 40 },
        xAxis: valueAxis({ nameGap: 8 }),
        yAxis: valueAxis(),
        series: [
          {
            type: 'scatter',
            symbolSize: 10,
            itemStyle: {
              color: COLOR.accent,
              opacity: 0.85,
              borderColor: '#fff',
              borderWidth: 1,
            },
            data: input.points.map(point => point.label
              ? { value: [point.x, point.y], name: point.label }
              : [point.x, point.y],
            ),
          },
        ],
      };
    }
    case 'radar': {
      if (!input.indicators?.length || !input.series?.length) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: titleBlock(title),
        tooltip: {},
        legend: legendBlock({ bottom: 4 }),
        radar: {
          center: ['50%', title ? '54%' : '50%'],
          radius: '48%',
          axisName: {
            ...textStyle(11, 500, COLOR.secondary),
            padding: [4, 4],
          },
          axisNameGap: 10,
          splitNumber: 4,
          axisLine: { lineStyle: { color: COLOR.border } },
          splitLine: { lineStyle: { color: COLOR.border } },
          splitArea: {
            show: true,
            areaStyle: {
              color: ['rgba(0, 0, 0, 0.015)', 'rgba(0, 0, 0, 0.03)'],
            },
          },
          indicator: input.indicators.map(item => ({
            name: item.name,
            max: item.max,
          })),
        },
        series: [
          {
            type: 'radar',
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 2 },
            data: input.series.map((item, index) => ({
              name: item.name,
              value: item.values,
              itemStyle: { color: seriesColor(index) },
              areaStyle: { color: seriesColor(index), opacity: 0.12 },
            })),
          },
        ],
      };
    }
    case 'heatmap': {
      if (!input.xLabels?.length || !input.yLabels?.length || !input.cells?.length) {
        return null;
      }
      const values = input.cells.map(cell => cell.value);
      const min = Math.min(...values);
      const max = Math.max(...values);
      return {
        title: titleBlock(title),
        tooltip: {
          position: 'top',
          textStyle: textStyle(12, 400, COLOR.text),
        },
        grid: {
          left: 56,
          right: 20,
          top: title ? 52 : 28,
          bottom: 56,
        },
        xAxis: {
          ...categoryAxis(input.xLabels),
          splitArea: { show: false },
        },
        yAxis: {
          ...categoryAxis(input.yLabels),
          splitArea: { show: false },
        },
        visualMap: {
          min,
          max,
          calculable: false,
          orient: 'horizontal',
          left: 'center',
          bottom: 8,
          itemWidth: 12,
          itemHeight: 140,
          text: ['', ''],
          textStyle: textStyle(11, 400, COLOR.secondary),
          inRange: {
            color: ['#e8f1ff', '#7eb6ff', COLOR.accent],
          },
        },
        series: [
          {
            type: 'heatmap',
            data: input.cells.map(cell => [cell.x, cell.y, cell.value]),
            label: { show: false },
            itemStyle: {
              borderColor: COLOR.surfaceSecondary,
              borderWidth: 3,
              borderRadius: 4,
            },
          },
        ],
      };
    }
    case 'gauge': {
      const gaugeValue =
        typeof input.value === 'number'
          ? input.value
          : input.chartData?.[0]?.value;
      if (typeof gaugeValue !== 'number' || !Number.isFinite(gaugeValue)) {
        return null;
      }
      const gaugeMax =
        typeof input.max === 'number' && Number.isFinite(input.max) ? input.max : 100;
      return {
        title: titleBlock(title),
        series: [
          {
            type: 'gauge',
            min: 0,
            max: gaugeMax,
            startAngle: 210,
            endAngle: -30,
            center: ['50%', title ? '58%' : '55%'],
            radius: '78%',
            progress: {
              show: true,
              width: 12,
              roundCap: true,
              itemStyle: { color: COLOR.accent },
            },
            axisLine: {
              roundCap: true,
              lineStyle: {
                width: 12,
                color: [[1, '#e5e5ea']],
              },
            },
            axisTick: {
              show: true,
              splitNumber: 4,
              distance: -16,
              length: 4,
              lineStyle: { color: COLOR.tertiary, width: 1 },
            },
            splitLine: {
              distance: -18,
              length: 8,
              lineStyle: { color: COLOR.secondary, width: 1.5 },
            },
            axisLabel: {
              ...textStyle(11, 400, COLOR.secondary),
              distance: 18,
            },
            pointer: {
              length: '58%',
              width: 4,
              itemStyle: { color: COLOR.accent },
            },
            anchor: {
              show: true,
              showAbove: true,
              size: 10,
              itemStyle: { color: COLOR.accent },
            },
            detail: {
              valueAnimation: true,
              formatter: '{value}',
              offsetCenter: [0, '72%'],
              ...textStyle(28, 600, COLOR.text),
            },
            title: {
              show: Boolean(input.chartData?.[0]?.label),
              offsetCenter: [0, '88%'],
              ...textStyle(12, 400, COLOR.secondary),
            },
            data: [
              {
                value: gaugeValue,
                name: input.chartData?.[0]?.label,
              },
            ],
          },
        ],
      };
    }
    default:
      return null;
  }
}

/** @deprecated Prefer buildChartOption — kept for simple call sites / tests. */
export function buildEchartsOption(
  type: ChartType,
  data: ChartItem[],
  title?: string,
): EChartsCoreOption | null {
  return buildChartOption({ type, chartData: data, title });
}

/** Coerce LLM chartData rows (value may be a number or numeric string). */
export function toChartItems(value: unknown): ChartItem[] {
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
      const rawValue = row['value'];
      const numeric = typeof rawValue === 'number' ? rawValue : Number(rawValue);
      if (typeof label !== 'string' || !Number.isFinite(numeric)) {
        return null;
      }
      return { label, value: numeric };
    })
    .filter((item): item is ChartItem => item !== null);
}

function toFiniteNumber(value: unknown): number | null {
  const numeric = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function toStringList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === 'string');
}

export function toChartSeries(value: unknown): ChartSeriesItem[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (!item || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const name = row['name'];
      const valuesRaw = row['values'];
      const kind = row['kind'];
      if (typeof name !== 'string' || !Array.isArray(valuesRaw)) {
        return null;
      }
      const values = valuesRaw
        .map(toFiniteNumber)
        .filter((n): n is number => n !== null);
      if (values.length === 0) {
        return null;
      }
      const series: ChartSeriesItem = { name, values };
      if (kind === 'bar' || kind === 'line') {
        series.kind = kind;
      }
      return series;
    })
    .filter((item): item is ChartSeriesItem => item !== null);
}

export function toScatterPoints(value: unknown): ChartScatterPoint[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (!item || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const x = toFiniteNumber(row['x']);
      const y = toFiniteNumber(row['y']);
      if (x === null || y === null) {
        return null;
      }
      const label = row['label'];
      return typeof label === 'string' ? { x, y, label } : { x, y };
    })
    .filter((item): item is ChartScatterPoint => item !== null);
}

export function toRadarIndicators(value: unknown): ChartRadarIndicator[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (!item || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const name = row['name'];
      const max = toFiniteNumber(row['max']);
      if (typeof name !== 'string' || max === null) {
        return null;
      }
      return { name, max };
    })
    .filter((item): item is ChartRadarIndicator => item !== null);
}

export function toHeatmapCells(value: unknown): ChartHeatmapCell[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (!item || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const x = toFiniteNumber(row['x']);
      const y = toFiniteNumber(row['y']);
      const cellValue = toFiniteNumber(row['value']);
      if (x === null || y === null || cellValue === null) {
        return null;
      }
      return { x, y, value: cellValue };
    })
    .filter((item): item is ChartHeatmapCell => item !== null);
}
