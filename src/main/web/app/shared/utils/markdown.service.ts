import { Injectable, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

@Injectable({ providedIn: 'root' })
export class MarkdownService {
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    marked.setOptions({
      gfm: true,
      breaks: true,
    });
  }

  render(content: string): SafeHtml {
    if (!content) return '';

    const html = marked.parse(content) as string;
    const cleanHtml = DOMPurify.sanitize(html, {
      ADD_TAGS: ['pre', 'code'],
      ADD_ATTR: ['class'],
    });

    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml || content);
  }

  renderToString(content: string): string {
    if (!content) return '';

    const html = marked.parse(content) as string;
    return DOMPurify.sanitize(html, {
      ADD_TAGS: ['pre', 'code'],
      ADD_ATTR: ['class'],
    }) || content;
  }

  processContent(content: string): SafeHtml {
    const processed = this.escapeHtml(content);

    const html = processed
      .replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
        return `<pre class="code-block"><code>${code.trim()}</code></pre>`;
      })
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/\n/g, '<br>');

    return this.sanitizer.bypassSecurityTrustHtml(`<p>${html}</p>`);
  }

  highlightJson(json: string): string {
    return json
      .replace(/"([^"]+)":/g, '<span class="json-key">"$1"</span>:')
      .replace(/: "([^"]*)"/g, ': <span class="json-string">"$1"</span>')
      .replace(/: (\d+\.?\d*)/g, ': <span class="json-number">$1</span>')
      .replace(/: (true|false)/g, ': <span class="json-boolean">$1</span>')
      .replace(/: (null)/g, ': <span class="json-null">$1</span>');
  }

  escapeHtml(text: string): string {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      '\'': '&#039;',
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }

  isRawJson(content: string): boolean {
    const trimmed = content.trim();
    return (
      (trimmed.startsWith('[') || trimmed.startsWith('{'))
      && !trimmed.startsWith('```')
      && !trimmed.startsWith('#')
    );
  }
}
