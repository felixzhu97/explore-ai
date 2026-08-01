import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { McpService } from './mcp.service';
import type {
  McpClientStatusResponse,
  McpHealthResponse,
  McpPrompt,
  McpResource,
  McpServerInfo,
  McpTool,
} from './mcp.model';
import { ZardButtonComponent } from '../shared/components/button';

type PrimitiveTab = 'tools' | 'resources' | 'prompts';

@Component({
  selector: 'app-mcp-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './mcp.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class McpPageComponent implements OnInit {
  private readonly mcp = inject(McpService);

  readonly health = signal<McpHealthResponse | null>(null);
  readonly clientStatus = signal<McpClientStatusResponse | null>(null);
  readonly servers = signal<McpServerInfo[]>([]);
  readonly tools = signal<McpTool[]>([]);
  readonly resources = signal<McpResource[]>([]);
  readonly prompts = signal<McpPrompt[]>([]);
  readonly activeTab = signal<PrimitiveTab>('tools');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly question = signal('');
  readonly chatResponse = signal<string | null>(null);
  readonly chatting = signal(false);

  ngOnInit(): void {
    this.loadDashboard();
  }

  selectTab(tab: PrimitiveTab): void {
    this.activeTab.set(tab);
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      health: this.mcp.getHealth().pipe(catchError(() => of(null))),
      status: this.mcp.getClientStatus().pipe(catchError(() => of(null))),
      servers: this.mcp.listServers().pipe(catchError(() => of([] as McpServerInfo[]))),
      tools: this.mcp.listTools().pipe(catchError(() => of([] as McpTool[]))),
      resources: this.mcp.listResources().pipe(catchError(() => of([] as McpResource[]))),
      prompts: this.mcp.listPrompts().pipe(catchError(() => of([] as McpPrompt[]))),
    }).subscribe({
      next: (result) => {
        this.health.set(result.health);
        this.clientStatus.set(result.status);
        this.servers.set(result.servers);
        this.tools.set(result.tools);
        this.resources.set(result.resources);
        this.prompts.set(result.prompts);
        if (!result.health && !result.status) {
          this.error.set('Failed to load MCP dashboard');
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load MCP dashboard');
        this.loading.set(false);
      },
    });
  }

  submitQuestion(): void {
    const question = this.question().trim();
    if (!question) {
      return;
    }

    this.chatting.set(true);
    this.chatResponse.set(null);
    this.mcp.chat(question).subscribe({
      next: (response) => {
        this.chatResponse.set(response.response);
        this.chatting.set(false);
      },
      error: () => {
        this.error.set('MCP chat request failed');
        this.chatting.set(false);
      },
    });
  }
}
