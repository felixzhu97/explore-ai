import type { EChartsCoreOption } from 'echarts/core';
import type {
  ChartBox,
  ChartCalendarCell,
  ChartCandle,
  ChartHeatmapCell,
  ChartItem,
  ChartLink,
  ChartNamedNode,
  ChartRadarIndicator,
  ChartRiverDatum,
  ChartScatterPoint,
  ChartSeriesItem,
  ChartTreeNode,
  ChartType,
} from './a2ui-chart.api';
import { buildRiverTimeline } from './river-timeline.util';
import { hasItems, hasText } from '../shared/presence';

export interface ChartBuildInput {
  type: ChartType;
  title?: string | undefined;
  chartData?: ChartItem[];
  /** Raw chartData rows before simple {label,value} coercion (LLM multi-value shapes). */
  chartDataRaw?: unknown;
  categories?: string[];
  series?: ChartSeriesItem[];
  points?: ChartScatterPoint[];
  indicators?: ChartRadarIndicator[];
  xLabels?: string[];
  yLabels?: string[];
  cells?: ChartHeatmapCell[];
  value?: number | undefined;
  max?: number | undefined;
  nodes?: ChartTreeNode[];
  links?: ChartLink[];
  boxes?: ChartBox[];
  candles?: ChartCandle[];
  /** Alias for candles (LLM-friendly). */
  ohlc?: ChartCandle[];
  dimensions?: string[];
  rows?: number[][];
  riverData?: ChartRiverDatum[];
  calendarCells?: ChartCalendarCell[];
  layout?: 'force' | 'circular' | undefined;
  range?: string | [string, string] | undefined;
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

function buildTextStyle(
  size = 12,
  weight: number | string = 400,
  color: string = COLOR.text,
) {
  return {
    fontFamily: CHART_FONT,
    fontSize: size,
    fontWeight: weight,
    color,
  };
}

function buildTitleBlock(title?: string) {
  if (!hasText(title)) {
    return undefined;
  }
  return {
    text: title,
    left: 'center' as const,
    top: 4,
    textStyle: buildTextStyle(14, 600, COLOR.text),
    padding: [0, 8, 8, 8],
  };
}

function buildLegendBlock(
  options: { top?: number | string; bottom?: number | string } = {},
) {
  return {
    left: 'center' as const,
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16,
    textStyle: buildTextStyle(12, 400, COLOR.secondary),
    ...options,
  };
}

function buildAxisLabelStyle() {
  return buildTextStyle(11, 400, COLOR.secondary);
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

function buildCategoryAxis(data: string[]) {
  return {
    type: 'category' as const,
    data,
    axisLine: { lineStyle: { color: COLOR.border } },
    axisTick: { show: false },
    axisLabel: { ...buildAxisLabelStyle(), margin: 10 },
  };
}

function buildValueAxis(extra: Record<string, unknown> = {}) {
  return {
    type: 'value' as const,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { ...buildAxisLabelStyle(), margin: 8 },
    splitLine: splitLineStyle(),
    ...extra,
  };
}

function getSeriesColor(index: number): string {
  return COLOR.series[index % COLOR.series.length] ?? COLOR.series[0];
}

function buildSimpleCategoryOption(
  type: 'bar' | 'line',
  data: ChartItem[],
  title?: string,
): EChartsCoreOption {
  return {
    color: [...COLOR.series],
    title: buildTitleBlock(title),
    tooltip: { trigger: 'axis' },
    grid: {
      left: 44,
      right: 16,
      top: hasText(title) ? 52 : 28,
      bottom: 36,
      containLabel: false,
    },
    xAxis: buildCategoryAxis(data.map(item => item.label)),
    yAxis: buildValueAxis(),
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

function buildMultiSeriesOption(
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
    title: buildTitleBlock(title),
    tooltip: { trigger: 'axis' },
    legend: buildLegendBlock({ top: hasText(title) ? 30 : 4 }),
    grid: {
      left: 44,
      right: useDualAxis ? 52 : 16,
      top: hasText(title) ? 68 : 44,
      bottom: 36,
    },
    xAxis: buildCategoryAxis(categories),
    yAxis: useDualAxis
      ? [
          buildValueAxis(),
          buildValueAxis({ splitLine: { show: false } }),
        ]
      : buildValueAxis(),
    series: series.map((item, index) => {
      const kind = item.kind ?? defaultKind;
      const yAxisIndex = useDualAxis && kind === 'line' ? 1 : 0;
      const color = getSeriesColor(index);
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
      if (hasItems(input.categories) && hasItems(input.series)) {
        return buildMultiSeriesOption(type, input.categories, input.series, title);
      }
      if (hasItems(input.chartData)) {
        return buildSimpleCategoryOption(type, input.chartData, title);
      }
      return null;
    }
    case 'pie':
    case 'doughnut': {
      if (!hasItems(input.chartData)) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: type === 'doughnut' ? ['42%', '68%'] : '62%',
            center: ['50%', hasText(title) ? '56%' : '52%'],
            itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
            label: {
              ...buildTextStyle(11, 500, COLOR.text),
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
      if (!hasItems(input.chartData)) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'funnel',
            left: '12%',
            width: '76%',
            top: hasText(title) ? 48 : 28,
            bottom: 16,
            minSize: '18%',
            maxSize: '100%',
            sort: 'descending',
            gap: 6,
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: {
              show: true,
              position: 'inside',
              ...buildTextStyle(12, 600, '#fff'),
              formatter: '{b}',
            },
            data: input.chartData.map((item, index) => ({
              name: item.label,
              value: item.value,
              itemStyle: { color: getSeriesColor(index) },
            })),
          },
        ],
      };
    }
    case 'combo': {
      if (
        !hasItems(input.categories)
        || input.series === undefined
        || input.series.length < 2
      ) {
        return null;
      }
      return buildMultiSeriesOption('bar', input.categories, input.series, title, true);
    }
    case 'scatter': {
      if (!hasItems(input.points)) {
        return null;
      }
      return {
        color: [COLOR.accent],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        grid: { left: 48, right: 20, top: hasText(title) ? 52 : 28, bottom: 40 },
        xAxis: buildValueAxis({ nameGap: 8 }),
        yAxis: buildValueAxis(),
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
            data: input.points.map(point => hasText(point.label)
              ? { value: [point.x, point.y], name: point.label }
              : [point.x, point.y],
            ),
          },
        ],
      };
    }
    case 'radar': {
      if (!hasItems(input.indicators) || !hasItems(input.series)) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: {},
        legend: buildLegendBlock({ bottom: 4 }),
        radar: {
          center: ['50%', hasText(title) ? '54%' : '50%'],
          radius: '48%',
          axisName: {
            ...buildTextStyle(11, 500, COLOR.secondary),
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
              itemStyle: { color: getSeriesColor(index) },
              areaStyle: { color: getSeriesColor(index), opacity: 0.12 },
            })),
          },
        ],
      };
    }
    case 'heatmap': {
      if (
        !hasItems(input.xLabels) || !hasItems(input.yLabels) || !hasItems(input.cells)
      ) {
        return null;
      }
      const values = input.cells.map(cell => cell.value);
      const min = Math.min(...values);
      const max = Math.max(...values);
      return {
        title: buildTitleBlock(title),
        tooltip: {
          position: 'top',
          textStyle: buildTextStyle(12, 400, COLOR.text),
        },
        grid: {
          left: 56,
          right: 20,
          top: hasText(title) ? 52 : 28,
          bottom: 56,
        },
        xAxis: {
          ...buildCategoryAxis(input.xLabels),
          splitArea: { show: false },
        },
        yAxis: {
          ...buildCategoryAxis(input.yLabels),
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
          textStyle: buildTextStyle(11, 400, COLOR.secondary),
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
        title: buildTitleBlock(title),
        series: [
          {
            type: 'gauge',
            min: 0,
            max: gaugeMax,
            startAngle: 210,
            endAngle: -30,
            center: ['50%', hasText(title) ? '58%' : '55%'],
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
              ...buildTextStyle(11, 400, COLOR.secondary),
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
              ...buildTextStyle(28, 600, COLOR.text),
            },
            title: {
              show: Boolean(input.chartData?.[0]?.label),
              offsetCenter: [0, '88%'],
              ...buildTextStyle(12, 400, COLOR.secondary),
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
    case 'treemap': {
      const nodes = hasItems(input.nodes)
        ? input.nodes
        : (hasItems(input.chartData)
            ? input.chartData.map(item => ({ name: item.label, value: item.value }))
            : []);
      if (nodes.length === 0) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'treemap',
            top: hasText(title) ? 40 : 16,
            bottom: 8,
            left: 8,
            right: 8,
            roam: false,
            nodeClick: false,
            breadcrumb: { show: false },
            label: { ...buildTextStyle(11, 600, '#fff'), show: true },
            itemStyle: { borderColor: '#fff', borderWidth: 2, gapWidth: 2 },
            data: nodes,
          },
        ],
      };
    }
    case 'sunburst': {
      const nodes = hasItems(input.nodes)
        ? input.nodes
        : (hasItems(input.chartData)
            ? input.chartData.map(item => ({ name: item.label, value: item.value }))
            : []);
      if (nodes.length === 0) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'sunburst',
            radius: [0, '72%'],
            center: ['50%', hasText(title) ? '56%' : '52%'],
            label: { ...buildTextStyle(11, 500, COLOR.text), rotate: 'radial' },
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            data: nodes,
          },
        ],
      };
    }
    case 'tree': {
      if (!hasItems(input.nodes)) {
        return null;
      }
      const root =
        input.nodes.length === 1
          ? input.nodes[0]
          : { name: hasText(title) ? title : 'Root', children: input.nodes };
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'tree',
            top: hasText(title) ? 48 : 24,
            bottom: 24,
            left: 48,
            right: 80,
            orient: 'LR',
            expandAndCollapse: false,
            initialTreeDepth: 3,
            label: { ...buildTextStyle(11, 500, COLOR.text), position: 'left', verticalAlign: 'middle' },
            lineStyle: { color: COLOR.border, width: 1.5 },
            itemStyle: { color: COLOR.accent, borderColor: COLOR.accent },
            data: [root],
          },
        ],
      };
    }
    case 'sankey': {
      const graphNodes = toNamedNodes(input.nodes);
      if (graphNodes.length === 0 || !hasItems(input.links)) {
        return null;
      }
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'sankey',
            top: hasText(title) ? 48 : 24,
            bottom: 16,
            left: 24,
            right: 24,
            nodeAlign: 'justify',
            emphasis: { focus: 'adjacency' },
            lineStyle: { color: 'gradient', curveness: 0.5, opacity: 0.35 },
            label: { ...buildTextStyle(11, 500, COLOR.text) },
            data: graphNodes.map(node => ({ name: node.name })),
            links: input.links.map(link => ({
              source: link.source,
              target: link.target,
              value: link.value,
            })),
          },
        ],
      };
    }
    case 'graph': {
      const graphNodes = toNamedNodes(input.nodes);
      if (graphNodes.length === 0 || !hasItems(input.links)) {
        return null;
      }
      const layout = input.layout === 'circular' ? 'circular' : 'force';
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'graph',
            layout,
            roam: true,
            draggable: true,
            label: { ...buildTextStyle(11, 500, COLOR.text), show: true, position: 'right' },
            force: { repulsion: 180, edgeLength: 80 },
            lineStyle: { color: COLOR.border, curveness: 0.15, width: 1.5 },
            itemStyle: { borderColor: '#fff', borderWidth: 1 },
            data: graphNodes.map((node, index) => ({
              name: node.name,
              symbolSize: 28,
              itemStyle: { color: getSeriesColor(index) },
            })),
            links: input.links.map(link => ({
              source: link.source,
              target: link.target,
              value: link.value,
              lineStyle: { width: Math.max(1, Math.min(6, link.value / 10)) },
            })),
          },
        ],
      };
    }
    case 'boxplot': {
      const resolved = resolveBoxes(input);
      if (resolved === null) {
        return null;
      }
      return {
        color: [COLOR.accent],
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        grid: { left: 48, right: 20, top: hasText(title) ? 52 : 28, bottom: 40 },
        xAxis: buildCategoryAxis(resolved.categories),
        yAxis: buildValueAxis(),
        series: [
          {
            type: 'boxplot',
            itemStyle: {
              color: 'rgba(0, 122, 255, 0.12)',
              borderColor: COLOR.accent,
              borderWidth: 1.5,
            },
            data: resolved.boxes.map(box => [
              box.min,
              box.q1,
              box.median,
              box.q3,
              box.max,
            ]),
          },
        ],
      };
    }
    case 'candlestick': {
      const resolved = resolveCandles(input);
      if (resolved === null) {
        return null;
      }
      return {
        title: buildTitleBlock(title),
        tooltip: { trigger: 'axis' },
        grid: { left: 48, right: 20, top: hasText(title) ? 52 : 28, bottom: 40 },
        xAxis: buildCategoryAxis(resolved.categories),
        yAxis: buildValueAxis({ scale: true }),
        series: [
          {
            type: 'candlestick',
            itemStyle: {
              color: '#34c759',
              color0: '#ff3b30',
              borderColor: '#34c759',
              borderColor0: '#ff3b30',
            },
            data: resolved.candles.map(candle => [
              candle.open,
              candle.close,
              candle.low,
              candle.high,
            ]),
          },
        ],
      };
    }
    case 'parallel': {
      const resolved = resolveParallel(input);
      if (resolved === null) {
        return null;
      }
      return {
        color: [COLOR.accent],
        title: buildTitleBlock(title),
        tooltip: {},
        parallel: {
          left: 48,
          right: 48,
          top: hasText(title) ? 56 : 32,
          bottom: 32,
          parallelAxisDefault: {
            nameTextStyle: buildTextStyle(11, 500, COLOR.secondary),
            axisLabel: buildAxisLabelStyle(),
            axisLine: { lineStyle: { color: COLOR.border } },
          },
        },
        parallelAxis: resolved.dimensions.map((name, index) => ({
          dim: index,
          name,
        })),
        series: [
          {
            type: 'parallel',
            lineStyle: { width: 1.5, opacity: 0.55, color: COLOR.accent },
            data: resolved.rows,
          },
        ],
      };
    }
    case 'themeRiver': {
      const riverData = resolveRiverData(input);
      if (riverData.length === 0) {
        return null;
      }
      const times: string[] = [];
      const seenTime = new Set<string>();
      for (const item of riverData) {
        if (!seenTime.has(item.time)) {
          seenTime.add(item.time);
          times.push(item.time);
        }
      }
      const timeline = buildRiverTimeline(times);
      const names = [...new Set(riverData.map(item => item.name))];
      const data = riverData.map(item => [
        timeline.toAxisTime(item.time),
        Math.max(0, item.value),
        item.name,
      ]);
      return {
        color: [...COLOR.series],
        title: buildTitleBlock(title),
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'line', lineStyle: { color: COLOR.border, width: 1 } },
        },
        legend: {
          ...buildLegendBlock({ top: hasText(title) ? 28 : 8 }),
          data: names,
        },
        singleAxis: {
          // ThemeRiver needs numeric time; category labels won't draw layers.
          type: 'time',
          top: hasText(title) ? 64 : 40,
          bottom: 40,
          left: 48,
          right: 48,
          boundaryGap: false,
          axisTick: { show: false },
          axisLabel: {
            ...buildAxisLabelStyle(),
            hideOverlap: true,
            formatter: (value: string | number) => timeline.formatLabel(value),
          },
          axisLine: { lineStyle: { color: COLOR.border } },
          splitLine: {
            show: true,
            lineStyle: { color: COLOR.border, type: 'dashed' as const, width: 1 },
          },
        },
        series: [
          {
            type: 'themeRiver',
            emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.12)' } },
            label: { show: false },
            data,
          },
        ],
      };
    }
    case 'calendar': {
      if (input.range === undefined || input.range === '' || !hasItems(input.calendarCells)) {
        return null;
      }
      const values = input.calendarCells.map(cell => cell.value);
      const min = Math.min(...values);
      const max = Math.max(...values);
      return {
        title: buildTitleBlock(title),
        tooltip: { trigger: 'item' },
        visualMap: {
          min,
          max,
          calculable: false,
          orient: 'horizontal',
          left: 'center',
          bottom: 8,
          itemWidth: 12,
          itemHeight: 80,
          inRange: { color: ['#e8f1ff', COLOR.accent] },
          textStyle: buildTextStyle(11, 400, COLOR.secondary),
        },
        calendar: {
          top: hasText(title) ? 56 : 32,
          left: 48,
          right: 24,
          bottom: 56,
          cellSize: ['auto', 14],
          range: input.range,
          itemStyle: { borderWidth: 2, borderColor: '#fff' },
          splitLine: { show: false },
          yearLabel: { show: false },
          monthLabel: { ...buildTextStyle(11, 500, COLOR.secondary) },
          dayLabel: { ...buildTextStyle(10, 400, COLOR.tertiary) },
        },
        series: [
          {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: input.calendarCells.map(cell => [cell.date, cell.value]),
          },
        ],
      };
    }
    default:
      return null;
  }
}

