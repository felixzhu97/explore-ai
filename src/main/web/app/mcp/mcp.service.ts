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

  getHealth(): Observable<McpHealthResponse> {
    return this.#http.get<McpHealthResponse>(`${API_BASE_URL}/mcp/health`);
  }

  getClientStatus(): Observable<McpClientStatusResponse> {
    return this.#http.get<McpClientStatusResponse>(`${API_BASE_URL}/mcp/client/status`);
  }

  listTools(): Observable<McpToolResponse[]> {
    return this.#http.get<McpToolResponse[]>(`${API_BASE_URL}/mcp/client/tools`);
  }

  chat(question: string): Observable<McpChatResponse> {
    const request: McpChatRequest = { question };
    return this.#http.post<McpChatResponse>(`${API_BASE_URL}/mcp/client/chat`, request);
  }
}
