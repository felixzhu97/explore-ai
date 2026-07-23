export type SenderActionKind = 'tool' | 'agent' | 'navigate' | 'session';

export interface SenderActionItem {
  id: string;
  kind: SenderActionKind;
  label: string;
  description?: string;
  /** Insert into input when kind=tool */
  promptTemplate?: string;
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
