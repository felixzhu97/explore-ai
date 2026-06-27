import { Component, ChangeDetectionStrategy, input, signal } from '@angular/core';

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
    <div class="mt-2 text-sm rounded-md border border-[--color-border] overflow-hidden bg-[--color-surface-secondary]">
      <div
        class="flex items-center gap-2 px-4 py-2 cursor-pointer transition-colors duration-150"
        [class]="getHeaderClasses()"
        (click)="toggleExpanded()"
      >
        <svg class="w-3.5 h-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
        </svg>
        <span class="flex-1 font-medium text-text">{{ toolCall().name }}</span>
        <span class="flex items-center justify-center w-5 min-w-5" [class]="getStatusClasses()">
          @switch (toolCall().status) {
            @case ('pending') {
              <span class="text-text-tertiary">○</span>
            }
            @case ('running') {
              <span class="inline-block w-3 h-3 border-2 border-current border-r-transparent rounded-full animate-spin"></span>
            }
            @case ('success') {
              <span class="text-success font-bold">✓</span>
            }
            @case ('error') {
              <span class="text-error font-bold">✗</span>
            }
          }
        </span>
        <span class="w-0 h-0 border-l-4 border-r-4 border-t-4 border-l-transparent border-r-transparent transition-transform duration-200" [class.border-t-text-tertiary]="!expanded()" [class.border-t-transparent]="expanded()" [class.rotate-180]="expanded()"></span>
      </div>

      @if (expanded()) {
        <div class="px-4 py-4 border-t border-[--color-border] max-h-[300px] overflow-y-auto">
          <div class="text-[11px] text-text-tertiary uppercase tracking-wide mb-1">Input</div>
          <pre class="text-xs leading-relaxed text-text bg-background p-2 rounded-sm overflow-x-auto whitespace-pre-wrap break-all font-mono m-0">{{ formatJson(toolCall().input) }}</pre>

          @if (toolCall().output) {
            <div class="text-[11px] text-text-tertiary uppercase tracking-wide mt-4 mb-1">Output</div>
            @if (toolCall().status === 'error') {
              <div class="text-sm text-error">{{ toolCall().output }}</div>
            } @else {
              <pre class="text-xs leading-relaxed text-text bg-background p-2 rounded-sm overflow-x-auto whitespace-pre-wrap break-all font-mono m-0" [innerHTML]="formatOutput(toolCall().output!)"></pre>
            }
          } @else if (toolCall().status === 'success') {
            <div class="text-sm italic text-text-tertiary mt-4">No output</div>
          }
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToolResultComponent {
  readonly toolCall = input.required<ToolCall>();

  readonly expanded = signal(false);

  toggleExpanded() {
    this.expanded.update(v => !v);
  }

  getHeaderClasses(): string {
    switch (this.toolCall().status) {
      case 'error':
        return 'bg-error-light hover:bg-error-light';
      case 'success':
        return 'bg-success-light hover:bg-success-light';
      default:
        return 'bg-surface hover:bg-[#f0f0f2]';
    }
  }

  getStatusClasses(): string {
    switch (this.toolCall().status) {
      case 'pending':
        return 'text-text-tertiary';
      case 'running':
        return 'text-warning';
      case 'success':
        return 'text-success';
      case 'error':
        return 'text-error';
    }
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

    const imagePattern = /(https?:\/\/[^\s]+\.(?:png|jpg|jpeg|gif|webp|bmp|svg)(?:\?[^\s]*)?)/gi;
    const imageMatches = [...trimmed.matchAll(imagePattern)];

    if (imageMatches.length > 0) {
      const imageUrl = imageMatches[0][1];
      const imageStartIndex = trimmed.indexOf(imageUrl);
      const beforeText = trimmed.substring(0, imageStartIndex).trim();
      const afterText = trimmed.substring(imageStartIndex + imageUrl.length).trim();

      return `${beforeText ? beforeText + ' ' : ''}[Image: ${imageUrl}]${afterText ? ' ' + afterText : ''}`;
    }

    if (
      (trimmed.startsWith('{') && trimmed.endsWith('}'))
      || (trimmed.startsWith('[') && trimmed.endsWith(']'))
    ) {
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
