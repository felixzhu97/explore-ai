import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { McpService } from '../services/mcp.service';
import type { McpClientStatusResponse, McpHealthResponse, McpTool } from '../mcp.model';
import { ZardButtonComponent } from '../../../shared/components/button';
import { I18nService } from '../../../core/i18n';

@Component({
  selector: 'app-mcp-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './mcp.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class McpPageComponent implements OnInit {
  private readonly mcp = inject(McpService);
  protected readonly i18n = inject(I18nService);

  readonly health = signal<McpHealthResponse | null>(null);
  readonly clientStatus = signal<McpClientStatusResponse | null>(null);
  readonly tools = signal<McpTool[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly question = signal('');
  readonly chatResponse = signal<string | null>(null);
  readonly isChatting = signal(false);

  ngOnInit(): void {
    this.loadDashboard();
  }

  toolsCountLabel(count: number): string {
    return this.i18n.tReplace(this.i18n.t().mcpPage.toolsCount, { count });
  }

  loadDashboard(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.mcp.getHealth().subscribe({
      next: health => this.health.set(health),
      error: () => this.error.set(this.i18n.t().mcpPage.errors.healthFailed),
    });

    this.mcp.getClientStatus().subscribe({
      next: status => this.clientStatus.set(status),
      error: () => this.error.set(this.i18n.t().mcpPage.errors.clientStatusFailed),
    });

    this.mcp.listTools().subscribe({
      next: (tools) => {
        this.tools.set(tools);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().mcpPage.errors.toolsFailed);
        this.isLoading.set(false);
      },
    });
  }

  submitQuestion(): void {
    const question = this.question().trim();
    if (!question) {
      return;
    }

    this.isChatting.set(true);
    this.chatResponse.set(null);
    this.mcp.chat(question).subscribe({
      next: (response) => {
        this.chatResponse.set(response.response);
        this.isChatting.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().mcpPage.errors.chatFailed);
        this.isChatting.set(false);
      },
    });
  }
}