function toNamedNodes(nodes?: ChartTreeNode[]): ChartNamedNode[] {
  if (!hasItems(nodes)) {
    return [];
  }
  return nodes
    .map(node => (typeof node.name === 'string' && node.name !== '' ? { name: node.name } : null))
    .filter((node): node is ChartNamedNode => node !== null);
}

function parseBox(item: unknown): ChartBox | null {
  if (Array.isArray(item) && item.length >= 5) {
    const [min, q1, median, q3, max] = item.slice(0, 5).map(toFiniteNumber);
    if (
      typeof min !== 'number' || typeof q1 !== 'number' || typeof median !== 'number'
      || typeof q3 !== 'number' || typeof max !== 'number'
    ) {
      return null;
    }
    return { min, q1, median, q3, max };
  }
  if (item === null || typeof item !== 'object') {
    return null;
  }
  const row = item as Record<string, unknown>;
  const min = toFiniteNumber(row['min'] ?? row['Min'] ?? row['最小值']);
  const q1 = toFiniteNumber(row['q1'] ?? row['Q1'] ?? row['下四分位']);
  const median = toFiniteNumber(row['median'] ?? row['Median'] ?? row['中位数']);
  const q3 = toFiniteNumber(row['q3'] ?? row['Q3'] ?? row['上四分位']);
  const max = toFiniteNumber(row['max'] ?? row['Max'] ?? row['最大值']);
  if (min === null || q1 === null || median === null || q3 === null || max === null) {
    return null;
  }
  return { min, q1, median, q3, max };
}

