import type { EChartsCoreOption } from 'echarts/core';
import type { ChartItem, ChartType } from './chart.api';

/** Map A2UI Chart props to an ECharts option (no executable code). */
export function buildEchartsOption(
  type: ChartType,
  data: ChartItem[],
  title?: string,
): EChartsCoreOption {
  const labels = data.map(item => item.label);
  const values = data.map(item => item.value);

  if (type === 'pie' || type === 'doughnut') {
    return {
      title: title ? { text: title, left: 'center' } : undefined,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: type === 'doughnut' ? ['40%', '70%'] : '65%',
          data: data.map(item => ({ name: item.label, value: item.value })),
        },
      ],
    };
  }

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: title ? 48 : 24, bottom: 32 },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [
      {
        type,
        data: values,
        ...(type === 'line' ? { smooth: true } : {}),
      },
    ],
  };
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
