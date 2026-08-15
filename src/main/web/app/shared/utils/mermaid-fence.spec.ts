import { describe, expect, it } from 'vitest';
import {
  splitMarkdownAndMermaid,
  stableMermaidId,
} from './mermaid-fence';

describe('mermaid-fence', () => {
  describe('splitMarkdownAndMermaid', () => {
    it('should split markdown and closed mermaid fence', () => {
      const content = [
        'Here is a sequence:',
        '',
        '```mermaid',
        'sequenceDiagram',
        '  Alice->>Bob: Hello',
        '```',
        '',
        'Done.',
      ].join('\n');

      const segments = splitMarkdownAndMermaid(content);

      expect(segments.map(s => s.type)).toEqual(['markdown', 'mermaid', 'markdown']);
      if (segments[1]?.type === 'mermaid') {
        expect(segments[1].source).toContain('sequenceDiagram');
        expect(segments[1].diagramId).toBe(stableMermaidId(segments[1].source));
      }
    });

    it('should emit pending when mermaid fence is unclosed', () => {
      const content = 'Intro\n\n```mermaid\nsequenceDiagram\n  A->>B: hi';

      const segments = splitMarkdownAndMermaid(content);

      expect(segments.map(s => s.type)).toEqual(['markdown', 'mermaid-pending']);
    });

    it('should return markdown only when no fence', () => {
      expect(splitMarkdownAndMermaid('plain text')).toEqual([
        { type: 'markdown', content: 'plain text' },
      ]);
    });

    it('should leave typescript fences as markdown', () => {
      const content = [
        'Code:',
        '',
        '```typescript',
        'const x = 1;',
        '```',
      ].join('\n');

      const segments = splitMarkdownAndMermaid(content);

      expect(segments).toEqual([{ type: 'markdown', content }]);
    });

    it('should not close mermaid with a later typescript fence', () => {
      const content = [
        'Diagram:',
        '',
        '```mermaid',
        'classDiagram',
        '  class Foo',
        '```',
        '',
        'Code:',
        '',
        '```typescript',
        'const x = 1;',
        '```',
      ].join('\n');

      const segments = splitMarkdownAndMermaid(content);

      expect(segments.map(s => s.type)).toEqual(['markdown', 'mermaid', 'markdown']);
      const mdAfter = segments[2];
      expect(mdAfter?.type).toBe('markdown');
      if (mdAfter?.type === 'markdown') {
        expect(mdAfter.content).toContain('```typescript');
      }
    });

    it('should split multiple mermaid fences', () => {
      const content = [
        '```mermaid',
        'graph TD',
        '  A-->B',
        '```',
        '',
        'and',
        '',
        '```mermaid',
        'stateDiagram-v2',
        '  [*] --> Idle',
        '```',
      ].join('\n');

      const segments = splitMarkdownAndMermaid(content);

      expect(segments.map(s => s.type)).toEqual(['mermaid', 'markdown', 'mermaid']);
    });

    it('should return empty array for empty content', () => {
      expect(splitMarkdownAndMermaid('')).toEqual([]);
    });
  });

  describe('stableMermaidId', () => {
    it('should return stable id for same source', () => {
      expect(stableMermaidId('graph TD\nA-->B')).toBe(stableMermaidId('graph TD\nA-->B'));
    });
  });
});
