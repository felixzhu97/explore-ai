import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { CardComponent } from '@shared/components/ui/card/card.component';
import { McpService, ToolDefinition, ToolInvokeResult } from '@core/services/mcp.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-mcp-tools-tab',
  standalone: true,
  imports: [CardComponent, FormsModule, JsonPipe],
  template: `
    <div class="container">
      <div class="header">
        <h2>MCP Tools Inspector</h2>
        <p class="subtitle">Browse and invoke MCP tools (RAG, Chat, Monitor, Web Search)</p>
      </div>

      @if (isLoading()) {
        <div class="loading">
          <span class="spinner"></span>
          <span>Loading tools...</span>
        </div>
      } @else if (error()) {
        <div class="error">{{ error() }}</div>
      } @else {
        <div class="tools-grid">
          @for (tool of tools(); track tool.name) {
            <app-card variant="outlined" [hoverable]="true">
              <div class="tool-card" (click)="openToolModal(tool)">
                <div class="tool-header">
                  <h3 class="tool-name">{{ tool.name }}</h3>
                  @if (tool.composite) {
                    <span class="badge composite">Composite</span>
                  }
                </div>
                <p class="tool-description">{{ tool.description }}</p>
                <button class="try-button" (click)="openToolModal(tool); $event.stopPropagation()">
                  Try it
                </button>
              </div>
            </app-card>
          }
        </div>
      }

      @if (selectedTool()) {
        <div class="modal-overlay" (click)="closeModal()">
          <div class="modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>{{ selectedTool()!.name }}</h3>
              <button class="close-btn" (click)="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
              <p class="modal-description">{{ selectedTool()!.description }}</p>

              <form class="invoke-form" (submit)="invokeTool($event)">
                <h4>Parameters</h4>
                @for (param of getToolParams(selectedTool()!); track param.name) {
                  <div class="form-group">
                    <label [for]="param.name"
                      >{{ param.name }}
                      @if (param.required) {
                        <span class="required">*</span>
                      }
                    </label>
                    @if (param.type === 'string') {
                      <input
                        type="text"
                        [id]="param.name"
                        [(ngModel)]="paramValues[param.name]"
                        [name]="param.name"
                        [placeholder]="param.description || param.name"
                      />
                    } @else if (param.type === 'number') {
                      <input
                        type="number"
                        [id]="param.name"
                        [(ngModel)]="paramValues[param.name]"
                        [name]="param.name"
                        [placeholder]="param.description || param.name"
                      />
                    } @else if (param.type === 'boolean') {
                      <input
                        type="checkbox"
                        [id]="param.name"
                        [(ngModel)]="paramValues[param.name]"
                        [name]="param.name"
                      />
                    } @else {
                      <input
                        type="text"
                        [id]="param.name"
                        [(ngModel)]="paramValues[param.name]"
                        [name]="param.name"
                        [placeholder]="param.description || param.name"
                      />
                    }
                  </div>
                }
                <div class="form-actions">
                  <button type="submit" class="invoke-btn" [disabled]="isInvoking()">
                    @if (isInvoking()) {
                      <span class="spinner small"></span>
                      Invoking...
                    } @else {
                      Invoke
                    }
                  </button>
                </div>
              </form>

              @if (invokeResult()) {
                <div class="result-section">
                  <h4>Result</h4>
                  <div class="result-columns">
                    <div class="result-column">
                      <h5>Content</h5>
                      <pre class="result-content">{{ invokeResult()!.content }}</pre>
                    </div>
                    @if (invokeResult()!.structured) {
                      <div class="result-column">
                        <h5>Structured</h5>
                        <pre class="result-json">{{ invokeResult()!.structured | json }}</pre>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .container {
        padding: var(--spacing-lg);
      }

      .header {
        margin-bottom: var(--spacing-xl);
      }

      .header h2 {
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-semibold);
        margin: 0 0 var(--spacing-xs) 0;
      }

      .subtitle {
        color: var(--color-text-secondary);
        margin: 0;
      }

      .loading,
      .error {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--spacing-sm);
        padding: var(--spacing-xxl);
        color: var(--color-text-secondary);
      }

      .error {
        color: var(--color-error);
      }

      .spinner {
        display: inline-block;
        width: 20px;
        height: 20px;
        border: 2px solid currentColor;
        border-right-color: transparent;
        border-radius: 50%;
        animation: spin 0.6s linear infinite;
      }

      .spinner.small {
        width: 14px;
        height: 14px;
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .tools-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: var(--spacing-lg);
      }

      .tool-card {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-sm);
      }

      .tool-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--spacing-sm);
      }

      .tool-name {
        font-size: var(--font-size-base);
        font-weight: var(--font-weight-medium);
        margin: 0;
      }

      .badge {
        font-size: var(--font-size-xs);
        padding: 2px 8px;
        border-radius: var(--radius-full);
        font-weight: var(--font-weight-medium);
      }

      .badge.composite {
        background: var(--color-primary-light);
        color: var(--color-primary);
      }

      .tool-description {
        font-size: var(--font-size-sm);
        color: var(--color-text-secondary);
        margin: 0;
        flex: 1;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .try-button {
        padding: var(--spacing-xs) var(--spacing-md);
        background: var(--color-primary);
        color: white;
        border: none;
        border-radius: var(--radius-md);
        font-size: var(--font-size-sm);
        cursor: pointer;
        transition: background var(--transition-fast);
        align-self: flex-start;
      }

      .try-button:hover {
        background: var(--color-primary-hover);
      }

      .modal-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        padding: var(--spacing-lg);
      }

      .modal {
        background: var(--color-surface);
        border-radius: var(--radius-xl);
        width: 100%;
        max-width: 600px;
        max-height: 80vh;
        overflow: hidden;
        display: flex;
        flex-direction: column;
      }

      .modal-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: var(--spacing-lg);
        border-bottom: 1px solid var(--color-border);
      }

      .modal-header h3 {
        margin: 0;
        font-size: var(--font-size-lg);
      }

      .close-btn {
        background: none;
        border: none;
        font-size: 24px;
        cursor: pointer;
        color: var(--color-text-secondary);
      }

      .close-btn:hover {
        color: var(--color-text);
      }

      .modal-body {
        padding: var(--spacing-lg);
        overflow-y: auto;
      }

      .modal-description {
        color: var(--color-text-secondary);
        margin: 0 0 var(--spacing-lg) 0;
      }

      .invoke-form {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .invoke-form h4 {
        margin: 0;
        font-size: var(--font-size-base);
      }

      .form-group {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-xs);
      }

      .form-group label {
        font-size: var(--font-size-sm);
        font-weight: var(--font-weight-medium);
      }

      .required {
        color: var(--color-error);
      }

      .form-group input[type='text'],
      .form-group input[type='number'] {
        padding: var(--spacing-sm) var(--spacing-md);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        font-size: var(--font-size-base);
        background: var(--color-background);
      }

      .form-group input[type='checkbox'] {
        width: 18px;
        height: 18px;
      }

      .form-actions {
        margin-top: var(--spacing-sm);
      }

      .invoke-btn {
        padding: var(--spacing-sm) var(--spacing-xl);
        background: var(--color-primary);
        color: white;
        border: none;
        border-radius: var(--radius-md);
        font-size: var(--font-size-base);
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);
      }

      .invoke-btn:hover:not(:disabled) {
        background: var(--color-primary-hover);
      }

      .invoke-btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .result-section {
        margin-top: var(--spacing-lg);
        padding-top: var(--spacing-lg);
        border-top: 1px solid var(--color-border);
      }

      .result-section h4 {
        margin: 0 0 var(--spacing-md) 0;
      }

      .result-columns {
        display: grid;
        grid-template-columns: 1fr;
        gap: var(--spacing-md);
      }

      .result-column h5 {
        margin: 0 0 var(--spacing-xs) 0;
        font-size: var(--font-size-sm);
        color: var(--color-text-secondary);
      }

      .result-content {
        background: var(--color-background);
        padding: var(--spacing-md);
        border-radius: var(--radius-md);
        font-size: var(--font-size-sm);
        white-space: pre-wrap;
        word-break: break-word;
        max-height: 200px;
        overflow-y: auto;
        margin: 0;
      }

      .result-json {
        background: var(--color-background);
        padding: var(--spacing-md);
        border-radius: var(--radius-md);
        font-size: var(--font-size-sm);
        white-space: pre-wrap;
        word-break: break-word;
        max-height: 200px;
        overflow-y: auto;
        margin: 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class McpToolsTabComponent implements OnInit {
  private mcpService = inject(McpService);
  private notification = inject(NotificationService);

  tools = signal<ToolDefinition[]>([]);
  isLoading = signal(true);
  error = signal<string | null>(null);
  selectedTool = signal<ToolDefinition | null>(null);
  isInvoking = signal(false);
  invokeResult = signal<ToolInvokeResult | null>(null);
  paramValues: Record<string, unknown> = {};

  ngOnInit() {
    this.loadTools();
  }

  loadTools() {
    this.isLoading.set(true);
    this.error.set(null);
    this.mcpService.listTools().subscribe({
      next: (tools) => {
        this.tools.set(tools);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load tools: ' + (err.message || 'Unknown error'));
        this.isLoading.set(false);
        this.notification.showError('Failed to load MCP tools');
      },
    });
  }

  openToolModal(tool: ToolDefinition) {
    this.selectedTool.set(tool);
    this.paramValues = {};
    this.invokeResult.set(null);
  }

  closeModal() {
    this.selectedTool.set(null);
  }

  getToolParams(
    tool: ToolDefinition
  ): Array<{ name: string; type: string; required: boolean; description?: string }> {
    const schema = tool.inputSchema as {
      properties?: Record<string, { type?: string; description?: string }>;
      required?: string[];
    };
    if (!schema.properties) return [];
    const required = schema.required || [];
    return Object.entries(schema.properties).map(([name, prop]) => ({
      name,
      type: prop.type || 'string',
      required: required.includes(name),
      description: prop.description,
    }));
  }

  invokeTool(event: Event) {
    event.preventDefault();
    const tool = this.selectedTool();
    if (!tool) return;

    this.isInvoking.set(true);
    this.invokeResult.set(null);

    this.mcpService.invokeTool(tool.name, this.paramValues).subscribe({
      next: (result) => {
        this.invokeResult.set(result);
        this.isInvoking.set(false);
        if (result.isError) {
          this.notification.showError('Tool invocation failed');
        } else {
          this.notification.showSuccess('Tool invoked successfully');
        }
      },
      error: (err) => {
        this.isInvoking.set(false);
        this.notification.showError('Failed to invoke tool: ' + (err.message || 'Unknown error'));
      },
    });
  }
}
