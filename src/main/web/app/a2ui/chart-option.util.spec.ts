import { describe, expect, it } from 'vitest';
import type { ChartBox, ChartCandle } from './chart.api';
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

  it('should map treemap from hierarchical nodes', () => {
    const option = buildChartOption({
      type: 'treemap',
      nodes: [{ name: 'EV', value: 18, children: [{ name: 'BYD', value: 10 }] }],
    });
    expect(option?.['series']).toMatchObject([{ type: 'treemap' }]);
  });

  it('should map treemap from chartData when nodes missing', () => {
    expect(
      buildChartOption({
        type: 'treemap',
        chartData: [
          { label: '研发', value: 120 },
          { label: '产品', value: 80 },
        ],
      })?.['series'],
    ).toMatchObject([
      {
        type: 'treemap',
        data: [
          { name: '研发', value: 120 },
          { name: '产品', value: 80 },
        ],
      },
    ]);
  });

  it('should map sunburst from nodes', () => {
    expect(
      buildChartOption({
        type: 'sunburst',
        nodes: [{ name: 'Root', children: [{ name: 'A', value: 1 }] }],
      })?.['series'],
    ).toMatchObject([{ type: 'sunburst' }]);
  });

  it('should map tree from nodes', () => {
    expect(
      buildChartOption({
        type: 'tree',
        nodes: [{ name: 'Org', children: [{ name: 'Eng' }] }],
      })?.['series'],
    ).toMatchObject([{ type: 'tree', orient: 'LR' }]);
  });

  it('should map sankey from nodes and links', () => {
    const option = buildChartOption({
      type: 'sankey',
      nodes: [{ name: 'A' }, { name: 'B' }],
      links: [{ source: 'A', target: 'B', value: 5 }],
    });
    expect(option?.['series']).toMatchObject([
      {
        type: 'sankey',
        data: [{ name: 'A' }, { name: 'B' }],
        links: [{ source: 'A', target: 'B', value: 5 }],
      },
    ]);
  });

  it('should map graph with force layout by default', () => {
    expect(
      buildChartOption({
        type: 'graph',
        nodes: [{ name: 'A' }, { name: 'B' }],
        links: [{ source: 'A', target: 'B', value: 2 }],
      })?.['series'],
    ).toMatchObject([{ type: 'graph', layout: 'force' }]);
  });

  it('should map boxplot from categories and boxes', () => {
    const option = buildChartOption({
      type: 'boxplot',
      categories: ['Q1'],
      boxes: [{ min: 1, q1: 2, median: 3, q3: 4, max: 5 }],
    });
    expect(option?.['series']).toMatchObject([
      { type: 'boxplot', data: [[1, 2, 3, 4, 5]] },
    ]);
  });

  it('should map boxplot from series five-number values', () => {
    expect(
      buildChartOption({
        type: 'boxplot',
        series: [
          { name: 'Q1', values: [10, 15, 20, 28, 35] },
          { name: 'Q2', values: [12, 18, 22, 30, 40] },
        ],
      })?.['series'],
    ).toMatchObject([{ type: 'boxplot', data: [[10, 15, 20, 28, 35], [12, 18, 22, 30, 40]] }]);
  });

  it('should map boxplot from tuple boxes', () => {
    const tupleBoxes = [[1, 2, 3, 4, 5]] as unknown as ChartBox[];
    expect(
      buildChartOption({
        type: 'boxplot',
        categories: ['Q1'],
        boxes: tupleBoxes,
      })?.['series'],
    ).toMatchObject([{ type: 'boxplot', data: [[1, 2, 3, 4, 5]] }]);
  });

  it('should map candlestick from categories and candles', () => {
    const option = buildChartOption({
      type: 'candlestick',
      categories: ['Mon'],
      candles: [{ open: 10, close: 12, low: 9, high: 13 }],
    });
    expect(option?.['series']).toMatchObject([
      { type: 'candlestick', data: [[10, 12, 9, 13]] },
    ]);
  });

  it('should map candlestick from ohlc tuples', () => {
    const ohlc = [
      [100, 103, 99, 104],
      [103, 106, 102, 107],
    ] as unknown as ChartCandle[];
    expect(
      buildChartOption({
        type: 'candlestick',
        categories: ['06-01', '06-02'],
        ohlc,
      })?.['series'],
    ).toMatchObject([
      {
        type: 'candlestick',
        data: [
          [100, 103, 99, 104],
          [103, 106, 102, 107],
        ],
      },
    ]);
  });

  it('should map parallel from dimensions and rows', () => {
    const option = buildChartOption({
      type: 'parallel',
      dimensions: ['Speed', 'Power'],
      rows: [
        [1, 2],
        [3, 4],
      ],
    });
    expect(option).toMatchObject({
      parallelAxis: [
        { dim: 0, name: 'Speed' },
        { dim: 1, name: 'Power' },
      ],
      series: [{ type: 'parallel', data: [[1, 2], [3, 4]] }],
    });
  });

  it('should map parallel from chartData label/values rows', () => {
    expect(
      buildChartOption({
        type: 'parallel',
        dimensions: ['续航', '价格', '加速'],
        chartDataRaw: [
          { label: 'A', values: [600, 30, 5] },
          { label: 'B', values: [500, 22, 6.5] },
        ],
      })?.['series'],
    ).toMatchObject([
      { type: 'parallel', data: [[600, 30, 5], [500, 22, 6.5]] },
    ]);
  });

  it('should map boxplot from chartData label/values rows', () => {
    expect(
      buildChartOption({
        type: 'boxplot',
        chartDataRaw: [
          { label: 'Q1', values: [10, 15, 20, 28, 35] },
          { label: 'Q2', values: [12, 18, 22, 30, 40] },
        ],
      })?.['series'],
    ).toMatchObject([
      { type: 'boxplot', data: [[10, 15, 20, 28, 35], [12, 18, 22, 30, 40]] },
    ]);
  });

  it('should map candlestick from chartData OHLC objects', () => {
    expect(
      buildChartOption({
        type: 'candlestick',
        chartDataRaw: [
          { label: '06-01', open: 100, close: 103, low: 99, high: 104 },
          { label: '06-02', open: 103, close: 106, low: 102, high: 107 },
        ],
      })?.['series'],
    ).toMatchObject([
      {
        type: 'candlestick',
        data: [
          [100, 103, 99, 104],
          [103, 106, 102, 107],
        ],
      },
    ]);
  });

  it('should map themeRiver from riverData', () => {
    expect(
      buildChartOption({
        type: 'themeRiver',
        riverData: [{ time: '2024-01', value: 10, name: 'A' }],
      })?.['series'],
    ).toMatchObject([{ type: 'themeRiver', data: [['2024-01-01', 10, 'A']] }]);
  });

  it('should map Chinese month labels onto a time singleAxis', () => {
    const option = buildChartOption({
      type: 'themeRiver',
      categories: ['1月', '2月'],
      series: [
        { name: '话题 X', values: [10, 12] },
        { name: '话题 Y', values: [8, 9] },
      ],
    });
    expect(option?.['singleAxis']).toMatchObject({ type: 'time' });
    expect(option?.['legend']).toMatchObject({ data: ['话题 X', '话题 Y'] });
    expect(option?.['series']).toMatchObject([
      {
        type: 'themeRiver',
        label: { show: false },
        data: [
          ['2024-01-01', 10, '话题 X'],
          ['2024-02-01', 12, '话题 X'],
          ['2024-01-01', 8, '话题 Y'],
          ['2024-02-01', 9, '话题 Y'],
        ],
      },
    ]);
  });

  it('should map themeRiver from categories and series', () => {
    expect(
      buildChartOption({
        type: 'themeRiver',
        categories: ['2024-01', '2024-02'],
        series: [
          { name: '科技', values: [10, 12] },
          { name: '娱乐', values: [8, 9] },
        ],
      })?.['series'],
    ).toMatchObject([
      {
        type: 'themeRiver',
        data: [
          ['2024-01-01', 10, '科技'],
          ['2024-02-01', 12, '科技'],
          ['2024-01-01', 8, '娱乐'],
          ['2024-02-01', 9, '娱乐'],
        ],
      },
    ]);
  });

  it('should map calendar heatmap from range and cells', () => {
    const option = buildChartOption({
      type: 'calendar',
      range: '2024',
      calendarCells: [{ date: '2024-01-01', value: 3 }],
    });
    expect(option).toMatchObject({
      calendar: { range: '2024' },
      series: [{ type: 'heatmap', coordinateSystem: 'calendar' }],
    });
  });

  it('should return null for empty advanced chart payloads', () => {
    expect(buildChartOption({ type: 'treemap', nodes: [] })).toBeNull();
    expect(buildChartOption({ type: 'sankey', nodes: [{ name: 'A' }], links: [] })).toBeNull();
    expect(buildChartOption({ type: 'boxplot', categories: ['A'], boxes: [] })).toBeNull();
    expect(buildChartOption({ type: 'parallel', dimensions: [], rows: [] })).toBeNull();
    expect(buildChartOption({ type: 'calendar', range: '2024', calendarCells: [] })).toBeNull();
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
