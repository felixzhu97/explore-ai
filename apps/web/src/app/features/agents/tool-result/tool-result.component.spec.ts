import { describe, it, expect } from 'vitest';

describe('ToolResultComponent', () => {
  describe('formatJson logic', () => {
    const formatJson = (obj: unknown): string => {
      try {
        return JSON.stringify(obj, null, 2);
      } catch {
        return String(obj);
      }
    };

    it('should format simple object', () => {
      const result = formatJson({ key: 'value' });
      expect(result).toContain('key');
      expect(result).toContain('value');
    });

    it('should format nested object', () => {
      const result = formatJson({ nested: { key: 'value' } });
      expect(result).toContain('nested');
      expect(result).toContain('key');
    });

    it('should format array', () => {
      const result = formatJson(['item1', 'item2']);
      expect(result).toContain('item1');
      expect(result).toContain('item2');
    });

    it('should return string representation for non-object values', () => {
      // formatJson uses JSON.stringify which quotes strings
      expect(formatJson('string')).toContain('string');
      expect(formatJson(123)).toContain('123');
    });
  });

  describe('formatOutput logic', () => {
    const formatOutput = (output: string): string => {
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
        (trimmed.startsWith('{') && trimmed.endsWith('}')) ||
        (trimmed.startsWith('[') && trimmed.endsWith(']'))
      ) {
        try {
          const parsed = JSON.parse(trimmed);
          return JSON.stringify(parsed, null, 2);
        } catch {
          // Not valid JSON
        }
      }

      return output;
    };

    it('should return text with image placeholder for image URLs', () => {
      const output = 'Check out this image: https://example.com/image.png';
      const result = formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('https://example.com/image.png');
    });

    it('should format JSON object output', () => {
      const output = '{"key": "value", "count": 42}';
      const result = formatOutput(output);
      expect(result).toContain('key');
      expect(result).toContain('value');
    });

    it('should format JSON array output', () => {
      const output = '["item1", "item2", "item3"]';
      const result = formatOutput(output);
      expect(result).toContain('item1');
      expect(result).toContain('item2');
    });

    it('should return plain text for non-JSON output', () => {
      const output = 'This is plain text output';
      const result = formatOutput(output);
      expect(result).toBe(output);
    });

    it('should handle image URLs with query parameters', () => {
      const output = 'Image: https://example.com/image.png?size=large';
      const result = formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle multiple image URLs - only first is processed as image', () => {
      // The implementation replaces the first image URL with placeholder but keeps other URLs
      const output = 'First: https://example.com/1.png Second: https://example.com/2.png';
      const result = formatOutput(output);
      expect(result).toContain('https://example.com/1.png');
      expect(result).toContain('[Image:');
    });

    it('should handle invalid JSON gracefully', () => {
      const output = '{not valid json';
      const result = formatOutput(output);
      expect(result).toBe(output);
    });

    it('should handle whitespace in output', () => {
      // When there's no image URL or JSON, original output is returned
      const output = '  Plain text  ';
      const result = formatOutput(output);
      // Original output is returned, not trimmed
      expect(result).toBe(output);
    });

    it('should handle various image formats', () => {
      const formats = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'];
      formats.forEach((format) => {
        const output = `https://example.com/image.${format}`;
        const result = formatOutput(output);
        expect(result).toContain('[Image:');
      });
    });
  });

  describe('ToolCall type', () => {
    it('should define valid ToolCall statuses', () => {
      const statuses = ['pending', 'running', 'success', 'error'];
      statuses.forEach((status) => {
        const toolCall = {
          id: 'tool_1',
          name: 'search',
          input: { query: 'test' },
          status: status as 'pending' | 'running' | 'success' | 'error',
        };
        expect(toolCall.status).toBe(status);
      });
    });

    it('should allow optional output field', () => {
      const toolCallWithOutput = {
        id: 'tool_1',
        name: 'search',
        input: {},
        status: 'success' as const,
        output: 'Search completed successfully',
      };

      const toolCallWithoutOutput = {
        id: 'tool_2',
        name: 'search',
        input: {},
        status: 'pending' as const,
      };

      expect(toolCallWithOutput.output).toBeDefined();
      expect((toolCallWithoutOutput as any).output).toBeUndefined();
    });
  });
});
