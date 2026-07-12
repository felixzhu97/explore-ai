export interface ChatBubbleSource {
  text: string;
  score: number;
  metadata?: Record<string, unknown>;
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
  assistantIcon?: 'chat' | 'document';
}
