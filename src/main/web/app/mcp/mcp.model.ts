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

export interface McpTool {
  name: string;
  description: string;
}

export interface McpChatResponse {
  response: string;
}
