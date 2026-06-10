import { Component, ChangeDetectionStrategy, input, signal, computed } from '@angular/core';

export type ToolCallStatus = 'pending' | 'running' | 'success' | 'error';

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: ToolCallStatus;
}

@Component({
  selector: 'app-tool-result',
  standalone: true,
  template: `
    <div class="tool-container">
      <div class="tool-header" [class]="'tool-header--' + toolCall().status" (click)="toggleExpanded()">
        <span class="tool-icon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
          </svg>
        </span>
        <span class="tool-name">{{ toolCall().name }}</span>
        <span class="status-indicator" [class]="'status-indicator--' + toolCall().status">
          @switch (toolCall().status) {
            @case ('pending') { ○ }
            @case ('running') { <span class="spinner"></span> }
            @case ('success') { ✓ }
            @case ('error') { ✗ }
          }
        </span>
        <span class="expand-icon" [class.expanded]="expanded()"></span>
      </div>

      @if (expanded()) {
        <div class="tool-body">
          <div class="section-label">Input</div>
          <pre class="code-block">{{ formatJson(toolCall().input) }}</pre>

          @if (toolCall().output) {
            <div class="section-label" style="margin-top: var(--spacing-md)">Output</div>
            @if (toolCall().status === 'error') {
              <div class="error-text">{{ toolCall().output }}</div>
            } @else {
              <pre class="code-block" [innerHTML]="formatOutput(toolCall().output!)"></pre>
            }
          } @else if (toolCall().status === 'success') {
            <div class="empty-output">No output</div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .tool-container {
      background: var(--color-surface-secondary);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      overflow: hidden;
      font-size: var(--font-size-sm);
      margin-top: var(--spacing-sm);
    }

    .tool-header {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      padding: var(--spacing-sm) var(--spacing-md);
      cursor: pointer;
      transition: background 0.15s ease;

      &--error { background: var(--color-error-light); }
      &--success { background: var(--color-success-light); }
      &--pending, &--running { background: var(--color-surface); }

      &:hover {
        &--error { background: var(--color-error-light); }
        &--success { background: var(--color-success-light); }
        &--pending, &--running { background: var(--color-surface-tertiary); }
      }
    }

    .tool-icon {
      font-size: 14px;
    }

    .tool-name {
      font-weight: var(--font-weight-medium);
      color: var(--color-text);
      flex: 1;
    }

    .status-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 20px;

      &--pending { color: var(--color-text-tertiary); }
      &--running { color: var(--color-warning); }
      &--success { color: var(--color-success); }
      &--error { color: var(--color-error); }
    }

    .spinner {
      display: inline-block;
      width: 12px;
      height: 12px;
      border: 2px solid currentColor;
      border-right-color: transparent;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .expand-icon {
      display: inline-block;
      width: 0;
      height: 0;
      border-left: 4px solid transparent;
      border-right: 4px solid transparent;
      border-top: 4px solid var(--color-text-tertiary);
      transition: transform 0.2s ease;

      &.expanded {
        transform: rotate(180deg);
      }
    }

    .tool-body {
      padding: var(--spacing-md);
      border-top: 1px solid rgba(0, 0, 0, 0.08);
      max-height: 300px;
      overflow-y: auto;
    }

    .section-label {
      font-size: var(--font-size-xs);
      color: var(--color-text-tertiary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: var(--spacing-xs);
    }

    .code-block {
      font-family: "SF Mono", Monaco, "Cascadia Code", monospace;
      font-size: var(--font-size-xs);
      line-height: 1.5;
      color: var(--color-text);
      background: var(--color-background);
      padding: var(--spacing-sm);
      border-radius: var(--radius-sm);
      overflow-x: auto;
      margin: 0;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .error-text {
      color: var(--color-error);
      font-size: var(--font-size-sm);
    }

    .empty-output {
      color: var(--color-text-tertiary);
      font-style: italic;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToolResultComponent {
  toolCall = input.required<ToolCall>();

  expanded = signal(false);

  toggleExpanded() {
    this.expanded.update(v => !v);
  }

  formatJson(obj: unknown): string {
    try {
      return JSON.stringify(obj, null, 2);
    } catch {
      return String(obj);
    }
  }

  formatOutput(output: string): string {
    const trimmed = output.trim();
    
    // Detect image URLs
    const imagePattern = /(https?:\/\/[^\s]+\.(?:png|jpg|jpeg|gif|webp|bmp|svg)(?:\?[^\s]*)?)/gi;
    const imageMatches = [...trimmed.matchAll(imagePattern)];
    
    if (imageMatches.length > 0) {
      const imageUrl = imageMatches[0][1];
      const imageStartIndex = trimmed.indexOf(imageUrl);
      const beforeText = trimmed.substring(0, imageStartIndex).trim();
      const afterText = trimmed.substring(imageStartIndex + imageUrl.length).trim();
      
      // For simplicity, return the text with image placeholder
      return `${beforeText ? beforeText + ' ' : ''}[Image: ${imageUrl}]${afterText ? ' ' + afterText : ''}`;
    }

    // Try to format JSON
    if ((trimmed.startsWith('{') && trimmed.endsWith('}')) ||
        (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
      try {
        const parsed = JSON.parse(trimmed);
        return JSON.stringify(parsed, null, 2);
      } catch {
        // Not valid JSON
      }
    }

    return output;
  }
}
