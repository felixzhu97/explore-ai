import type { EChartsCoreOption } from 'echarts/core';

export type SharedChartType = 'bar' | 'line' | 'pie' | 'doughnut';

export interface SharedChartItem {
  label: string;
  value: number;
}

export function buildSharedChartOption(
  type: SharedChartType,
  data: SharedChartItem[],
  title?: string,
): EChartsCoreOption {
  const labels = data.map(item => item.label);
  const values = data.map(item => item.value);

  if (type === 'pie' || type === 'doughnut') {
    return {
      title: title ? { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 500 } } : undefined,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: type === 'doughnut' ? ['42%', '68%'] : '65%',
          data: data.map(item => ({ name: item.label, value: item.value })),
        },
      ],
    };
  }

  return {
    title: title ? { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 500 } } : undefined,
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: title ? 48 : 24, bottom: 36 },
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
