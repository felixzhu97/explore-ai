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

/** Prefer a selected tool in the model query; keep user text for the bubble. */
export function composeToolAwareQuery(
  question: string,
  toolName: string,
  intentTemplate: string,
): string {
  const trimmed = question.trim();
  const intent = intentTemplate.replaceAll('{name}', toolName).trim();
  if (!trimmed) {
    return intent;
  }
  return `${intent}\n\n${trimmed}`;
}