function parseCandle(item: unknown): ChartCandle | null {
  if (Array.isArray(item) && item.length >= 4) {
    // ECharts candlestick: [open, close, low, high]
    const [open, close, low, high] = item.slice(0, 4).map(toFiniteNumber);
    if (
      typeof open !== 'number' || typeof close !== 'number'
      || typeof low !== 'number' || typeof high !== 'number'
    ) {
      return null;
    }
    return { open, close, low, high };
  }
  if (item === null || typeof item !== 'object') {
    return null;
  }
  const row = item as Record<string, unknown>;
  const open = toFiniteNumber(row['open'] ?? row['Open'] ?? row['o']);
  const close = toFiniteNumber(row['close'] ?? row['Close'] ?? row['c']);
  const low = toFiniteNumber(row['low'] ?? row['Low'] ?? row['l']);
  const high = toFiniteNumber(row['high'] ?? row['High'] ?? row['h']);
  if (open === null || close === null || low === null || high === null) {
    return null;
  }
  return { open, close, low, high };
}

function toLabeledVectors(value: unknown): { name: string; values: number[] }[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (item === null || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const name = row['label'] ?? row['name'];
      if (typeof name !== 'string' || name === '') {
        return null;
      }
      if (Array.isArray(row['values'])) {
        const values = row['values']
          .map(toFiniteNumber)
          .filter((n): n is number => n !== null);
        if (values.length === 0) {
          return null;
        }
        return { name, values };
      }
      return null;
    })
    .filter((item): item is { name: string; values: number[] } => item !== null);
}

