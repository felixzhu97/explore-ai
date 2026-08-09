import { describe, expect, it } from 'vitest';
import { buildEchartsOption, toChartItems } from './chart-option.util';

describe('buildEchartsOption', () => {
  const data = [
    { label: 'A', value: 1 },
    { label: 'B', value: 2 },
  ];

  it('should map bar chart to category series', () => {
    const option = buildEchartsOption('bar', data, 'Title');

    expect(option).toMatchObject({
      title: { text: 'Title', left: 'center' },
      xAxis: { type: 'category', data: ['A', 'B'] },
      series: [{ type: 'bar', data: [1, 2] }],
    });
    expect((option['series'] as unknown[])[0]).not.toHaveProperty('smooth');
  });

  it('should map line chart with smooth', () => {
    const option = buildEchartsOption('line', data);

    expect(option).toMatchObject({
      series: [{ type: 'line', data: [1, 2], smooth: true }],
    });
  });

  it('should map pie and doughnut to pie series', () => {
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
  it('should accept numeric string values', () => {
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

  it('should drop non-finite or invalid rows', () => {
    expect(
      toChartItems([
        { label: 'ok', value: 1 },
        { label: 'bad', value: 'x' },
        { label: 1, value: 2 },
        null,
      ]),
    ).toEqual([{ label: 'ok', value: 1 }]);
  });

  it('should return empty when not array', () => {
    expect(toChartItems(undefined)).toEqual([]);
    expect(toChartItems({ label: 'A', value: 1 })).toEqual([]);
  });
});
