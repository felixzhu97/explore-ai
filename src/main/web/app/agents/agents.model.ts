export interface AgentInfo {
  type: string;
  name: string;
  description: string;
  healthy: boolean;
  supervisor: boolean;
}

export interface AgentHealth {
  type: string;
  healthy: boolean;
  status: string;
}

export interface AgentInvokeRequest {
  message: string;
  sessionId?: string;
  agentType?: string;
}

export type AgentQuickPromptKey =
  | 'supervisor'
  | 'k8s'
  | 'monitoring'
  | 'model'
  | 'llmops'
  | 'aiops'
  | 'vectordb';