function resolveBoxes(
  input: ChartBuildInput,
): { categories: string[]; boxes: ChartBox[] } | null {
  const fromBoxes = (input.boxes ?? [])
    .map(parseBox)
    .filter((b): b is ChartBox => b !== null);
  if (fromBoxes.length > 0) {
    const categories = hasItems(input.categories)
      ? input.categories.slice(0, fromBoxes.length)
      : fromBoxes.map((_, i) => `C${String(i + 1)}`);
    while (categories.length < fromBoxes.length) {
      categories.push(`C${String(categories.length + 1)}`);
    }
    return { categories, boxes: fromBoxes };
  }
  const fromSeries = hasItems(input.series)
    ? input.series
    : toLabeledVectors(input.chartDataRaw).map(v => ({
        name: v.name,
        values: v.values,
      }));
  if (fromSeries.length !== 0) {
    const boxes = fromSeries
      .map(s => parseBox(s.values))
      .filter((b): b is ChartBox => b !== null);
    if (boxes.length === 0) {
      return null;
    }
    const categories = input.categories?.length === boxes.length
      ? input.categories
      : fromSeries.slice(0, boxes.length).map(s => s.name);
    return { categories, boxes };
  }
  return null;
}

function resolveCandles(
  input: ChartBuildInput,
): { categories: string[]; candles: ChartCandle[] } | null {
  const raw = [...(input.candles ?? []), ...(input.ohlc ?? [])];
  const fromCandles = raw.map(parseCandle).filter((c): c is ChartCandle => c !== null);
  if (fromCandles.length > 0) {
    const categories = hasItems(input.categories)
      ? input.categories.slice(0, fromCandles.length)
      : fromCandles.map((_, i) => `D${String(i + 1)}`);
    while (categories.length < fromCandles.length) {
      categories.push(`D${String(categories.length + 1)}`);
    }
    return { categories, candles: fromCandles };
  }
  // One series with flat OHLC groups, or one candle per series values[0..3]
  const onlySeries = input.series?.length === 1 ? input.series[0] : undefined;
  if (onlySeries !== undefined && onlySeries.values.length >= 4) {
    const values = onlySeries.values;
    const categories = input.categories ?? [];
    if (values.length % 4 === 0 && categories.length === values.length / 4) {
      const candles: ChartCandle[] = [];
      for (let i = 0; i < values.length; i += 4) {
        const candle = parseCandle(values.slice(i, i + 4));
        if (candle === null) {
          return null;
        }
        candles.push(candle);
      }
      return { categories, candles };
    }
  }
  if (hasItems(input.series)) {
    const candles = input.series
      .map(s => parseCandle(s.values))
      .filter((c): c is ChartCandle => c !== null);
    if (candles.length === 0) {
      return null;
    }
    const categories = input.categories?.length === candles.length
      ? input.categories
      : input.series.slice(0, candles.length).map(s => s.name);
    return { categories, candles };
  }
  const vectors = toLabeledVectors(input.chartDataRaw);
  if (vectors.length > 0) {
    const candles = vectors
      .map(v => parseCandle(v.values))
      .filter((c): c is ChartCandle => c !== null);
    if (candles.length > 0) {
      return {
        categories: vectors.slice(0, candles.length).map(v => v.name),
        candles,
      };
    }
  }
  if (Array.isArray(input.chartDataRaw)) {
    const candles: ChartCandle[] = [];
    const categories: string[] = [];
    for (const item of input.chartDataRaw) {
      if (item === null || typeof item !== 'object') {
        continue;
      }
      const row = item as Record<string, unknown>;
      const candle = parseCandle(row);
      if (candle === null) {
        continue;
      }
      const name = row['label'] ?? row['name'] ?? row['date'];
      categories.push(typeof name === 'string' && name !== '' ? name : `D${String(categories.length + 1)}`);
      candles.push(candle);
    }
    if (candles.length > 0) {
      return { categories, candles };
    }
  }
  return null;
}

