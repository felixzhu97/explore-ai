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
  templateUrl: './tool-result.component.html',
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
