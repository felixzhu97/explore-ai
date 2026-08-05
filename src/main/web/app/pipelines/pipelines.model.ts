export interface AgentInfo {
  type: string;
  name: string;
  description: string;
  healthy: boolean;
  supervisor: boolean;
  runtime?: string;
  toolKeys?: string[];
  systemPrompt?: string;
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

/** Builtin multilingual workflow template from the backend catalog. */
export interface WorkflowTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  nameAliases?: string[];
}

/** Client-owned saved workflow template. */
export interface SavedWorkflowTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  sourceTemplateId?: string | null;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowTemplateWriteRequest {
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
}
