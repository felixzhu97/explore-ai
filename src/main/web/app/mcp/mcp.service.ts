import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { HealthStatus } from '../http/health-status';

export interface McpHealthResponse {
  status: HealthStatus;
  server: string;
  version: string;
  protocol: string;
}

export type McpClientStatus = 'READY';

export interface McpClientStatusResponse {
  status: McpClientStatus;
  registeredTools: number;
  connectedServers: string[];
}

export interface McpToolResponse {
  name: string;
  description: string;
}

export interface McpChatRequest {
  question: string;
  documentIds?: string[];
}

export interface McpChatResponse {
  response: string;
}

@Service()
export class McpService {
  readonly #http = inject(HttpClient);

  /** Returns the MCP module health. */
  getHealth(): Observable<McpHealthResponse> {
    return this.#http.get<McpHealthResponse>(`${API_BASE_URL}/mcp/health`);
  }

  /** Returns the MCP client status. */
  getClientStatus(): Observable<McpClientStatusResponse> {
    return this.#http.get<McpClientStatusResponse>(`${API_BASE_URL}/mcp/client/status`);
  }

  /** Lists the MCP client tools. */
  listTools(): Observable<McpToolResponse[]> {
    return this.#http.get<McpToolResponse[]>(`${API_BASE_URL}/mcp/client/tools`);
  }

  /** Asks a question through the MCP chat. */
  sendChat(question: string): Observable<McpChatResponse> {
    const request: McpChatRequest = { question };
    return this.#http.post<McpChatResponse>(`${API_BASE_URL}/mcp/client/chat`, request);
  }
}