function resolveParallel(
  input: ChartBuildInput,
): { dimensions: string[]; rows: number[][] } | null {
  const fromRows = toNumberRows(input.rows);
  if (hasItems(input.dimensions) && fromRows.length > 0) {
    const dimCount = input.dimensions.length;
    const rows = fromRows
      .map((row) => {
        if (row.length === dimCount) {
          return row;
        }
        // Drop leading label index if model prepended a numeric id
        if (row.length === dimCount + 1) {
          return row.slice(1);
        }
        if (row.length > dimCount) {
          return row.slice(0, dimCount);
        }
        return null;
      })
      .filter((row): row is number[] => row !== null);
    if (rows.length > 0) {
      return { dimensions: input.dimensions, rows };
    }
  }
  if (hasItems(input.series)) {
    const dimCount = input.dimensions?.length
      ?? Math.max(...input.series.map(s => s.values.length));
    if (dimCount <= 0) {
      return null;
    }
    const dimensions = input.dimensions?.length === dimCount
      ? input.dimensions
      : Array.from({ length: dimCount }, (_, i) => `D${String(i + 1)}`);
    const rows = input.series
      .map(s => s.values.slice(0, dimCount))
      .filter(row => row.length === dimCount);
    if (rows.length === 0) {
      return null;
    }
    return { dimensions, rows };
  }
  const vectors = toLabeledVectors(input.chartDataRaw);
  if (vectors.length > 0) {
    const dimCount = input.dimensions?.length
      ?? Math.max(...vectors.map(v => v.values.length));
    if (dimCount <= 0) {
      return null;
    }
    const dimensions = input.dimensions?.length === dimCount
      ? input.dimensions
      : Array.from({ length: dimCount }, (_, i) => `D${String(i + 1)}`);
    const rows = vectors
      .map(v => v.values.slice(0, dimCount))
      .filter(row => row.length === dimCount);
    if (rows.length > 0) {
      return { dimensions, rows };
    }
  }
  return null;
}

