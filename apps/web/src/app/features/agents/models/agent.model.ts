// Agent Feature Models

export interface Agent {
  id: string;
  name: string;
  description: string;
  icon?: string;
  endpoint: string;
}

export interface AgentInfo {
  name: string;
  description: string;
  status?: 'online' | 'offline' | 'busy';
}

export type ToolCallStatus = 'pending' | 'running' | 'success' | 'error';

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: ToolCallStatus;
}

export interface ChatMessageData {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number | Date;
  toolCalls?: ToolCall[];
  isLoading?: boolean;
}
