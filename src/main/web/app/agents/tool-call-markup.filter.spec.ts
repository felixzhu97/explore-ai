import { describe, expect, it } from 'vitest';
import {
  parseDsmlToolInvocations,
  stripToolCallMarkup,
  toMinimalToolSteps,
} from './tool-call-markup.filter';

const FULLWIDTH = [
  '前言',
  '<｜DSML｜tool_calls>',
  '<｜DSML｜invoke name="searchWeb">',
  '<｜DSML｜parameter name="query" string="true">Anthropic Claude enterprise pricing 2025 API business',
  '</｜DSML｜parameter>',
  '</｜DSML｜invoke>',
  '<｜DSML｜invoke name="searchWeb">',
  '<｜DSML｜parameter name="query" string="true">Vercel AI SDK pricing',
  '</｜DSML｜parameter>',
  '</｜DSML｜invoke>',
  '</｜DSML｜tool_calls>',
  '后记',
].join('\n');

describe('stripToolCallMarkup', () => {
  it('should_strip_fullwidth_and_ascii_dsml_blocks', () => {
    expect(stripToolCallMarkup(FULLWIDTH)).toBe('前言\n\n后记');
    expect(
      stripToolCallMarkup('<|DSML|tool_calls>x</|DSML|tool_calls> keep'),
    ).toBe('keep');
  });

  it('should_strip_spaced_dsml_like_tags', () => {
    const spaced =
      'hi < | DSML | tool_calls>q</ | DSML | tool_calls> bye';
    const cleaned = stripToolCallMarkup(spaced);
    expect(cleaned).not.toMatch(/DSML/i);
    expect(cleaned).toContain('hi');
    expect(cleaned).toContain('bye');
  });

  it('should_strip_unclosed_dsml_stream_fragment', () => {
    const raw = [
      '第一段答案。',
      '<|DSML|tool_calls>',
      '<|DSML|invoke name="searchWeb">',
      '<|DSML|parameter name="query" string="true">private lookup',
    ].join('\n');

    expect(stripToolCallMarkup(raw)).toBe('第一段答案。');
  });

  it('should_preserve_non_dsml_angle_tags_when_sanitizing', () => {
    const raw = 'Keep <code>literal</code> and <custom data="x">metadata</custom>.';

    expect(stripToolCallMarkup(raw)).toBe(raw);
  });
});

describe('parseDsmlToolInvocations', () => {
  it('should_parse_and_dedupe_search_queries', () => {
    const items = parseDsmlToolInvocations(FULLWIDTH);
    expect(items).toEqual([
      {
        toolName: 'searchWeb',
        query: 'Anthropic Claude enterprise pricing 2025 API business',
      },
      { toolName: 'searchWeb', query: 'Vercel AI SDK pricing' },
    ]);
  });

  it('should_parse_body_text_when_query_parameter_is_absent', () => {
    const raw = [
      '<|DSML|invoke name="lookup">',
      '<span>latest revenue filings</span>',
      '</|DSML|invoke>',
    ].join('\n');

    expect(parseDsmlToolInvocations(raw)).toEqual([
      { toolName: 'lookup', query: 'latest revenue filings' },
    ]);
  });
});

describe('toMinimalToolSteps', () => {
  it('should_build_minimal_chinese_search_labels', () => {
    const steps = toMinimalToolSteps(
      parseDsmlToolInvocations(FULLWIDTH),
      'success',
    );
    expect(steps[0].label).toMatch(/^搜索 · Anthropic/);
    expect(steps[0].label.length).toBeLessThanOrEqual(3 + 3 + 42 + 1);
    expect(steps[0].status).toBe('success');
  });
});
