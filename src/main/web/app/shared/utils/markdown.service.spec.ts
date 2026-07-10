import { TestBed } from '@angular/core/testing';
import { MarkdownService } from './markdown.service';

describe('MarkdownService', () => {
  let service: MarkdownService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarkdownService);
  });

  describe('normalizeGfmSyntax', () => {
    it('should insert space after list marker when missing', () => {
      expect(service.normalizeGfmSyntax('-**Label:** text')).toBe('- **Label:** text');
    });

    it('should insert space after heading marker when missing', () => {
      expect(service.normalizeGfmSyntax('##Section')).toBe('## Section');
    });

    it('should not break numeric ranges', () => {
      expect(service.normalizeGfmSyntax('1819-1942')).toBe('1819-1942');
    });

    it('should not modify fenced code blocks', () => {
      const input = '```\n#define MAX\n-1\n-verbose\n```';
      expect(service.normalizeGfmSyntax(input)).toBe(input);
    });

    it('should preserve italic markers at line start', () => {
      expect(service.normalizeGfmSyntax('*italic* text')).toBe('*italic* text');
    });

    it('should preserve negative numbers at line start', () => {
      expect(service.normalizeGfmSyntax('-1 is negative')).toBe('-1 is negative');
    });

    it('should promote outline headings containing hyphens', () => {
      expect(service.normalizeGfmSyntax('一、Section - Heading')).toContain('## 一、Section - Heading');
    });
  });

  describe('render', () => {
    it('should render GFM list after syntax repair', () => {
      const html = service.renderToString('-**Label:** description');
      expect(html).toContain('<ul>');
      expect(html).toContain('<li>');
      expect(html).not.toContain('-**Label');
    });

    it('should render GFM heading after syntax repair', () => {
      const html = service.renderToString('#Title\n\nBody');
      expect(html).toContain('<h1');
      expect(html).toContain('Title');
    });

    it('should render outline section headings during streaming', () => {
      const html = service.render(
        '一、古代至马六甲王朝（约公元1世纪—1511年）\n- **早期文明：**内容',
        true,
      );
      const text = service.renderToString(
        '一、古代至马六甲王朝（约公元1世纪—1511年）\n- **早期文明：**内容',
      );
      expect(text).toContain('<h2');
      expect(text).toContain('一、古代至马六甲王朝');
      expect(html).toBeTruthy();
    });

    it('should not fall back to raw content when sanitization returns empty string', () => {
      const malicious = '<img src=x onerror=alert(1)>';
      const html = service.renderToString(malicious);
      expect(html).not.toContain('onerror');
      expect(html).not.toBe(malicious);
    });
  });
});
