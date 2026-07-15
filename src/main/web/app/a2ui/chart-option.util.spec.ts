import { describe, expect, it } from 'vitest';
import { buildEchartsOption, toChartItems } from './chart-option.util';

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
    expect((option['series'] as unknown[])[0]).not.toHaveProperty('smooth');
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

describe('toChartItems', () => {
  it('should_accept_numeric_string_values', () => {
    expect(
      toChartItems([
        { label: 'A', value: '10' },
        { label: 'B', value: 20 },
      ]),
    ).toEqual([
      { label: 'A', value: 10 },
      { label: 'B', value: 20 },
    ]);
  });

  it('should_drop_non_finite_or_invalid_rows', () => {
    expect(
      toChartItems([
        { label: 'ok', value: 1 },
        { label: 'bad', value: 'x' },
        { label: 1, value: 2 },
        null,
      ]),
    ).toEqual([{ label: 'ok', value: 1 }]);
  });

  it('should_return_empty_when_not_array', () => {
    expect(toChartItems(undefined)).toEqual([]);
    expect(toChartItems({ label: 'A', value: 1 })).toEqual([]);
  });
});