function resolveRiverData(input: ChartBuildInput): ChartRiverDatum[] {
  const direct = toRiverData(input.riverData);
  if (direct.length > 0) {
    return direct;
  }
  if (hasItems(input.categories) && hasItems(input.series)) {
    const out: ChartRiverDatum[] = [];
    for (const series of input.series) {
      input.categories.forEach((time, index) => {
        const value = series.values[index];
        if (typeof value === 'number' && Number.isFinite(value)) {
          out.push({ time, value, name: series.name });
        }
      });
    }
    if (out.length > 0) {
      return out;
    }
  }
  // chartData [{label: topic, values: monthly heats}] + categories as months
  const vectors = toLabeledVectors(input.chartDataRaw);
  if (vectors.length > 0 && hasItems(input.categories)) {
    const out: ChartRiverDatum[] = [];
    for (const vector of vectors) {
      input.categories.forEach((time, index) => {
        const value = vector.values[index];
        if (typeof value === 'number' && Number.isFinite(value)) {
          out.push({ time, value, name: vector.name });
        }
      });
    }
    if (out.length > 0) {
      return out;
    }
  }
  // river-shaped chartData / triples already handled by toRiverData(riverData);
  // also accept chartDataRaw as river triples or {time,value,name}
  const fromRaw = toRiverData(input.chartDataRaw);
  if (fromRaw.length > 0) {
    return fromRaw;
  }
  return [];
}

