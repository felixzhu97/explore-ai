import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ToolCall } from '@features/agents/tool-result/tool-result.component';
import { ToolResultComponent } from '@features/agents/tool-result/tool-result.component';
import type { ChatMessageData } from '@shared/models';
export type { ChatMessageData };

@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [ToolResultComponent],
  templateUrl: './chat-message.component.html',
  styleUrl: './chat-message.component.scss',
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
    const isRawJson =
      (trimmed.startsWith('[') || trimmed.startsWith('{')) &&
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
    html = html.replace(
      /\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener">$1</a>'
    );

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
      "'": '&#039;',
    };
    return text.replace(/[&<>"']/g, (m) => map[m]);
  }
}
