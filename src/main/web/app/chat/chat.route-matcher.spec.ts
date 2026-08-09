import { describe, expect, it } from 'vitest';
import { UrlSegment } from '@angular/router';
import { chatRouteMatcher } from './chat.route-matcher';

describe('chatRouteMatcher', () => {
  it('should match bare chat path', () => {
    const result = chatRouteMatcher([new UrlSegment('chat', {})], {} as never, {} as never);

    expect(result?.consumed).toEqual([expect.objectContaining({ path: 'chat' })]);
    expect(result?.posParams).toBeUndefined();
  });

  it('should match chat with session id', () => {
    const session = new UrlSegment('abc-123', {});
    const result = chatRouteMatcher(
      [new UrlSegment('chat', {}), session],
      {} as never,
      {} as never,
    );

    expect(result?.posParams?.['sessionId']).toBe(session);
    expect(result?.consumed).toHaveLength(2);
  });

  it('should not match unrelated paths', () => {
    expect(chatRouteMatcher([new UrlSegment('rag', {})], {} as never, {} as never)).toBeNull();
    expect(
      chatRouteMatcher(
        [new UrlSegment('chat', {}), new UrlSegment('a', {}), new UrlSegment('b', {})],
        {} as never,
        {} as never,
      ),
    ).toBeNull();
  });
});
