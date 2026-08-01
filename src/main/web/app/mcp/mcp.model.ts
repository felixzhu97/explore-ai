export interface McpHealthResponse {
  status: string;
  server: string;
  version: string;
  protocol: string;
}

export interface McpClientStatusResponse {
  status: string;
  registeredTools: number;
  connectedServers: string[];
}

export interface McpServerCapabilities {
  tools: boolean;
  resources: boolean;
  prompts: boolean;
}

export interface McpServerInfo {
  name: string;
  toolCount: number;
  resourceCount: number;
  promptCount: number;
  status: string;
  capabilities: McpServerCapabilities;
}

export interface McpTool {
  name: string;
  description: string;
  serverName: string;
}

export interface McpResource {
  uri: string;
  name: string;
  description: string;
  serverName: string;
}

export interface McpPrompt {
  name: string;
  description: string;
  serverName: string;
}

export interface McpChatResponse {
  response: string;
}
