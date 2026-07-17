import type { ChatBubbleToolStep } from '@shared/components/chat-shell';

/**
 * DeepSeek DSML tool-call markup helpers (not A2UI).
 * Tags use fullwidth ｜ (U+FF5C) or ASCII |; also tolerate odd spacing.
 */

export interface DsmlToolInvocation {
  toolName: string;
  query: string;
}

const QUERY_MAX = 42;

/** Any opening/closing tag that mentions DSML. */
const DSML_ANY_TAG = /<\/?\s*[^<>]*DSML[^<>]*>/gi;

/** Paired blocks: open tag containing DSML … matching close tag containing DSML. */
const DSML_PAIR_BLOCK =
  /<\s*[^<>]*DSML[^<>]*>[\s\S]*?<\/\s*[^<>]*DSML[^<>]*>/gi;

/** Unclosed DSML open through end (streaming partial). */
const DSML_UNCLOSED = /<\s*[^<>]*DSML[^<>]*>[\s\S]*$/gi;

const INVOKE_RE =
  /<\s*[^<>]*DSML[^<>]*invoke[^<>]*name\s*=\s*["']([^"']+)["'][^<>]*>([\s\S]*?)<\/\s*[^<>]*DSML[^<>]*invoke[^<>]*>/gi;

const QUERY_PARAM_RE =
  /<\s*[^<>]*DSML[^<>]*parameter[^<>]*name\s*=\s*["']query["'][^<>]*>([\s\S]*?)<\/\s*[^<>]*DSML[^<>]*parameter[^<>]*>/i;

export function stripToolCallMarkup(content: string): string {
  if (!content) {
    return '';
  }
  let cleaned = content.replace(DSML_PAIR_BLOCK, '');
  cleaned = cleaned.replace(DSML_UNCLOSED, '');
  cleaned = cleaned.replace(DSML_ANY_TAG, '');
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
    const query = (param?.[1] ?? body.replace(/<[^>]+>/g, '')).trim();
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
