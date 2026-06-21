import { Injectable } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

@Injectable({ providedIn: 'root' })
export class MarkdownService {
  constructor(private sanitizer: DomSanitizer) {
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
}
