import { z } from 'zod';
import type { ComponentApi } from '@a2ui/web_core/v0_9';

const ChartItemSchema = z.object({
  label: z.string(),
  value: z.number(),
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
        default:
          break;
      }
    }),
} as const satisfies ComponentApi;

export type ChartItem = z.infer<typeof ChartItemSchema>;
export type ChartSeriesItem = z.infer<typeof SeriesItemSchema>;
export type ChartScatterPoint = z.infer<typeof ScatterPointSchema>;
export type ChartRadarIndicator = z.infer<typeof RadarIndicatorSchema>;
export type ChartHeatmapCell = z.infer<typeof HeatmapCellSchema>;
export type ChartType = z.infer<typeof ChartTypeSchema>;

export const CHART_TYPES: readonly ChartType[] = ChartTypeSchema.options;
