import { Injectable, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

const HR_PLACEHOLDER = '\uE000HR\uE001';
const CODE_PLACEHOLDER_PREFIX = '\uE002CODE';
const CODE_PLACEHOLDER_SUFFIX = '\uE003';

@Injectable({ providedIn: 'root' })
export class MarkdownService {
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    marked.setOptions({
      gfm: true,
      breaks: true,
    });
  }

  render(content: string, streaming = false): SafeHtml {
    if (!content) return '';

    const source = this.prepareSource(content, streaming);
    const html = marked.parse(source) as string;
    const cleanHtml = DOMPurify.sanitize(html, {
      ADD_TAGS: ['pre', 'code'],
      ADD_ATTR: ['class'],
    });

    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml);
  }

  /** @deprecated Use {@link render} with streaming=true */
  renderStreaming(content: string): SafeHtml {
    return this.render(content, true);
  }

  renderToString(content: string): string {
    if (!content) return '';

    const source = this.prepareSource(content, false);
    const html = marked.parse(source) as string;
    return DOMPurify.sanitize(html, {
      ADD_TAGS: ['pre', 'code'],
      ADD_ATTR: ['class'],
    });
  }

  /**
   * Repair GFM token spacing (e.g. "-item" → "- item") per list/heading rules.
   * Mirrors backend GfmSyntaxNormalizer.
   */
  normalizeGfmSyntax(content: string): string {
    let normalized = content;

    const codeBlocks: string[] = [];
    normalized = normalized.replace(/```[\s\S]*?```|~~~[\s\S]*?~~~/g, (match) => {
      codeBlocks.push(match);
      return `${CODE_PLACEHOLDER_PREFIX}${codeBlocks.length - 1}${CODE_PLACEHOLDER_SUFFIX}`;
    });

    const horizontalRules: string[] = [];
    normalized = normalized.replace(/^([-*_]){3,}\s*$/gm, (match) => {
      horizontalRules.push(match);
      return `${HR_PLACEHOLDER}${horizontalRules.length - 1}`;
    });

    normalized = normalized.replace(/^(#{1,6})([^\s#\n])/gm, '$1 $2');
    normalized = normalized.replace(/^(\d+\.)([^\s\n])/gm, '$1 $2');
    normalized = normalized.replace(/([：。！？；:])(#{1,6}\s)/g, '$1\n$2');
    normalized = normalized.replace(/([：。！？；:])-(?!-)(?=[^\s\n0-9a-zA-Z])/g, '$1\n- ');
    normalized = normalized.replace(/([：。！？；:])(\d+\.)(?=[^\s\n])/g, '$1\n$2 ');
    normalized = normalized.replace(
      /([一二三四五六七八九十]+、[^\n-]+)(-(?!-)(?=[^\s\n0-9a-zA-Z]))/g,
      '$1\n$2',
    );
    normalized = this.promoteOutlineSectionHeadings(normalized);
    normalized = normalized.replace(/^-(?!-)(?=[^\s\n0-9a-zA-Z])/gm, '- ');
    normalized = normalized.replace(/^\*(?!\*)(?=[^\s\n0-9a-zA-Z])/gm, '* ');
    normalized = normalized.replace(/^\+(?=[^\s\n0-9a-zA-Z])/gm, '+ ');

    normalized = normalized.replace(
      new RegExp(`${HR_PLACEHOLDER}(\\d+)`, 'g'),
      (_, index) => horizontalRules[Number(index)] ?? '',
    );

    normalized = normalized.replace(
      new RegExp(`${CODE_PLACEHOLDER_PREFIX}(\\d+)${CODE_PLACEHOLDER_SUFFIX}`, 'g'),
      (_, index) => codeBlocks[Number(index)] ?? '',
    );

    return normalized;
  }

  /** Outline sections written as "一、…" on their own line → GFM ## headings. */
  private promoteOutlineSectionHeadings(content: string): string {
    return content.replace(
      /(^|\n)([一二三四五六七八九十]+、[^\n]+)(?=\n|$)/g,
      '$1## $2',
    );
  }

  private prepareSource(content: string, streaming: boolean): string {
    let source = this.normalizeGfmSyntax(content);
    if (streaming && !source.endsWith('\n')) {
      source += '\n';
    }
    return source;
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