/** Coerce LLM chartData rows (value may be a number or numeric string). */
export function toChartItems(value: unknown): ChartItem[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (item === null || typeof item !== 'object') {
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
      if (item === null || typeof item !== 'object') {
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
    .map((item): ChartScatterPoint | null => {
      if (item === null || typeof item !== 'object') {
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
      if (item === null || typeof item !== 'object') {
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
      if (item === null || typeof item !== 'object') {
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

function toTreeNode(value: unknown): ChartTreeNode | null {
  if (value === null || typeof value !== 'object') {
    return null;
  }
  const row = value as Record<string, unknown>;
  const name = row['name'];
  if (typeof name !== 'string' || name === '') {
    return null;
  }
  const node: ChartTreeNode = { name };
  const numeric = toFiniteNumber(row['value']);
  if (numeric !== null) {
    node.value = numeric;
  }
  if (Array.isArray(row['children'])) {
    const children = row['children']
      .map(toTreeNode)
      .filter((child): child is ChartTreeNode => child !== null);
    if (children.length > 0) {
      node.children = children;
    }
  }
  return node;
}

export function toTreeNodes(value: unknown): ChartTreeNode[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map(toTreeNode).filter((node): node is ChartTreeNode => node !== null);
}

export function toChartLinks(value: unknown): ChartLink[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (item === null || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const source = row['source'];
      const target = row['target'];
      const linkValue = toFiniteNumber(row['value']);
      if (typeof source !== 'string' || typeof target !== 'string' || linkValue === null) {
        return null;
      }
      return { source, target, value: linkValue };
    })
    .filter((item): item is ChartLink => item !== null);
}

export function toBoxes(value: unknown): ChartBox[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map(parseBox).filter((item): item is ChartBox => item !== null);
}

export function toCandles(value: unknown): ChartCandle[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map(parseCandle).filter((item): item is ChartCandle => item !== null);
}

export function toNumberRows(value: unknown): number[][] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((row) => {
      if (Array.isArray(row)) {
        const numbers = row.map(toFiniteNumber);
        if (numbers.some(n => n === null)) {
          return null;
        }
        return numbers as number[];
      }
      if (row !== null && typeof row === 'object') {
        const record = row as Record<string, unknown>;
        if (Array.isArray(record['values'])) {
          const numbers = record['values'].map(toFiniteNumber);
          if (numbers.some(n => n === null)) {
            return null;
          }
          return numbers as number[];
        }
      }
      return null;
    })
    .filter((row): row is number[] => row !== null);
}

export function toRiverData(value: unknown): ChartRiverDatum[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (Array.isArray(item) && item.length >= 3) {
        const time: unknown = item[0];
        const riverValue = toFiniteNumber(item[1]);
        const name: unknown = item[2];
        if (typeof time === 'string' && riverValue !== null && typeof name === 'string') {
          return { time, value: riverValue, name };
        }
        return null;
      }
      if (item === null || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const time = row['time'] ?? row['date'] ?? row['month'];
      const name = row['name'] ?? row['topic'] ?? row['series'];
      const riverValue = toFiniteNumber(row['value']);
      if (typeof time !== 'string' || typeof name !== 'string' || riverValue === null) {
        return null;
      }
      return { time, value: riverValue, name };
    })
    .filter((item): item is ChartRiverDatum => item !== null);
}

export function toCalendarCells(value: unknown): ChartCalendarCell[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (item === null || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const date = row['date'];
      const cellValue = toFiniteNumber(row['value']);
      if (typeof date !== 'string' || cellValue === null) {
        return null;
      }
      return { date, value: cellValue };
    })
    .filter((item): item is ChartCalendarCell => item !== null);
}

export function toChartRange(value: unknown): string | [string, string] | undefined {
  if (typeof value === 'string' && value !== '') {
    return value;
  }
  if (Array.isArray(value) && value.length === 2
    && typeof value[0] === 'string' && typeof value[1] === 'string') {
    return [value[0], value[1]];
  }
  return undefined;
}

export function toGraphLayout(value: unknown): 'force' | 'circular' | undefined {
  return value === 'force' || value === 'circular' ? value : undefined;
}
