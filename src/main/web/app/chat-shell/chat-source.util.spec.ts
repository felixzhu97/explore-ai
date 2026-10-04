import { describe, expect, it } from 'vitest';
import {
  sourceFaviconUrl,
  sourceHostname,
  sourceLabel,
  sourcePublishedAt,
  sourceTitle,
} from './chat-source.util';

describe('chat-source.util', () => {
  it('should strip www from the hostname', () => {
    expect(sourceHostname({ text: '', score: 0, url: 'https://www.Example.com/a' }))
      .toBe('example.com');
  });

  it('should return no favicon when the url is invalid', () => {
    expect(sourceFaviconUrl({ text: '', score: 0, url: 'not a url' })).toBeNull();
  });

  it('should fall back to the given label when the source is empty', () => {
    expect(sourceLabel({ text: '', score: 0 }, 'Sources')).toBe('Sources');
    expect(sourceTitle({ text: '', score: 0 }, 'Sources')).toBe('Sources');
  });

  it('should read the published date from metadata when not set directly', () => {
    expect(sourcePublishedAt({ text: '', score: 0, metadata: { date: ' 2026-01-02 ' } }))
      .toBe('2026-01-02');
  });
});
