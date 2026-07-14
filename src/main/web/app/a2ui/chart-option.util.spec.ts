import { describe, expect, it } from 'vitest';
import { buildEchartsOption } from './chart-option.util';

describe('buildEchartsOption', () => {
  const data = [
    { label: 'A', value: 1 },
    { label: 'B', value: 2 },
  ];

  it('should_map_bar_chart_to_category_series', () => {
    const option = buildEchartsOption('bar', data, 'Title');

    expect(option).toMatchObject({
      title: { text: 'Title', left: 'center' },
      xAxis: { type: 'category', data: ['A', 'B'] },
      series: [{ type: 'bar', data: [1, 2] }],
    });
  });

  it('should_map_line_chart_with_smooth', () => {
    const option = buildEchartsOption('line', data);

    expect(option).toMatchObject({
      series: [{ type: 'line', data: [1, 2], smooth: true }],
    });
  });

  it('should_map_pie_and_doughnut_to_pie_series', () => {
    const pie = buildEchartsOption('pie', data, 'Share');
    const doughnut = buildEchartsOption('doughnut', data);

    expect(pie).toMatchObject({
      series: [
        {
          type: 'pie',
          radius: '65%',
          data: [
            { name: 'A', value: 1 },
            { name: 'B', value: 2 },
          ],
        },
      ],
    });
    expect(doughnut).toMatchObject({
      series: [{ type: 'pie', radius: ['40%', '70%'] }],
    });
  });
});
