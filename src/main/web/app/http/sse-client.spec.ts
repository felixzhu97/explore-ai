import { describe, it, expect } from 'vitest';
import { parseSseToken, parseChatStreamEvent, SseEventAssembler } from './sse-client';

describe('sse-client', () => {
  describe('parseSseToken', () => {
    it('should return newline when data is empty', () => {
      expect(parseSseToken('')).toBe('\n');
    });

    it('should ignore json object without event type', () => {
      expect(parseSseToken('{"token":"hello"}')).toBeNull();
    });

    it('should parse typed message event', () => {
      expect(parseSseToken('{"type":"message","token":"hi"}')).toBe('hi');
    });

    it('should return null for tool events', () => {
      expect(parseSseToken('{"type":"tool_call","name":"searchWeb","input":"{}"}')).toBeNull();
    });

    it('should parse quoted json string', () => {
      expect(parseSseToken('"hello"')).toBe('hello');
    });

    it('should return plain text when not json', () => {
      expect(parseSseToken('hello')).toBe('hello');
    });
  });

  describe('parseChatStreamEvent', () => {
    it('should parse sources event', () => {
      const event = parseChatStreamEvent(
        '{"type":"sources","query":"q","items":[{"title":"T","url":"https://a.com","snippet":"s"}]}',
      );
      expect(event).toEqual({
        type: 'sources',
        query: 'q',
        items: [{ title: 'T', url: 'https://a.com', snippet: 's', publishedAt: null }],
      });
    });

    it('should keep published at and skip malformed source items', () => {
      const event = parseChatStreamEvent(
        '{"type":"sources","query":"q","items":['
        + '{"title":"T","url":"u","snippet":"s","publishedAt":" Jul 19, 2026 "},'
        + '"bad",null]}',
      );
      expect(event).toEqual({
        type: 'sources',
        query: 'q',
        items: [{ title: 'T', url: 'u', snippet: 's', publishedAt: 'Jul 19, 2026' }],
      });
    });

    it('should return null for unknown event type', () => {
      expect(parseChatStreamEvent('{"type":"usage","tokens":3}')).toBeNull();
    });

    it('should parse tool call and result', () => {
      expect(parseChatStreamEvent('{"type":"tool_call","name":"searchWeb","input":"{}"}')).toEqual({
        type: 'tool_call',
        name: 'searchWeb',
        input: '{}',
      });
      expect(parseChatStreamEvent('{"type":"tool_result","name":"searchWeb","ok":true,"output":"done"}')).toEqual({
        type: 'tool_result',
        name: 'searchWeb',
        ok: true,
        output: 'done',
      });
    });
  });

  describe('SseEventAssembler', () => {
    it('should assemble event on blank line', () => {
      const assembler = new SseEventAssembler();
      assembler.pushLine('event: message');
      assembler.pushLine('data: hello');
      const event = assembler.pushLine('');

      expect(event).toEqual({ eventType: 'message', data: 'hello' });
    });

    it('should join multiple data lines', () => {
      const assembler = new SseEventAssembler();
      assembler.pushLine('data: line1');
      assembler.pushLine('data: line2');
      const event = assembler.flush();

      expect(event).toEqual({ eventType: '', data: 'line1\nline2' });
    });
  });
});
