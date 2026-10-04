export interface ChatSourceView {
  text: string;
  score: number;
  url?: string;
  title?: string;
  /** Publisher date when known (e.g. Serper organic `date`). */
  publishedAt?: string;
  metadata?: Record<string, unknown>;
}

export interface ToolStep {
  name: string;
  label: string;
  status: 'running' | 'success' | 'error';
}

export interface ChatMessageView {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp?: number;
  images?: string[];
  streaming?: boolean;
  sources?: ChatSourceView[];
  toolSteps?: ToolStep[];
  assistantIcon?: 'chat' | 'document';
}
