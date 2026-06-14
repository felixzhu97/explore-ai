import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE_URL = '/api/mcp';

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
  composite?: boolean;
}

export interface ToolInvokeResult {
  content: string;
  isError: boolean;
  structured?: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class McpService {
  private http = inject(HttpClient);

  listTools(): Observable<ToolDefinition[]> {
    return this.http.get<ToolDefinition[]>(`${BASE_URL}/.well-known/tools`);
  }

  invokeTool(name: string, args: Record<string, unknown>): Observable<ToolInvokeResult> {
    const body = {
      jsonrpc: '2.0',
      id: crypto.randomUUID(),
      method: 'tools/call',
      params: { name, arguments: args },
    };
    return this.http.post<ToolInvokeResult>(`${BASE_URL}/messages`, body);
  }
}
