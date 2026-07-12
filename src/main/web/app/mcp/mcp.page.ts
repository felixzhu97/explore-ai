import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { McpService } from './mcp.service';
import type { McpClientStatusResponse, McpHealthResponse, McpTool } from './mcp.model';
import { ZardButtonComponent } from '@/shared/components/button';

@Component({
  selector: 'app-mcp-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './mcp.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class McpPageComponent implements OnInit {
  private readonly mcp = inject(McpService);

  readonly health = signal<McpHealthResponse | null>(null);
  readonly clientStatus = signal<McpClientStatusResponse | null>(null);
  readonly tools = signal<McpTool[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly question = signal('');
  readonly chatResponse = signal<string | null>(null);
  readonly chatting = signal(false);

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    this.mcp.getHealth().subscribe({
      next: health => this.health.set(health),
      error: () => this.error.set('Failed to load MCP health'),
    });

    this.mcp.getClientStatus().subscribe({
      next: status => this.clientStatus.set(status),
      error: () => this.error.set('Failed to load MCP client status'),
    });

    this.mcp.listTools().subscribe({
      next: (tools) => {
        this.tools.set(tools);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load MCP tools');
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
