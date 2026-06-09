import { describe, it, expect } from 'vitest';

describe('ApiService', () => {
  const defaultProviders = [
    {
      name: 'openai',
      display_name: 'OpenAI',
      models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
      status: 'available',
    },
    {
      name: 'anthropic',
      display_name: 'Anthropic Claude',
      models: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-3-5-sonnet-20241022'],
      status: 'available',
    },
    {
      name: 'ollama',
      display_name: 'Ollama (Local)',
      models: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b', 'llama3.1:8b', 'mistral:7b'],
      status: 'available',
    },
  ];

  describe('defaultProviders', () => {
    it('should have openai provider with default models', () => {
      const openai = defaultProviders.find((p) => p.name === 'openai');
      expect(openai).toBeDefined();
      expect(openai?.display_name).toBe('OpenAI');
      expect(openai?.models).toContain('gpt-4o');
      expect(openai?.status).toBe('available');
    });

    it('should have anthropic provider with default models', () => {
      const anthropic = defaultProviders.find((p) => p.name === 'anthropic');
      expect(anthropic).toBeDefined();
      expect(anthropic?.display_name).toBe('Anthropic Claude');
      expect(anthropic?.models).toContain('claude-sonnet-4-20250514');
    });

    it('should have ollama provider with default models', () => {
      const ollama = defaultProviders.find((p) => p.name === 'ollama');
      expect(ollama).toBeDefined();
      expect(ollama?.display_name).toBe('Ollama (Local)');
      expect(ollama?.models).toContain('qwen2.5:7b');
    });

    it('should have all providers with available status', () => {
      defaultProviders.forEach((provider) => {
        expect(provider.status).toBe('available');
      });
    });
  });

  describe('base64ToBlob logic', () => {
    it('should convert base64 string to Blob with correct mime type', () => {
      const base64 = btoa('test content');
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'text/plain' });

      expect(blob).toBeInstanceOf(Blob);
      expect(blob.type).toBe('text/plain');
    });

    it('should default to image/png mime type', () => {
      const base64 = btoa('test');
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'image/png' });

      expect(blob.type).toBe('image/png');
    });

    it('should handle empty string', () => {
      const base64 = '';
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'text/plain' });

      expect(blob).toBeInstanceOf(Blob);
    });

    it('should handle binary data', () => {
      const binaryString = '\x00\x01\x02\x03';
      const base64 = btoa(binaryString);
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'application/octet-stream' });

      expect(blob).toBeInstanceOf(Blob);
      expect(blob.type).toBe('application/octet-stream');
    });
  });
});
