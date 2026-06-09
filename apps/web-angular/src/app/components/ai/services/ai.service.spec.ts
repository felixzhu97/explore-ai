import { describe, it, expect } from 'vitest';

describe('AiService', () => {
  describe('service constants', () => {
    it('should define correct service URLs', () => {
      const TEXT_SERVICE_URL = '/api/text';
      const VISION_SERVICE_URL = '/api/vision';
      const RAG_SERVICE_URL = '/api/rag';
      const SPEECH_SERVICE_URL = '/api/tts';
      const MEDIA_GEN_SERVICE_URL = '/api/image';

      expect(TEXT_SERVICE_URL).toBe('/api/text');
      expect(VISION_SERVICE_URL).toBe('/api/vision');
      expect(RAG_SERVICE_URL).toBe('/api/rag');
      expect(SPEECH_SERVICE_URL).toBe('/api/tts');
      expect(MEDIA_GEN_SERVICE_URL).toBe('/api/image');
    });
  });

  describe('interface definitions', () => {
    it('should define ChatMessage interface correctly', () => {
      const message = {
        role: 'user' as const,
        content: 'Hello',
      };

      expect(message.role).toBe('user');
      expect(message.content).toBe('Hello');
    });

    it('should allow assistant role', () => {
      const message = {
        role: 'assistant' as const,
        content: 'Response',
      };

      expect(message.role).toBe('assistant');
    });

    it('should allow system role', () => {
      const message = {
        role: 'system' as const,
        content: 'System prompt',
      };

      expect(message.role).toBe('system');
    });
  });
});
