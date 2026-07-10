// Chat Feature Models

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

export interface ChatRequest {
  messages: ChatMessage[];
  session_id?: string;
  system_prompt?: string;
  temperature?: number;
  max_tokens?: number;
  provider?: string;
  model?: string;
  tools_enabled?: boolean;
}

export interface ChatResponse {
  text: string;
  response?: string;
  provider: string;
  model: string;
  session_id: string;
  usage?: Record<string, number>;
  finish_reason?: string;
}

export interface ModelInfo {
  name: string;
  provider: string;
  description?: string;
  max_tokens?: number;
}

export interface ProviderInfo {
  name: string;
  display_name: string;
  models: string[];
  status: 'available' | 'configured' | 'unavailable';
  supported_languages?: string[];
  features?: string[];
}

export interface ToolCallStatus {
  pending: 'pending';
  running: 'running';
  success: 'success';
  error: 'error';
}

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: 'pending' | 'running' | 'success' | 'error';
}

export interface ChatTabState {
  provider: string;
  model: string;
}

export interface SessionInfo {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  lastActivityAt: string;
}
