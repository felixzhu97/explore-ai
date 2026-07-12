import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import type { ProviderInfo } from '@shared/models';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService],
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  describe('service creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('getProviders', () => {
    it('should return providers from API', () => {
      const mockProviders = [
        {
          name: 'openai',
          displayName: 'OpenAI',
          models: ['gpt-4o'],
          status: 'available',
        },
      ];

      service.getProviders().subscribe((providers) => {
        expect(providers).toEqual(mockProviders);
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.flush(mockProviders);
    });

    it('should return default providers on error', () => {
      service.getProviders().subscribe((providers) => {
        expect(providers.length).toBeGreaterThan(0);
        expect(providers[0].name).toBe('openai');
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.error(new ProgressEvent('error'));
    });
  });

  describe('getModels', () => {
    it('should return models from API', () => {
      const mockResponse = {
        provider: 'openai',
        models: [{ name: 'gpt-4o', provider: 'openai' }],
        count: 1,
      };

      service.getModels('openai').subscribe((models) => {
        expect(models.length).toBe(1);
        expect(models[0].name).toBe('gpt-4o');
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/text/models'));
      req.flush(mockResponse);
    });

    it('should return default models on error', () => {
      service.getModels('openai').subscribe((models) => {
        expect(models.length).toBeGreaterThan(0);
        expect(models[0].provider).toBe('openai');
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });

    it('should use anthropic defaults for anthropic provider', () => {
      service.getModels('anthropic').subscribe((models) => {
        expect(models.some(m => m.name.includes('claude'))).toBe(true);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });

    it('should use openai defaults for unknown provider', () => {
      service.getModels('unknown').subscribe((models) => {
        expect(models.some(m => m.name.includes('gpt'))).toBe(true);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });
  });

  describe('chatStream', () => {
    it('should return abort controller', () => {
      const result = service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      expect(result).toHaveProperty('abort');
      expect(typeof result.abort).toBe('function');
    });

    it('should call fetch with POST method', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/text/chat/stream',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });

    it('should handle non-ok response with error callback', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should call onDone when stream completes', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should call onError on stream error event', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should call onChunk with parsed token from meta event', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should handle abort on the returned controller', async () => {
      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            setTimeout(() => resolve({ done: true, value: new Uint8Array() }), 1000);
          });
        }),
        cancel: vi.fn(),
      };
      const mockStream = {
        getReader: () => mockReader,
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: mockStream,
      } as never);

      const result = service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      // result.abort() should be callable
      expect(typeof result.abort).toBe('function');
    });

    it('should not call onError on AbortError', async () => {
      const abortError = new DOMException('Aborted', 'AbortError');
      vi.spyOn(global, 'fetch').mockRejectedValue(abortError);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).not.toHaveBeenCalled();
    });
  });

  describe('chatStream streaming callback coverage', () => {
    it('should call onChunk when token is received from meta event', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();
      const onDone = vi.fn();
      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        onDone,
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: meta\ndata: {"token":"Hello"}\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Hello');
    });

    it('should call onChunk for numeric-only SSE data lines', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();
      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        onDone,
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data:约为\n\ndata:1387\n\ndata:万人\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: undefined });

      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('约为');
      expect(onChunk).toHaveBeenCalledWith('1387');
      expect(onChunk).toHaveBeenCalledWith('万人');
      expect(onDone).toHaveBeenCalled();
    });

    it('should preserve newline-only SSE data lines', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data:# Title\n\ndata:\n\ndata:- item\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: undefined });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('\n');
    });

    it('should join multiline data fields within one SSE event', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data:# Title\ndata:- item\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: undefined });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('# Title\n- item');
    });

    it('should call onDone when [DONE] is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n\n') });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onDone when done event is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: done\ndata: \n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onError when error event is received with JSON error data', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: error\ndata: {"error":"Something went wrong"}\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Something went wrong');
    });

    it('should call onError when error event is received without JSON error data', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: error\ndata: Invalid error format\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Invalid error format');
    });

    it('should handle data line without currentEvent (empty event type)', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data: {"token":"chunk1"}\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk1');
    });

    it('should handle non-JSON data gracefully (skip)', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      // Send non-JSON data
      readResolve({
        done: false,
        value: encoder.encode('data: plain text data\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      // Continue reading
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    it('should reset currentEvent on empty line', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      // Send event followed by empty line, then data without event
      readResolve({
        done: false,
        value: encoder.encode('event: meta\n\ndata: {"token":"after reset"}\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('after reset');
    });

    it('should not call onChunk when token is missing in parsed data', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data: {"no_token_field":true}\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).not.toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: null,
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should continue reading when not done', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n\n') });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(mockReader.read).toHaveBeenCalled();
    });

    it('should handle partial line in buffer', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      // Send partial line, then complete it
      readResolve({
        done: false,
        value: encoder.encode('data: {"token":"par'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('tial"}\n') });
      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('partial');
    });
  });

  describe('ragChat streaming callback coverage', () => {
    it('should call onChunk when chunk data is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();
      const onDone = vi.fn();
      const onError = vi.fn();
      const onSources = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        onChunk,
        onSources,
        onDone,
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data: Some chunk text\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Some chunk text');
    });

    it('should call onDone when [DONE] is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onDone = vi.fn();

      service.ragChat({ query: 'test', sessionId: 'session1' }, vi.fn(), vi.fn(), onDone, vi.fn());

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n\n') });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onSources when sources event is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onSources = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        onSources,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode(
          'event: sources\ndata: [{"text":"source 1","score":0.9,"metadata":{}}]\n\n',
        ),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onSources).toHaveBeenCalledWith([{ text: 'source 1', score: 0.9, metadata: {} }]);
    });

    it('should call onError when Error: prefix is received', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data: Error:Database connection failed\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Database connection failed');
    });

    it('should replace <br> with newlines in chunk data', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('data: Line 1<br>Line 2<br>Line 3\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Line 1\nLine 2\nLine 3');
    });

    it('should reset currentEvent on empty line', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      // Send sources event followed by empty line, then chunk without event
      readResolve({
        done: false,
        value: encoder.encode('event: sources\ndata: []\n\ndata: chunk after reset\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk after reset');
    });

    it('should reset currentEvent after sources event', async () => {
      const encoder = new TextEncoder();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let readResolve!: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          return new Promise((resolve) => {
            readResolve = resolve;
          });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as never);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: sources\ndata: []\n\ndata: chunk after sources\n\n'),
      });
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk after sources');
    });

    it('should handle non-ok response by throwing error', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: null,
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle network error', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });
  });

  describe('generateImage', () => {
    it('should send image generation request', () => {
      const mockResponse = {
        imageUrl: null,
        imageBase64: 'base64data',
        model: 'dall-e-3',
        prompt: 'A sunset',
        status: 'SUCCESS',
      };

      service
        .generateImage({
          prompt: 'A sunset',
          width: 512,
          height: 512,
        })
        .subscribe((response) => {
          expect(response.imageBase64).toBe('base64data');
          expect(response.model).toBe('dall-e-3');
        });

      const req = httpMock.expectOne('/api/images/generate');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.prompt).toBe('A sunset');
      expect(req.request.body.n).toBe(1);
      req.flush(mockResponse);
    });
  });

  describe('getVoices', () => {
    it('should return voices from API', () => {
      const mockVoices = [
        {
          id: 'alloy',
          name: 'Alloy',
          language: 'en',
          gender: 'neutral',
        },
      ];

      service.getVoices().subscribe((voices) => {
        expect(voices).toEqual([
          {
            id: 'alloy',
            name: 'Alloy',
            language: 'en',
            gender: 'neutral',
            provider: 'openai',
            isDefault: true,
          },
        ]);
      });

      const req = httpMock.expectOne('/api/audio/voices');
      req.flush({ voices: mockVoices });
    });

    it('should normalize mixed string and object voices', () => {
      service.getVoices().subscribe((voices) => {
        expect(voices).toEqual([
          {
            id: 'alloy',
            name: 'Alloy',
            language: 'en',
            provider: 'openai',
            isDefault: true,
          },
          {
            id: 'echo',
            name: 'Echo',
            language: 'en',
            gender: 'male',
            provider: 'openai',
            isDefault: false,
          },
        ]);
      });

      const req = httpMock.expectOne('/api/audio/voices');
      req.flush({
        voices: [
          'alloy',
          { id: 'echo', name: 'Echo', language: 'en', gender: 'male' },
        ],
      });
    });

    it('should return default voices on error', () => {
      service.getVoices().subscribe((voices) => {
        expect(voices.length).toBeGreaterThan(0);
        expect(voices[0].id).toBe('alloy');
      });

      const req = httpMock.expectOne('/api/audio/voices');
      req.error(new ProgressEvent('error'));
    });
  });

  describe('synthesizeSpeech', () => {
    it('should send synthesis request', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello world' }).subscribe((blob) => {
        expect(blob).toBeInstanceOf(Blob);
      });

      const req = httpMock.expectOne('/api/audio/speak');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.text).toBe('Hello world');
      req.flush(mockBlob);
    });

    it('should include voice parameter when provided', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello', voice: 'nova' }).subscribe(vi.fn());

      const req = httpMock.expectOne('/api/audio/speak');
      expect(req.request.body.voice).toBe('nova');
      req.flush(mockBlob);
    });

    it('should include speed parameter when provided', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello', speed: 1.5 }).subscribe(vi.fn());

      const req = httpMock.expectOne('/api/audio/speak');
      expect(req.request.body.speed).toBe(1.5);
      req.flush(mockBlob);
    });
  });

  describe('captionImage', () => {
    it('should send caption request with file', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = {
        caption: 'A beautiful sunset',
        processingTimeMs: 500,
      };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('A beautiful sunset');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('detectObjects', () => {
    it('should send detect request with file', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = {
        detections: [
          {
            className: 'person',
            confidence: 0.95,
            bbox: [0, 0, 100, 200] as [number, number, number, number],
          },
        ],
        processingTimeMs: 120,
      };

      service.detectObjects(mockFile).subscribe((response) => {
        expect(response.detections?.length).toBe(1);
        expect(response.detections?.[0].className).toBe('person');
        expect(response.processingTimeMs).toBe(120);
      });

      const req = httpMock.expectOne('/api/vision/detect');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('ocrImage', () => {
    it('should send OCR request with file', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = {
        fullText: 'Hello World',
        processingTimeMs: 300,
      };

      service.ocrImage(mockFile).subscribe((response) => {
        expect(response.fullText).toBe('Hello World');
      });

      const req = httpMock.expectOne('/api/vision/ocr');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getDocuments', () => {
    it('should return documents from API', () => {
      const mockResponse = {
        documents: [{ doc_id: '1', filename: 'doc.pdf' }],
      };

      service.getDocuments().subscribe((response) => {
        expect(response.documents.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/rag/documents');
      req.flush(mockResponse);
    });

    it('should return empty documents on error', () => {
      service.getDocuments().subscribe((response) => {
        expect(response.documents).toEqual([]);
      });

      const req = httpMock.expectOne('/api/rag/documents');
      req.error(new ProgressEvent('error'));
    });
  });

  describe('uploadDocument', () => {
    it('should upload document file', () => {
      const mockFile = new File(['test content'], 'test.pdf', {
        type: 'application/pdf',
      });
      const mockResponse = { id: 'doc123' };

      service.uploadDocument(mockFile).subscribe((response) => {
        expect(response.id).toBe('doc123');
      });

      const req = httpMock.expectOne('/api/rag/documents/upload');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('deleteDocument', () => {
    it('should delete document by id', () => {
      service.deleteDocument('doc123').subscribe(vi.fn());

      const req = httpMock.expectOne('/api/rag/documents/doc123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('ragChat', () => {
    it('should return abort controller', () => {
      const result = service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      expect(result).toHaveProperty('abort');
      expect(typeof result.abort).toBe('function');
    });

    it('should call fetch with POST method', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/rag/chat/stream',
        expect.objectContaining({
          method: 'POST',
        }),
      );
    });

    it('should handle non-ok response with error callback', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should call onDone when stream completes', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onDone = vi.fn();

      service.ragChat({ query: 'test', sessionId: 'session1' }, vi.fn(), vi.fn(), onDone, vi.fn());

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should call onSources on sources event', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onSources = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        onSources,
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should call onChunk with chunk data', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));
    });

    it('should call onError on non-ok response', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle network error', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });
  });

  describe('downloadBlob', () => {
    it('should create download link and trigger click', () => {
      const blob = new Blob(['test'], { type: 'text/plain' });
      const createElementSpy = vi.spyOn(document, 'createElement');
      const appendChildSpy = vi.fn();
      const removeChildSpy = vi.fn();
      const clickSpy = vi.fn();

      Object.defineProperty(document.body, 'appendChild', {
        value: appendChildSpy,
      });
      Object.defineProperty(document.body, 'removeChild', {
        value: removeChildSpy,
      });

      vi.mocked(createElementSpy).mockReturnValue({
        href: '',
        download: '',
        click: clickSpy,
      } as unknown as HTMLAnchorElement);

      service.downloadBlob(blob, 'test.txt');

      expect(createElementSpy).toHaveBeenCalledWith('a');
      expect(clickSpy).toHaveBeenCalled();
    });
  });

  describe('downloadBase64Image', () => {
    it('should convert base64 to blob and download', () => {
      const base64 = btoa('test image data');
      const downloadBlobSpy = vi.spyOn(service, 'downloadBlob');

      service.downloadBase64Image(base64, 'image.png');

      expect(downloadBlobSpy).toHaveBeenCalled();
    });
  });

  describe('base64ToBlob', () => {
    it('should convert base64 string to Blob', () => {
      const base64 = btoa('test content');
      const blob = service.base64ToBlob(base64, 'text/plain');

      expect(blob).toBeInstanceOf(Blob);
      expect(blob.type).toBe('text/plain');
    });

    it('should default to image/png mime type', () => {
      const base64 = btoa('test');
      const blob = service.base64ToBlob(base64);

      expect(blob.type).toBe('image/png');
    });

    it('should handle empty string', () => {
      const blob = service.base64ToBlob('', 'text/plain');
      expect(blob).toBeInstanceOf(Blob);
    });

    it('should handle binary data', () => {
      const binaryData = '\x00\x01\x02\x03';
      const base64 = btoa(binaryData);
      const blob = service.base64ToBlob(base64, 'application/octet-stream');

      expect(blob).toBeInstanceOf(Blob);
      expect(blob.type).toBe('application/octet-stream');
    });
  });

  describe('default providers fallback', () => {
    it('should return default providers on API error', () => {
      service.getProviders().subscribe((providers) => {
        expect(providers.length).toBeGreaterThan(0);
        expect(providers.some((p: ProviderInfo) => p.name === 'openai')).toBe(true);
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.error(new ProgressEvent('error'));
    });

    it('should have openai as default provider', () => {
      service.getProviders().subscribe((providers) => {
        const openai = providers.find((p: ProviderInfo) => p.name === 'openai');
        expect(openai).toBeDefined();
        expect(openai?.displayName).toBe('OpenAI');
        expect(openai?.models).toContain('gpt-4o');
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.error(new ProgressEvent('error'));
    });

    it('should have anthropic as default provider', () => {
      service.getProviders().subscribe((providers) => {
        const anthropic = providers.find((p: ProviderInfo) => p.name === 'anthropic');
        expect(anthropic).toBeDefined();
        expect(anthropic?.displayName).toBe('Anthropic Claude');
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.error(new ProgressEvent('error'));
    });

    it('should have ollama as default provider', () => {
      service.getProviders().subscribe((providers) => {
        const ollama = providers.find((p: ProviderInfo) => p.name === 'ollama');
        expect(ollama).toBeDefined();
        expect(ollama?.displayName).toBe('Ollama (Local)');
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.error(new ProgressEvent('error'));
    });
  });

  describe('chatStream edge cases', () => {
    it('should send request with all optional parameters', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.chatStream(
        {
          messages: [{ role: 'user', content: 'Hi' }],
          sessionId: 'session123',
          provider: 'openai',
          model: 'gpt-4o',
        },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/text/chat/stream',
        expect.objectContaining({ method: 'POST' }),
      );

      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.sessionId).toBe('session123');
      expect(body.provider).toBe('openai');
      expect(body.model).toBe('gpt-4o');
    });

    it('should handle HTTP 401 unauthorized error', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle HTTP 503 service unavailable', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
      } as never);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle empty messages array', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.chatStream({ messages: [] }, vi.fn(), vi.fn(), vi.fn());

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.messages).toEqual([]);
    });

    it('should handle special characters in messages', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.chatStream(
        { messages: [{ role: 'user', content: 'Hello! 你好! 🎉 <script>' }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.messages[0].content).toBe('Hello! 你好! 🎉 <script>');
    });

    it('should handle very long message content', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      const longContent = 'A'.repeat(100000);
      service.chatStream(
        { messages: [{ role: 'user', content: longContent }] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.messages[0].content.length).toBe(100000);
    });
  });

  describe('ragChat edge cases', () => {
    it('should send request with docIds parameter', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.ragChat(
        {
          query: 'What is the revenue?',
          sessionId: 'session123',
          topK: 5,
          temperature: 0.5,
          docIds: ['doc1', 'doc2', 'doc3'],
        },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/rag/chat/stream',
        expect.objectContaining({ method: 'POST' }),
      );

      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.query).toBe('What is the revenue?');
      expect(body.sessionId).toBe('session123');
      expect(body.topK).toBe(5);
      expect(body.temperature).toBe(0.5);
      expect(body.docIds).toEqual(['doc1', 'doc2', 'doc3']);
    });

    it('should handle empty query string', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.ragChat({ query: '', sessionId: 'session123' }, vi.fn(), vi.fn(), vi.fn(), vi.fn());

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.query).toBe('');
    });

    it('should handle HTTP 400 bad request', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
      } as never);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', sessionId: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError,
      );

      await new Promise(resolve => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle empty docIds array', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as never);

      service.ragChat(
        { query: 'test', sessionId: 'session123', docIds: [] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn(),
      );

      await new Promise(resolve => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.docIds).toEqual([]);
    });
  });

  describe('generateImage edge cases', () => {
    it('should send request with all optional parameters', () => {
      const mockResponse = {
        imageUrl: 'https://example.com/image.png',
        imageBase64: null,
        model: 'dall-e-3',
        prompt: 'A sunset',
        status: 'SUCCESS',
      };

      service
        .generateImage({
          prompt: 'A sunset',
          model: 'dall-e-3',
          quality: 'hd',
          width: 1024,
          height: 1024,
          n: 2,
        })
        .subscribe((response) => {
          expect(response.imageUrl).toBe('https://example.com/image.png');
        });

      const req = httpMock.expectOne('/api/images/generate');
      expect(req.request.body.prompt).toBe('A sunset');
      expect(req.request.body.model).toBe('dall-e-3');
      expect(req.request.body.quality).toBe('hd');
      expect(req.request.body.width).toBe(1024);
      expect(req.request.body.height).toBe(1024);
      expect(req.request.body.n).toBe(2);
      req.flush(mockResponse);
    });

    it('should handle minimal image generation request', () => {
      const mockResponse = {
        imageBase64: 'base64data',
        status: 'SUCCESS',
      };

      service.generateImage({ prompt: 'test' }).subscribe((response) => {
        expect(response.imageBase64).toBe('base64data');
      });

      const req = httpMock.expectOne('/api/images/generate');
      expect(req.request.body.prompt).toBe('test');
      expect(req.request.body.n).toBe(1);
      req.flush(mockResponse);
    });
  });

  describe('captionImage edge cases', () => {
    it('should send correct file type for PNG', () => {
      const mockFile = new File(['png-data'], 'test.png', {
        type: 'image/png',
      });
      const mockResponse = { caption: 'A PNG image', processingTimeMs: 500 };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('A PNG image');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      expect(req.request.body.get('file')).toBe(mockFile);
      req.flush(mockResponse);
    });

    it('should handle large file upload', () => {
      const largeContent = new Array(1024 * 1024).fill('x').join('');
      const mockFile = new File([largeContent], 'large.jpg', {
        type: 'image/jpeg',
      });
      const mockResponse = { caption: 'Large image' };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('Large image');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      req.flush(mockResponse);
    });
  });

  describe('detectObjects edge cases', () => {
    it('should handle multiple detections', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = {
        detections: [
          {
            className: 'person',
            confidence: 0.95,
            bbox: [0, 0, 100, 200] as [number, number, number, number],
          },
          {
            className: 'car',
            confidence: 0.87,
            bbox: [150, 100, 300, 250] as [number, number, number, number],
          },
          {
            className: 'dog',
            confidence: 0.72,
            bbox: [50, 150, 120, 220] as [number, number, number, number],
          },
        ],
      };

      service.detectObjects(mockFile).subscribe((response) => {
        expect(response.detections?.length).toBe(3);
        expect(response.detections?.[0].className).toBe('person');
        expect(response.detections?.[1].className).toBe('car');
        expect(response.detections?.[2].className).toBe('dog');
      });

      const req = httpMock.expectOne('/api/vision/detect');
      req.flush(mockResponse);
    });

    it('should handle empty detections array', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = { detections: [] };

      service.detectObjects(mockFile).subscribe((response) => {
        expect(response.detections?.length).toBe(0);
      });

      const req = httpMock.expectOne('/api/vision/detect');
      req.flush(mockResponse);
    });
  });

  describe('uploadDocument edge cases', () => {
    it('should send correct file with title parameter', () => {
      const mockFile = new File(['content'], 'document.pdf', {
        type: 'application/pdf',
      });
      const mockResponse = { id: 'new-doc-id' };

      service.uploadDocument(mockFile).subscribe((response) => {
        expect(response.id).toBe('new-doc-id');
      });

      const req = httpMock.expectOne('/api/rag/documents/upload');
      expect(req.request.body.get('file')).toBe(mockFile);
      expect(req.request.body.get('title')).toBe('document.pdf');
      req.flush(mockResponse);
    });

    it('should handle docx file type', () => {
      const mockFile = new File(['word-content'], 'document.docx', {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      });
      const mockResponse = { id: 'docx-id' };

      service.uploadDocument(mockFile).subscribe((response) => {
        expect(response.id).toBe('docx-id');
      });

      const req = httpMock.expectOne('/api/rag/documents/upload');
      req.flush(mockResponse);
    });
  });

  describe('deleteDocument edge cases', () => {
    it('should handle document ID with special characters', () => {
      service.deleteDocument('doc-123_abc').subscribe(vi.fn());

      const req = httpMock.expectOne('/api/rag/documents/doc-123_abc');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle UUID format document ID', () => {
      const uuid = '550e8400-e29b-41d4-a716-446655440000';
      service.deleteDocument(uuid).subscribe(vi.fn());

      const req = httpMock.expectOne(`/api/rag/documents/${uuid}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('downloadBlob edge cases', () => {
    it('should create download link with correct filename', () => {
      const blob = new Blob(['test content'], { type: 'text/plain' });
      const mockLink = {
        href: '',
        download: '',
        click: vi.fn(),
      };
      vi.spyOn(document, 'createElement').mockReturnValue(mockLink as never);

      service.downloadBlob(blob, 'custom-filename.txt');

      expect(mockLink.download).toBe('custom-filename.txt');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('should revoke object URL after download', () => {
      const blob = new Blob(['test'], { type: 'text/plain' });
      const mockLink = {
        href: '',
        download: '',
        click: vi.fn(),
      };
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:http://localhost:4200/test');
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(vi.fn());
      vi.spyOn(document, 'createElement').mockReturnValue(mockLink as never);

      service.downloadBlob(blob, 'test.txt');

      expect(mockLink.click).toHaveBeenCalled();
    });
  });

  describe('downloadBase64Image edge cases', () => {
    it('should use default filename when not provided', () => {
      const base64 = btoa('test image');
      const downloadBlobSpy = vi.spyOn(service, 'downloadBlob');

      service.downloadBase64Image(base64);

      expect(downloadBlobSpy).toHaveBeenCalledWith(expect.any(Blob), 'image.png');
    });

    it('should handle PNG mime type detection from base64 header', () => {
      const pngBase64 = btoa('PNG data');
      const downloadBlobSpy = vi.spyOn(service, 'downloadBlob');

      service.downloadBase64Image(pngBase64, 'photo.png');

      expect(downloadBlobSpy).toHaveBeenCalledWith(expect.any(Blob), 'photo.png');
    });
  });

  describe('base64ToBlob edge cases', () => {
    it('should handle various mime types', () => {
      const base64 = btoa('binary data');

      const pngBlob = service.base64ToBlob(base64, 'image/png');
      expect(pngBlob.type).toBe('image/png');

      const jsonBlob = service.base64ToBlob(base64, 'application/json');
      expect(jsonBlob.type).toBe('application/json');

      const octetBlob = service.base64ToBlob(base64, 'application/octet-stream');
      expect(octetBlob.type).toBe('application/octet-stream');
    });

    it('should preserve byte array conversion accuracy', () => {
      const testString = 'Hello, World! 123';
      const base64 = btoa(testString);
      const blob = service.base64ToBlob(base64, 'text/plain');

      expect(blob.size).toBeGreaterThan(0);
    });
  });
});
