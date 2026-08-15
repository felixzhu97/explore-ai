export type MermaidSegment =
  | { type: 'markdown'; content: string }
  | { type: 'mermaid'; source: string; diagramId: string }
  | { type: 'mermaid-pending' };

/** Closing ``` alone on its line (no language tag) so later fences are not stolen. */
const MERMAID_FENCE_RE = /```mermaid\s*\n([\s\S]*?)\n```[ \t]*(?:\n|$)/gi;

/** Stable short id from fence body (djb2). */
export function stableMermaidId(source: string): string {
  let hash = 5381;
  for (let i = 0; i < source.length; i++) {
    hash = ((hash << 5) + hash) ^ source.charCodeAt(i);
  }
  return `mermaid-${(hash >>> 0).toString(16)}`;
}

/**
 * Split markdown into markdown / completed mermaid fences / pending fence.
 */
export function splitMarkdownAndMermaid(content: string): MermaidSegment[] {
  if (!content) {
    return [];
  }

  const segments: MermaidSegment[] = [];
  let lastIndex = 0;
  MERMAID_FENCE_RE.lastIndex = 0;

  let match: RegExpExecArray | null;
  while ((match = MERMAID_FENCE_RE.exec(content)) !== null) {
    const before = content.slice(lastIndex, match.index);
    if (before.trim()) {
      segments.push({ type: 'markdown', content: before });
    }
    const source = (match[1] ?? '').trimEnd();
    segments.push({
      type: 'mermaid',
      source,
      diagramId: stableMermaidId(source),
    });
    lastIndex = match.index + match[0].length;
  }

  const rest = content.slice(lastIndex);
  if (hasUnclosedMermaidFence(rest)) {
    const openIndex = rest.search(/```mermaid\b/i);
    const beforeOpen = openIndex >= 0 ? rest.slice(0, openIndex) : '';
    if (beforeOpen.trim()) {
      segments.push({ type: 'markdown', content: beforeOpen });
    }
    segments.push({ type: 'mermaid-pending' });
  } else if (rest.trim()) {
    segments.push({ type: 'markdown', content: rest });
  }

  return segments.length > 0 ? segments : [{ type: 'markdown', content }];
}

function hasUnclosedMermaidFence(text: string): boolean {
  return /```mermaid\b/i.test(text);
}
