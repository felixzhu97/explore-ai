import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ToolCall } from '../tool-result/tool-result.component';
import { ToolResultComponent } from '../tool-result/tool-result.component';

export interface ChatMessageData {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
  toolCalls?: ToolCall[];
}

@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [ToolResultComponent],
  template: `
    <div class="message-bubble" [class.message-bubble--user]="isUser()">
      <div class="message-content" [class.message-content--user]="isUser()">
        @if (isUser()) {
          <p>{{ message().content }}</p>
        } @else {
          <div [innerHTML]="renderedContent()"></div>
        }
      </div>

      @if (message().toolCalls && message().toolCalls!.length > 0) {
        <div class="tool-calls">
          @for (toolCall of message().toolCalls; track toolCall.id) {
            <app-tool-result [toolCall]="toolCall" />
          }
        </div>
      }

      <div class="message-meta">
        <span class="message-time">{{ formattedTime() }}</span>
      </div>
    </div>
  `,
  styles: [`
    .message-bubble {
      display: flex;
      flex-direction: column;
      max-width: 80%;
      animation: fadeIn 0.2s ease;
      align-self: flex-start;
      align-items: flex-start;

      &--user {
        align-self: flex-end;
        align-items: flex-end;
      }
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .message-content {
      padding: var(--spacing-md);
      border-radius: var(--radius-lg);
      font-size: var(--font-size-base);
      line-height: var(--line-height-relaxed);
      word-break: break-word;

      &:not(.message-content--user) {
        background: var(--color-surface);
        border: 1px solid rgba(0, 0, 0, 0.08);
        border-bottom-left-radius: var(--radius-sm);
        color: var(--color-text);
      }

      &--user {
        background: var(--color-primary);
        color: white;
        border-bottom-right-radius: var(--radius-sm);
      }

      p { margin: 0.5em 0; }
      p:first-child { margin-top: 0; }
      p:last-child { margin-bottom: 0; }

      h1, h2, h3, h4, h5, h6 {
        margin: 0.5em 0 0.25em;
        font-weight: 600;
        line-height: 1.3;
      }
      h1 { font-size: 1.2em; }
      h2 { font-size: 1.1em; }
      h3 { font-size: 1.05em; }

      code {
        font-family: "SF Mono", Monaco, "Cascadia Code", monospace;
        font-size: 0.9em;
        padding: 0.15em 0.4em;
        border-radius: 3px;
        background: var(--color-surface-secondary);
      }

      pre {
        margin: 0.5em 0;
        padding: var(--spacing-sm);
        border-radius: var(--radius-sm);
        background: var(--color-background);
        overflow-x: auto;
        
        code { padding: 0; background: none; }
      }

      blockquote {
        margin: 0.5em 0;
        padding-left: 1em;
        border-left: 3px solid rgba(0, 122, 255, 0.3);
        color: var(--color-text-secondary);
      }

      table {
        width: 100%;
        border-collapse: collapse;
        margin: 0.5em 0;
        font-size: 0.9em;
      }

      th, td {
        border: 1px solid rgba(0, 0, 0, 0.08);
        padding: 0.5em;
        text-align: left;
      }

      th { background: var(--color-surface-secondary); font-weight: 600; }

      ul, ol { margin: 0.5em 0; padding-left: 1.5em; }
      li { margin: 0.25em 0; }
    }

    .tool-calls {
      margin-top: var(--spacing-sm);
      width: 100%;
      max-width: 500px;
    }

    .message-meta {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      margin-top: 4px;
      padding: 0 4px;
    }

    .message-time {
      font-size: var(--font-size-xs);
      color: var(--color-text-tertiary);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatMessageComponent {
  message = input.required<ChatMessageData>();

  constructor(private sanitizer: DomSanitizer) {}

  isUser = computed(() => this.message().role === 'user');

  formattedTime = computed(() => {
    return new Date(this.message().timestamp).toLocaleTimeString();
  });

  renderedContent = computed(() => {
    const content = this.message().content;
    if (!content) return '';

    const trimmed = content.trim();
    
    // Check if it's raw JSON
    const isRawJson = (trimmed.startsWith('[') || trimmed.startsWith('{')) &&
                      !trimmed.startsWith('```') &&
                      !trimmed.startsWith('#');

    if (isRawJson) {
      try {
        const parsed = JSON.parse(trimmed);
        return this.sanitizer.bypassSecurityTrustHtml(
          this.highlightJson(JSON.stringify(parsed, null, 2))
        );
      } catch {
        // Not valid JSON, fall through
      }
    }

    // Process markdown-like content
    return this.sanitizer.bypassSecurityTrustHtml(this.processContent(content));
  });

  private processContent(content: string): string {
    let html = this.escapeHtml(content);
    
    // Code blocks
    html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
      return `<pre class="code-block"><code>${code.trim()}</code></pre>`;
    });

    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Headers
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

    // Bold and italic
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');

    // Links
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');

    // Line breaks (but not inside code blocks)
    html = html.replace(/\n\n/g, '</p><p>');
    html = html.replace(/\n/g, '<br>');

    return `<p>${html}</p>`;
  }

  private highlightJson(json: string): string {
    return json
      .replace(/"([^"]+)":/g, '<span class="json-key">"$1"</span>:')
      .replace(/: "([^"]*)"/g, ': <span class="json-string">"$1"</span>')
      .replace(/: (\d+\.?\d*)/g, ': <span class="json-number">$1</span>')
      .replace(/: (true|false)/g, ': <span class="json-boolean">$1</span>')
      .replace(/: (null)/g, ': <span class="json-null">$1</span>');
  }

  private escapeHtml(text: string): string {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }
}
