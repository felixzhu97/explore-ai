import { z } from 'zod';
import type { ComponentApi } from '@a2ui/web_core/v0_9';

const ChartItemSchema = z.object({
  label: z.string(),
  value: z.number(),
});

const PathBindingSchema = z.object({ path: z.string() });

/**
 * A2UI catalog API for Chart — rendered with ECharts on the client.
 */
export const ChartApi = {
  name: 'Chart',
  schema: z
    .object({
      type: z.enum(['bar', 'line', 'pie', 'doughnut']),
      title: z.union([z.string(), PathBindingSchema]).optional(),
      chartData: z.union([z.array(ChartItemSchema), PathBindingSchema]),
    })
    .strict(),
} as const satisfies ComponentApi;

export type ChartItem = z.infer<typeof ChartItemSchema>;
export type ChartType = z.infer<typeof ChartApi.schema>['type'];
