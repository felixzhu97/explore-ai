import { describe, it, expect } from 'vitest';
import { parseSseToken, parseChatStreamEvent, SseEventAssembler } from './sse-client';

describe('sse-client', () => {
  describe('parseSseToken', () => {
    it('should_return_newline_when_data_is_empty', () => {
      expect(parseSseToken('')).toBe('\n');
    });

    it('should_parse_json_token_object', () => {
      expect(parseSseToken('{"token":"hello"}')).toBe('hello');
    });

    it('should_parse_typed_message_event', () => {
      expect(parseSseToken('{"type":"message","token":"hi"}')).toBe('hi');
    });

    it('should_return_null_for_tool_events', () => {
      expect(parseSseToken('{"type":"tool_call","name":"searchWeb","input":"{}"}')).toBeNull();
    });

    it('should_parse_quoted_json_string', () => {
      expect(parseSseToken('"hello"')).toBe('hello');
    });

    it('should_return_plain_text_when_not_json', () => {
      expect(parseSseToken('hello')).toBe('hello');
    });
  });

  describe('parseChatStreamEvent', () => {
    it('should_parse_sources_event', () => {
      const event = parseChatStreamEvent(
        '{"type":"sources","query":"q","items":[{"title":"T","url":"https://a.com","snippet":"s"}]}',
      );
      expect(event).toEqual({
        type: 'sources',
        query: 'q',
        items: [{ title: 'T', url: 'https://a.com', snippet: 's' }],
      });
    });

    it('should_parse_tool_call_and_result', () => {
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
    it('should_assemble_event_on_blank_line', () => {
      const assembler = new SseEventAssembler();
      assembler.pushLine('event: message');
      assembler.pushLine('data: hello');
      const event = assembler.pushLine('');

      expect(event).toEqual({ eventType: 'message', data: 'hello' });
    });

    it('should_join_multiple_data_lines', () => {
      const assembler = new SseEventAssembler();
      assembler.pushLine('data: line1');
      assembler.pushLine('data: line2');
      const event = assembler.flush();

      expect(event).toEqual({ eventType: '', data: 'line1\nline2' });
    });
  });
});
