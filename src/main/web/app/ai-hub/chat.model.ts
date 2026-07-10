// Chat Feature Models — aligned with backend DTOs (camelCase JSON)

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatMessageData extends ChatMessage {
  id?: string;
  timestamp: number | Date | string;
  toolCalls?: ToolCall[];
  isLoading?: boolean;
}

/** POST /api/text/chat/stream */
export interface ChatStreamRequest {
  messages: ChatMessage[];
  sessionId?: string;
  provider?: string;
  model?: string;
  toolsEnabled?: boolean;
}

export interface ModelInfo {
  name: string;
  provider: string;
  description?: string;
  maxTokens?: number;
}

export interface ProviderInfo {
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

export interface SessionInfo {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  lastActivityAt: string;
}
