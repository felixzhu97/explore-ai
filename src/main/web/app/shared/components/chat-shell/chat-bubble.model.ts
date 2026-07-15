export interface ChatBubbleSource {
  text: string;
  score: number;
  url?: string;
  title?: string;
  metadata?: Record<string, unknown>;
}

export interface ChatBubbleToolStep {
  name: string;
  label: string;
  status: 'running' | 'success' | 'error';
}

export interface ChatBubbleMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp?: number;
  images?: string[];
  streaming?: boolean;
  sources?: ChatBubbleSource[];
  sourcesExpanded?: boolean;
  toolSteps?: ChatBubbleToolStep[];
  assistantIcon?: 'chat' | 'document';
}
