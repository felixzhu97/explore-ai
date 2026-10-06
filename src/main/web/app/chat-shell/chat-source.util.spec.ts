import { describe, expect, it } from 'vitest';
import {
  getSourceFaviconUrl,
  getSourceHostname,
  getSourceLabel,
  getSourcePublishedAt,
  getSourceTitle,
} from './chat-source.util';

describe('chat-source.util', () => {
  it('should strip www from the hostname', () => {
    expect(getSourceHostname({ text: '', score: 0, url: 'https://www.Example.com/a' }))
      .toBe('example.com');
  });

  it('should return no favicon when the url is invalid', () => {
    expect(getSourceFaviconUrl({ text: '', score: 0, url: 'not a url' })).toBeNull();
  });

  it('should fall back to the given label when the source is empty', () => {
    expect(getSourceLabel({ text: '', score: 0 }, 'Sources')).toBe('Sources');
    expect(getSourceTitle({ text: '', score: 0 }, 'Sources')).toBe('Sources');
  });

  it('should read the published date from metadata when not set directly', () => {
    expect(getSourcePublishedAt({ text: '', score: 0, metadata: { date: ' 2026-01-02 ' } }))
      .toBe('2026-01-02');
  });
});
