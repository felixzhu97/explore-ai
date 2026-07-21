import type { ChatBubbleToolStep } from '../shared/components/chat-shell';

/**
 * DeepSeek DSML tool-call markup helpers (not A2UI).
 * Tags use fullwidth ｜ (U+FF5C) or ASCII |; also tolerate odd spacing.
 */

export interface DsmlToolInvocation {
  toolName: string;
  query: string;
}

const QUERY_MAX = 42;

const INVOKE_RE =
  /<\s*[^<>]*DSML[^<>]*invoke[^<>]*name\s*=\s*["']([^"']+)["'][^<>]*>([\s\S]*?)<\/\s*[^<>]*DSML[^<>]*invoke[^<>]*>/gi;

const QUERY_PARAM_RE =
  /<\s*[^<>]*DSML[^<>]*parameter[^<>]*name\s*=\s*["']query["'][^<>]*>([\s\S]*?)<\/\s*[^<>]*DSML[^<>]*parameter[^<>]*>/i;

export function stripToolCallMarkup(content: string): string {
  if (!content) {
    return '';
  }
  let cleaned = content;
  let previous = '';
  // Repeat until stable — nested/odd DSML fragments need more than one pass.
  while (cleaned !== previous) {
    previous = cleaned;
    cleaned = removeDsmlPairBlocks(cleaned);
    cleaned = removeUnclosedDsmlOpen(cleaned);
    cleaned = removeDsmlTags(cleaned);
  }
  return cleaned
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

export function parseDsmlToolInvocations(content: string): DsmlToolInvocation[] {
  if (!content || !/DSML/i.test(content)) {
    return [];
  }
  const found: DsmlToolInvocation[] = [];
  const seen = new Set<string>();
  INVOKE_RE.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = INVOKE_RE.exec(content)) !== null) {
    const toolName = match[1]?.trim() || 'tool';
    const body = match[2] ?? '';
    const param = body.match(QUERY_PARAM_RE);
    const query = (param?.[1] ?? stripAngleBracketTags(body)).trim();
    const key = `${toolName}\0${query}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    found.push({ toolName, query });
  }
  return found;
}

export function toMinimalToolSteps(
  invocations: readonly DsmlToolInvocation[],
  status: ChatBubbleToolStep['status'] = 'running',
): ChatBubbleToolStep[] {
  return invocations.map(item => ({
    name: item.toolName,
    label: minimalToolLabel(item.toolName, item.query),
    status,
  }));
}

function minimalToolLabel(toolName: string, query: string): string {
  const truncated = truncateQuery(query);
  if (toolName === 'searchWeb') {
    return truncated ? `搜索 · ${truncated}` : '搜索';
  }
  return truncated ? `${toolName} · ${truncated}` : toolName;
}

function truncateQuery(query: string): string {
  const normalized = query.replace(/\s+/g, ' ').trim();
  if (normalized.length <= QUERY_MAX) {
    return normalized;
  }
  return `${normalized.slice(0, QUERY_MAX).trimEnd()}…`;
}

/** Remove paired tags whose open/close both mention DSML. */
function removeDsmlPairBlocks(text: string): string {
  const lower = text.toLowerCase();
  let cursor = 0;
  const parts: string[] = [];
  while (cursor < text.length) {
    const openStart = indexOfDsmlOpenTag(text, lower, cursor);
    if (openStart < 0) {
      parts.push(text.slice(cursor));
      break;
    }
    parts.push(text.slice(cursor, openStart));
    const openEnd = text.indexOf('>', openStart);
    if (openEnd < 0) {
      parts.push(text.slice(openStart));
      break;
    }
    const closeStart = indexOfDsmlCloseTag(text, lower, openEnd + 1);
    if (closeStart < 0) {
      // Leave unclosed open for a later pass.
      parts.push(text.slice(openStart));
      break;
    }
    const closeEnd = text.indexOf('>', closeStart);
    if (closeEnd < 0) {
      parts.push(text.slice(openStart));
      break;
    }
    cursor = closeEnd + 1;
  }
  return parts.join('');
}

/** Drop trailing unclosed DSML open through end of string. */
function removeUnclosedDsmlOpen(text: string): string {
  const lower = text.toLowerCase();
  const openStart = indexOfDsmlOpenTag(text, lower, 0);
  if (openStart < 0) {
    return text;
  }
  // Only strip if no closing DSML tag remains after this open.
  if (indexOfDsmlCloseTag(text, lower, openStart + 1) >= 0) {
    return text;
  }
  return text.slice(0, openStart);
}

/** Remove any single angle-bracket tag that mentions DSML. */
function removeDsmlTags(text: string): string {
  const lower = text.toLowerCase();
  let cursor = 0;
  const parts: string[] = [];
  while (cursor < text.length) {
    const tagStart = text.indexOf('<', cursor);
    if (tagStart < 0) {
      parts.push(text.slice(cursor));
      break;
    }
    parts.push(text.slice(cursor, tagStart));
    const tagEnd = text.indexOf('>', tagStart + 1);
    if (tagEnd < 0) {
      parts.push(text.slice(tagStart));
      break;
    }
    const tag = lower.slice(tagStart, tagEnd + 1);
    if (!tag.includes('dsml')) {
      parts.push(text.slice(tagStart, tagEnd + 1));
    }
    cursor = tagEnd + 1;
  }
  return parts.join('');
}

function indexOfDsmlOpenTag(text: string, lower: string, from: number): number {
  let cursor = from;
  while (cursor < text.length) {
    const start = text.indexOf('<', cursor);
    if (start < 0) {
      return -1;
    }
    if (lower.charAt(start + 1) === '/') {
      cursor = start + 1;
      continue;
    }
    const end = text.indexOf('>', start + 1);
    if (end < 0) {
      return -1;
    }
    if (lower.slice(start, end + 1).includes('dsml')) {
      return start;
    }
    cursor = end + 1;
  }
  return -1;
}

function indexOfDsmlCloseTag(text: string, lower: string, from: number): number {
  let cursor = from;
  while (cursor < text.length) {
    const start = lower.indexOf('</', cursor);
    if (start < 0) {
      return -1;
    }
    const end = text.indexOf('>', start + 2);
    if (end < 0) {
      return -1;
    }
    if (lower.slice(start, end + 1).includes('dsml')) {
      return start;
    }
    cursor = end + 1;
  }
  return -1;
}

function stripAngleBracketTags(text: string): string {
  let cursor = 0;
  const parts: string[] = [];
  while (cursor < text.length) {
    const open = text.indexOf('<', cursor);
    if (open < 0) {
      parts.push(text.slice(cursor));
      break;
    }
    parts.push(text.slice(cursor, open));
    const close = text.indexOf('>', open + 1);
    if (close < 0) {
      break;
    }
    cursor = close + 1;
  }
  return parts.join('');
}
