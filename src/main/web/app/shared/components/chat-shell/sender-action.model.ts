export type SenderActionKind = 'tool' | 'agent' | 'navigate' | 'session';

export interface SenderActionItem {
  id: string;
  kind: SenderActionKind;
  label: string;
  description?: string;
  /** Route when kind=navigate or agent */
  path?: string;
  /** Agent type when kind=agent */
  agentType?: string;
}

export interface SenderActionGroup {
  id: string;
  label: string;
  items: SenderActionItem[];
}

/** Prefer selected tools in the model query; keep user text for the bubble. */
export function composeToolAwareQuery(
  question: string,
  toolNames: string | readonly string[],
  intentTemplate: string,
): string {
  const names = (Array.isArray(toolNames) ? toolNames : [toolNames])
    .map(name => name.trim())
    .filter(Boolean);
  const trimmed = question.trim();
  if (names.length === 0) {
    return trimmed;
  }
  const intent = intentTemplate.replaceAll('{name}', names.join(', ')).trim();
  if (!trimmed) {
    return intent;
  }
  return `${intent}\n\n${trimmed}`;
}

export function appendUniqueSenderAction(
  current: readonly SenderActionItem[],
  item: SenderActionItem,
): SenderActionItem[] {
  if (current.some(existing => existing.id === item.id)) {
    return [...current];
  }
  return [...current, item];
}

export function removeSenderActionById(
  current: readonly SenderActionItem[],
  id: string,
): SenderActionItem[] {
  return current.filter(item => item.id !== id);
}
