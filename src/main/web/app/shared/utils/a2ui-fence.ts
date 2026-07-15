import type { A2uiMessage } from '@a2ui/web_core/v0_9';

export type ContentSegment =
  | { type: 'markdown'; content: string }
  | { type: 'a2ui'; messages: A2uiMessage[]; surfaceId: string; raw: string }
  | { type: 'a2ui-pending' };

/** Closing ``` alone on its line (no language tag) so later fences are not stolen. */
const A2UI_FENCE_RE = /```a2ui\s*\n([\s\S]*?)\n```[ \t]*(?:\n|$)/gi;

/** Stable short id from fence body (djb2). */
export function stableSurfaceId(raw: string): string {
  let hash = 5381;
  for (let i = 0; i < raw.length; i++) {
    hash = ((hash << 5) + hash) ^ raw.charCodeAt(i);
  }
  return `a2ui-${(hash >>> 0).toString(16)}`;
}

export function parseA2uiNdjson(raw: string): A2uiMessage[] {
  const messages: A2uiMessage[] = [];
  for (const line of raw.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('//')) {
      continue;
    }
    try {
      const parsed = JSON.parse(trimmed) as A2uiMessage;
      if (parsed && typeof parsed === 'object' && 'version' in parsed) {
        messages.push(normalizeMessageVersion(parsed));
      }
    } catch {
      // skip invalid lines
    }
  }
  return messages;
}

/** Schema in @a2ui/web_core expects literal "v0.9". */
function normalizeMessageVersion(message: A2uiMessage): A2uiMessage {
  const version = (message as { version?: string }).version;
  if (version === 'v0.9.1' || version === 'v0.9') {
    return { ...message, version: 'v0.9' } as A2uiMessage;
  }
  return message;
}

export function remapSurfaceIds(
  messages: A2uiMessage[],
  surfaceId: string,
): A2uiMessage[] {
  return messages.map((message) => {
    if ('createSurface' in message && message.createSurface) {
      return {
        ...message,
        version: 'v0.9' as const,
        createSurface: { ...message.createSurface, surfaceId },
      };
    }
    if ('updateComponents' in message && message.updateComponents) {
      return {
        ...message,
        version: 'v0.9' as const,
        updateComponents: { ...message.updateComponents, surfaceId },
      };
    }
    if ('updateDataModel' in message && message.updateDataModel) {
      return {
        ...message,
        version: 'v0.9' as const,
        updateDataModel: { ...message.updateDataModel, surfaceId },
      };
    }
    if ('deleteSurface' in message && message.deleteSurface) {
      return {
        ...message,
        version: 'v0.9' as const,
        deleteSurface: { ...message.deleteSurface, surfaceId },
      };
    }
    return message;
  });
}

/**
 * Split assistant markdown into markdown / completed a2ui fences / pending fence.
 */
export function splitMarkdownAndA2ui(content: string): ContentSegment[] {
  if (!content) {
    return [];
  }

  const segments: ContentSegment[] = [];
  let lastIndex = 0;
  A2UI_FENCE_RE.lastIndex = 0;

  let match: RegExpExecArray | null;
  while ((match = A2UI_FENCE_RE.exec(content)) !== null) {
    const before = content.slice(lastIndex, match.index);
    if (before.trim()) {
      segments.push({ type: 'markdown', content: before });
    }
    const raw = match[1] ?? '';
    const surfaceId = stableSurfaceId(raw);
    const messages = remapSurfaceIds(parseA2uiNdjson(raw), surfaceId);
    segments.push({ type: 'a2ui', messages, surfaceId, raw });
    lastIndex = match.index + match[0].length;
  }

  const rest = content.slice(lastIndex);
  if (hasUnclosedA2uiFence(rest)) {
    const openIndex = rest.search(/```a2ui\b/i);
    const beforeOpen = openIndex >= 0 ? rest.slice(0, openIndex) : '';
    if (beforeOpen.trim()) {
      segments.push({ type: 'markdown', content: beforeOpen });
    }
    segments.push({ type: 'a2ui-pending' });
  } else if (rest.trim()) {
    segments.push({ type: 'markdown', content: rest });
  }

  return segments.length > 0 ? segments : [{ type: 'markdown', content }];
}

function hasUnclosedA2uiFence(text: string): boolean {
  // Closed fences already consumed by A2UI_FENCE_RE; remaining opener is unclosed.
  return /```a2ui\b/i.test(text);
}
