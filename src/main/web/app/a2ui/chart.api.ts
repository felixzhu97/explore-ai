import { z } from 'zod';
import type { ComponentApi } from '@a2ui/web_core/v0_9';

const ChartItemSchema = z.object({
  label: z.string(),
  value: z.number().optional(),
  values: z.array(z.number()).optional(),
  open: z.number().optional(),
  close: z.number().optional(),
  low: z.number().optional(),
  high: z.number().optional(),
});

const PathBindingSchema = z.object({ path: z.string() });

const SeriesItemSchema = z.object({
  name: z.string(),
  values: z.array(z.number()),
  kind: z.enum(['bar', 'line']).optional(),
});

const ScatterPointSchema = z.object({
  x: z.number(),
  y: z.number(),
  label: z.string().optional(),
});

const RadarIndicatorSchema = z.object({
  name: z.string(),
  max: z.number(),
});

const HeatmapCellSchema = z.object({
  x: z.number(),
  y: z.number(),
  value: z.number(),
});

export interface ChartTreeNode {
  name: string;
  value?: number;
  children?: ChartTreeNode[];
}

const TreeNodeSchema: z.ZodType<ChartTreeNode> = z.lazy(() => z.object({
  name: z.string(),
  value: z.number().optional(),
  children: z.array(TreeNodeSchema).optional(),
}),
);

const NamedNodeSchema = z.object({
  name: z.string(),
});

const LinkSchema = z.object({
  source: z.string(),
  target: z.string(),
  value: z.number(),
});

const BoxSchema = z.union([
  z.object({
    min: z.number(),
    q1: z.number(),
    median: z.number(),
    q3: z.number(),
    max: z.number(),
  }),
  z.array(z.number()).length(5),
]);

const CandleSchema = z.union([
  z.object({
    open: z.number(),
    close: z.number(),
    low: z.number(),
    high: z.number(),
  }),
  z.array(z.number()).length(4),
]);

const RiverDatumSchema = z.union([
  z.object({
    time: z.string(),
    value: z.number(),
    name: z.string(),
  }),
  z.tuple([z.string(), z.number(), z.string()]),
]);

const ParallelRowSchema = z.union([
  z.array(z.number()),
  z.object({
    name: z.string().optional(),
    values: z.array(z.number()),
  }),
]);

const CalendarCellSchema = z.object({
  date: z.string(),
  value: z.number(),
});

const ChartTypeSchema = z.enum([
  'bar',
  'line',
  'pie',
  'doughnut',
  'scatter',
  'radar',
  'heatmap',
  'funnel',
  'gauge',
  'combo',
  'treemap',
  'sunburst',
  'tree',
  'sankey',
  'graph',
  'boxplot',
  'candlestick',
  'parallel',
  'themeRiver',
  'calendar',
]);

/**
 * A2UI catalog API for Chart — rendered with ECharts on the client.
 * Complex fields are literal-only (path binding kept for title / chartData).
 */
