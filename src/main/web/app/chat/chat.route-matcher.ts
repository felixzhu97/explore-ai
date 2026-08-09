import { UrlMatchResult, UrlMatcher, UrlSegment } from '@angular/router';

/**
 * Single route config for `/chat` and `/chat/:sessionId` so Angular reuses
 * `ChatPage` when promoting a bare draft URL after the first message.
 * Separate `path` entries remount the page and abort the in-flight SSE.
 */
export const chatRouteMatcher: UrlMatcher = (
  segments: UrlSegment[],
): UrlMatchResult | null => {
  if (segments.length === 0 || segments[0].path !== 'chat') {
    return null;
  }
  if (segments.length === 1) {
    return { consumed: segments };
  }
  if (segments.length === 2) {
    return {
      consumed: segments,
      posParams: { sessionId: segments[1] },
    };
  }
  return null;
};
