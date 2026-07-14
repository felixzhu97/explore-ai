import { describe, expect, it } from 'vitest';
import {
  parseA2uiNdjson,
  remapSurfaceIds,
  splitMarkdownAndA2ui,
  stableSurfaceId,
} from './a2ui-fence';

describe('a2ui-fence', () => {
  describe('parseA2uiNdjson', () => {
    it('should_parse_valid_ndjson_lines_and_skip_invalid', () => {
      const raw = [
        '{"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}',
        'not-json',
        '// comment',
        '{"version":"v0.9.1","updateComponents":{"surfaceId":"s1","components":[]}}',
      ].join('\n');

      const messages = parseA2uiNdjson(raw);

      expect(messages).toHaveLength(2);
      expect(messages[0]).toMatchObject({ version: 'v0.9' });
      expect(messages[1]).toMatchObject({ version: 'v0.9' });
    });
  });

  describe('splitMarkdownAndA2ui', () => {
    it('should_split_markdown_and_closed_a2ui_fence', () => {
      const content = [
        'Here is a chart:',
        '',
        '```a2ui',
        '{"version":"v0.9","createSurface":{"surfaceId":"chart-1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}',
        '```',
        '',
        'Done.',
      ].join('\n');

      const segments = splitMarkdownAndA2ui(content);

      expect(segments.map(s => s.type)).toEqual(['markdown', 'a2ui', 'markdown']);
      if (segments[1]?.type === 'a2ui') {
        expect(segments[1].messages).toHaveLength(1);
        expect(segments[1].surfaceId).toBe(stableSurfaceId(segments[1].raw));
      }
    });

    it('should_emit_pending_when_a2ui_fence_is_unclosed', () => {
      const content = 'Intro\n\n```a2ui\n{"version":"v0.9","createSurface":{';

      const segments = splitMarkdownAndA2ui(content);

      expect(segments.map(s => s.type)).toEqual(['markdown', 'a2ui-pending']);
    });

    it('should_return_markdown_only_when_no_fence', () => {
      expect(splitMarkdownAndA2ui('plain text')).toEqual([
        { type: 'markdown', content: 'plain text' },
      ]);
    });
  });

  describe('remapSurfaceIds', () => {
    it('should_rewrite_surfaceId_on_known_message_types', () => {
      const messages = parseA2uiNdjson(
        [
          '{"version":"v0.9","createSurface":{"surfaceId":"old","catalogId":"c"}}',
          '{"version":"v0.9","updateComponents":{"surfaceId":"old","components":[]}}',
          '{"version":"v0.9","updateDataModel":{"surfaceId":"old","path":"/","value":{}}}',
          '{"version":"v0.9","deleteSurface":{"surfaceId":"old"}}',
        ].join('\n'),
      );

      const remapped = remapSurfaceIds(messages, 'a2ui-fixed');

      expect(remapped).toHaveLength(4);
      for (const message of remapped) {
        const payload =
          ('createSurface' in message && message.createSurface)
          || ('updateComponents' in message && message.updateComponents)
          || ('updateDataModel' in message && message.updateDataModel)
          || ('deleteSurface' in message && message.deleteSurface);
        expect(payload).toMatchObject({ surfaceId: 'a2ui-fixed' });
      }
    });
  });
});