export const ChartApi = {
  name: 'Chart',
  schema: z
    .object({
      type: ChartTypeSchema,
      title: z.union([z.string(), PathBindingSchema]).optional(),
      chartData: z.union([z.array(ChartItemSchema), PathBindingSchema]).optional(),
      categories: z.array(z.string()).optional(),
      series: z.array(SeriesItemSchema).optional(),
      points: z.array(ScatterPointSchema).optional(),
      indicators: z.array(RadarIndicatorSchema).optional(),
      xLabels: z.array(z.string()).optional(),
      yLabels: z.array(z.string()).optional(),
      cells: z.array(HeatmapCellSchema).optional(),
      value: z.number().optional(),
      max: z.number().optional(),
      nodes: z.array(z.union([TreeNodeSchema, NamedNodeSchema])).optional(),
      links: z.array(LinkSchema).optional(),
      boxes: z.array(BoxSchema).optional(),
      candles: z.array(CandleSchema).optional(),
      dimensions: z.array(z.string()).optional(),
      rows: z.array(ParallelRowSchema).optional(),
      riverData: z.array(RiverDatumSchema).optional(),
      calendarCells: z.array(CalendarCellSchema).optional(),
      layout: z.enum(['force', 'circular']).optional(),
      range: z.union([z.string(), z.tuple([z.string(), z.string()])]).optional(),
      /** Alias some models use for candlestick OHLC rows. */
      ohlc: z.array(CandleSchema).optional(),
    })
    .strict()
    .superRefine((data, ctx) => {
      const hasChartData =
        data.chartData !== undefined
        && (Array.isArray(data.chartData) ? data.chartData.length > 0 : true);
      const hasSeries =
        Array.isArray(data.categories)
        && data.categories.length > 0
        && Array.isArray(data.series)
        && data.series.length > 0;
      const hasNodes = Array.isArray(data.nodes) && data.nodes.length > 0;
      const hasLinks = Array.isArray(data.links) && data.links.length > 0;
      const hasNamedSeries = Array.isArray(data.series) && data.series.length > 0;
      const hasBoxes = Array.isArray(data.boxes) && data.boxes.length > 0;
      const hasCandles =
        (Array.isArray(data.candles) && data.candles.length > 0)
        || (Array.isArray(data.ohlc) && data.ohlc.length > 0);
      const hasRows = Array.isArray(data.rows) && data.rows.length > 0;
      const hasDimensions = Array.isArray(data.dimensions) && data.dimensions.length > 0;
      const hasRiver = Array.isArray(data.riverData) && data.riverData.length > 0;

      switch (data.type) {
        case 'bar':
        case 'line':
          if (!hasChartData && !hasSeries) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'bar/line require chartData or categories+series',
            });
          }
          break;
        case 'pie':
        case 'doughnut':
        case 'funnel':
          if (!hasChartData) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: `${data.type} requires chartData`,
            });
          }
          break;
        case 'combo':
          if (!hasSeries || (data.series?.length ?? 0) < 2) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'combo requires categories and at least two series',
            });
          }
          break;
        case 'scatter':
          if (!Array.isArray(data.points) || data.points.length === 0) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'scatter requires points',
            });
          }
          break;
        case 'radar':
          if (
            !Array.isArray(data.indicators)
            || data.indicators.length === 0
            || !Array.isArray(data.series)
            || data.series.length === 0
          ) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'radar requires indicators and series',
            });
          }
          break;
        case 'heatmap':
          if (
            !Array.isArray(data.xLabels)
            || !Array.isArray(data.yLabels)
            || !Array.isArray(data.cells)
            || data.cells.length === 0
          ) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'heatmap requires xLabels, yLabels, and cells',
            });
          }
          break;
        case 'gauge':
          if (typeof data.value !== 'number' && !hasChartData) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'gauge requires value or chartData',
            });
          }
          break;
        case 'treemap':
        case 'sunburst':
        case 'tree':
          if (!hasNodes && !(data.type !== 'tree' && hasChartData)) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: `${data.type} requires nodes`
                + (data.type === 'tree' ? '' : ' or chartData'),
            });
          }
          break;
        case 'sankey':
        case 'graph':
          if (!hasNodes || !hasLinks) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: `${data.type} requires nodes and links`,
            });
          }
          break;
        case 'boxplot': {
          const seriesOk = hasNamedSeries
            && data.series!.every(s => s.values.length >= 5);
          if (!hasBoxes && !seriesOk) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'boxplot requires boxes or series with five-number values',
            });
          }
          break;
        }
        case 'candlestick':
          if (
            !hasCandles
            && !(hasNamedSeries && data.series!.some(s => s.values.length >= 4))
          ) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'candlestick requires candles/ohlc or series OHLC values',
            });
          }
          break;
        case 'parallel':
          if (!hasNamedSeries && !(hasDimensions && hasRows) && !hasChartData) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'parallel requires dimensions+rows, series, or chartData vectors',
            });
          }
          break;
        case 'themeRiver':
          if (!hasRiver && !hasSeries) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'themeRiver requires riverData or categories+series',
            });
          }
          break;
        case 'calendar':
          if (
            data.range === undefined
            || !Array.isArray(data.calendarCells)
            || data.calendarCells.length === 0
          ) {
            ctx.addIssue({
              code: z.ZodIssueCode.custom,
              message: 'calendar requires range and calendarCells',
            });
          }
          break;
        default:
          break;
      }
    }),
} as const satisfies ComponentApi;

export interface ChartItem {
  label: string;
  value: number;
}
export type ChartSeriesItem = z.infer<typeof SeriesItemSchema>;
export type ChartScatterPoint = z.infer<typeof ScatterPointSchema>;
export type ChartRadarIndicator = z.infer<typeof RadarIndicatorSchema>;
export type ChartHeatmapCell = z.infer<typeof HeatmapCellSchema>;
export type ChartNamedNode = z.infer<typeof NamedNodeSchema>;
export type ChartLink = z.infer<typeof LinkSchema>;
export interface ChartBox {
  min: number;
  q1: number;
  median: number;
  q3: number;
  max: number;
}
export interface ChartCandle {
  open: number;
  close: number;
  low: number;
  high: number;
}
export interface ChartRiverDatum {
  time: string;
  value: number;
  name: string;
}
export type ChartCalendarCell = z.infer<typeof CalendarCellSchema>;
export type ChartType = z.infer<typeof ChartTypeSchema>;

export const CHART_TYPES: readonly ChartType[] = ChartTypeSchema.options;
