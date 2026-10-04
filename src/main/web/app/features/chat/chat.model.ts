// Chat Feature Models — aligned with backend DTOs (camelCase JSON)

import type { ToolStep } from '../../shared/components/chat-shell/chat-bubble.model';

export interface ChatStreamMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatHistoryMessage extends ChatStreamMessage {
  id?: string;
  timestamp: number | Date | string;
  toolCalls?: ToolCall[];
  isLoading?: boolean;
  sources?: WebSource[];
}

/** POST /api/chat/stream */
export interface ChatStreamRequest {
  messages: ChatStreamMessage[];
  sessionId?: string;
  provider?: string;
  model?: string;
  toolsEnabled?: boolean;
  skillIds?: string[];
}

export interface ChatModel {
  name: string;
  provider: string;
  description?: string;
  maxTokens?: number;
}

export interface ChatProvider {
  name: string;
  displayName: string;
  models: string[];
  status: 'available' | 'unavailable';
}

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: 'pending' | 'running' | 'success' | 'error';
}

export interface ChatSessionSummary {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  lastActivityAt: string;
}

/** Web search hit from the `sources` SSE event or session history (WebSourceResponse). */
export interface WebSource {
  title: string;
  url: string;
  snippet: string;
  publishedAt?: string;
}

/** Message in the local chat thread, merged from the stream and session history. */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  toolSteps?: ToolStep[];
  sources?: WebSource[];
}
