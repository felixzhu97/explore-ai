import { describe, expect, it } from 'vitest';
import {
  buildChartOption,
  buildEchartsOption,
  toChartItems,
  toChartSeries,
  toHeatmapCells,
  toScatterPoints,
} from './chart-option.util';

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
    expect((option!['series'] as unknown[])[0]).not.toHaveProperty('smooth');
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
          radius: '62%',
          data: [
            { name: 'A', value: 1 },
            { name: 'B', value: 2 },
          ],
        },
      ],
    });
    expect(doughnut).toMatchObject({
      series: [{ type: 'pie', radius: ['42%', '68%'] }],
    });
  });

  it('should return null when data empty', () => {
    expect(buildEchartsOption('bar', [])).toBeNull();
  });
});

describe('buildChartOption specialized types', () => {
  it('should map funnel from chartData', () => {
    const option = buildChartOption({
      type: 'funnel',
      chartData: [
        { label: 'Visit', value: 100 },
        { label: 'Buy', value: 20 },
      ],
    });
    expect(option?.['series']).toMatchObject([
      {
        type: 'funnel',
        sort: 'descending',
        gap: 6,
        minSize: '18%',
        label: { show: true, position: 'inside' },
        data: [
          { name: 'Visit', value: 100 },
          { name: 'Buy', value: 20 },
        ],
      },
    ]);
  });

  it('should map scatter from points', () => {
    const option = buildChartOption({
      type: 'scatter',
      points: [
        { x: 1, y: 2, label: 'p1' },
        { x: 3, y: 4 },
      ],
    });
    expect(option).toMatchObject({
      series: [{ type: 'scatter' }],
    });
    expect((option!['series'] as { data: unknown[] }[])[0].data).toHaveLength(2);
  });

  it('should map radar with indicators and series', () => {
    const option = buildChartOption({
      type: 'radar',
      title: 'Scores',
      indicators: [
        { name: 'Speed', max: 100 },
        { name: 'Quality', max: 100 },
      ],
      series: [{ name: 'A', values: [80, 70] }],
    });
    expect(option?.['radar']).toMatchObject({
      center: ['50%', '54%'],
      radius: '48%',
      indicator: [
        { name: 'Speed', max: 100 },
        { name: 'Quality', max: 100 },
      ],
    });
    expect(option?.['series']).toMatchObject([
      { type: 'radar', data: [{ name: 'A', value: [80, 70] }] },
    ]);
    expect(option?.['legend']).toMatchObject({ bottom: 4, left: 'center' });
  });

  it('should map heatmap with cells', () => {
    const option = buildChartOption({
      type: 'heatmap',
      xLabels: ['Mon', 'Tue'],
      yLabels: ['AM', 'PM'],
      cells: [
        { x: 0, y: 0, value: 1 },
        { x: 1, y: 1, value: 5 },
      ],
    });
    expect(option).toMatchObject({
      series: [{ type: 'heatmap', data: [[0, 0, 1], [1, 1, 5]], label: { show: false } }],
      visualMap: { min: 1, max: 5, calculable: false },
    });
  });

  it('should map gauge from value', () => {
    const option = buildChartOption({ type: 'gauge', value: 72, max: 100, title: 'KPI' });
    expect(option).toMatchObject({
      series: [
        {
          type: 'gauge',
          max: 100,
          progress: { show: true },
          data: [{ value: 72 }],
        },
      ],
    });
  });

  it('should map multi-series bar with legend', () => {
    const option = buildChartOption({
      type: 'bar',
      categories: ['Q1', 'Q2'],
      series: [
        { name: 'A', values: [1, 2] },
        { name: 'B', values: [3, 4] },
      ],
    });
    expect(option).toMatchObject({
      legend: {},
      xAxis: { data: ['Q1', 'Q2'] },
      series: [
        { name: 'A', type: 'bar', data: [1, 2] },
        { name: 'B', type: 'bar', data: [3, 4] },
      ],
    });
  });

  it('should map combo with bar and line series', () => {
    const option = buildChartOption({
      type: 'combo',
      categories: ['Jan', 'Feb'],
      series: [
        { name: 'Volume', values: [10, 20], kind: 'bar' },
        { name: 'Trend', values: [1, 2], kind: 'line' },
      ],
    });
    expect(option).toMatchObject({
      legend: {},
      yAxis: [{ type: 'value' }, { type: 'value', splitLine: { show: false } }],
      series: [
        { name: 'Volume', type: 'bar', data: [10, 20], yAxisIndex: 0 },
        { name: 'Trend', type: 'line', data: [1, 2], smooth: true, yAxisIndex: 1 },
      ],
    });
  });

  it('should return null for invalid combo', () => {
    expect(
      buildChartOption({
        type: 'combo',
        categories: ['A'],
        series: [{ name: 'Only', values: [1], kind: 'bar' }],
      }),
    ).toBeNull();
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

describe('coercers', () => {
  it('should coerce series with kinds', () => {
    expect(
      toChartSeries([
        { name: 'A', values: [1, '2'], kind: 'line' },
        { name: 'bad', values: ['x'] },
      ]),
    ).toEqual([{ name: 'A', values: [1, 2], kind: 'line' }]);
  });

  it('should coerce scatter points and heatmap cells', () => {
    expect(toScatterPoints([{ x: '1', y: 2, label: 'p' }])).toEqual([
      { x: 1, y: 2, label: 'p' },
    ]);
    expect(toHeatmapCells([{ x: 0, y: 1, value: '3' }])).toEqual([
      { x: 0, y: 1, value: 3 },
    ]);
  });
});
