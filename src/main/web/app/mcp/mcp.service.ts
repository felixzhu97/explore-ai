import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type {
  McpChatResponse,
  McpClientStatusResponse,
  McpHealthResponse,
  McpPrompt,
  McpResource,
  McpServerInfo,
  McpTool,
} from './mcp.model';

@Injectable({ providedIn: 'root' })
export class McpService {
  private readonly http = inject(HttpClient);

  getHealth(): Observable<McpHealthResponse> {
    return this.http.get<McpHealthResponse>(`${API_BASE_URL}/mcp/health`);
  }

  getClientStatus(): Observable<McpClientStatusResponse> {
    return this.http.get<McpClientStatusResponse>(`${API_BASE_URL}/mcp/client/status`);
  }

  listServers(): Observable<McpServerInfo[]> {
    return this.http.get<McpServerInfo[]>(`${API_BASE_URL}/mcp/client/servers`);
  }

  listTools(): Observable<McpTool[]> {
    return this.http.get<McpTool[]>(`${API_BASE_URL}/mcp/client/tools`);
  }

  listResources(): Observable<McpResource[]> {
    return this.http.get<McpResource[]>(`${API_BASE_URL}/mcp/client/resources`);
  }

  listPrompts(): Observable<McpPrompt[]> {
    return this.http.get<McpPrompt[]>(`${API_BASE_URL}/mcp/client/prompts`);
  }

  chat(question: string): Observable<McpChatResponse> {
    return this.http.post<McpChatResponse>(`${API_BASE_URL}/mcp/client/chat`, { question });
  }
}
