import { buildSharedChartOption } from './chart-option.util';

interface SeriesItem {
  type?: string;
  data?: unknown;
  radius?: string | string[];
}

function firstSeries(
  option: ReturnType<typeof buildSharedChartOption>,
): SeriesItem | undefined {
  const series = option['series'];
  if (!Array.isArray(series) || series.length === 0) {
    return undefined;
  }
  return series[0] as SeriesItem;
}

describe('buildSharedChartOption', () => {
  it('should_build_line_option_when_type_is_line', () => {
    const option = buildSharedChartOption('line', [{ label: '2026-07-01', value: 3 }], 'Requests');
    expect(firstSeries(option)).toMatchObject({ type: 'line', data: [3] });
  });

  it('should_build_doughnut_option_when_type_is_doughnut', () => {
    const option = buildSharedChartOption('doughnut', [{ label: 'READY', value: 2 }]);
    expect(firstSeries(option)).toMatchObject({ type: 'pie', radius: ['42%', '68%'] });
  });
});
