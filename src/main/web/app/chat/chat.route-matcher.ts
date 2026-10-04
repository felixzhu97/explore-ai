import { type UrlMatchResult, type UrlMatcher, type UrlSegment } from '@angular/router';

/**
 * Single route config for `/chat` and `/chat/:sessionId` so Angular reuses
 * `ChatPageComponent` when promoting a bare draft URL after the first message.
 * Separate `path` entries remount the page and abort the in-flight SSE.
 */
export const chatRouteMatcher: UrlMatcher = (
  segments: UrlSegment[],
): UrlMatchResult | null => {
  const [first, sessionId, ...rest] = segments;
  if (first?.path !== 'chat' || rest.length > 0) {
    return null;
  }
  return sessionId
    ? { consumed: segments, posParams: { sessionId } }
    : { consumed: segments };
};
