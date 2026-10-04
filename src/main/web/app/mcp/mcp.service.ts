import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

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

@Injectable({ providedIn: 'root' })
export class McpService {
  readonly #http = inject(HttpClient);

  getHealth(): Observable<McpHealthResponse> {
    return this.#http.get<McpHealthResponse>(`${API_BASE_URL}/mcp/health`);
  }

  getClientStatus(): Observable<McpClientStatusResponse> {
    return this.#http.get<McpClientStatusResponse>(`${API_BASE_URL}/mcp/client/status`);
  }

  listTools(): Observable<McpTool[]> {
    return this.#http.get<McpTool[]>(`${API_BASE_URL}/mcp/client/tools`);
  }

  chat(question: string): Observable<McpChatResponse> {
    return this.#http.post<McpChatResponse>(`${API_BASE_URL}/mcp/client/chat`, { question });
  }
}
