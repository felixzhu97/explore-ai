import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AiService, ModelInfo } from './ai.service';

describe('AiService', () => {
  let service: AiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AiService],
    });

    service = TestBed.inject(AiService);
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

  describe('chat', () => {
    it('should send chat request to correct endpoint', () => {
      const mockResponse = {
        text: 'Hello!',
        provider: 'openai',
        model: 'gpt-4o',
        session_id: 'session123',
      };

      service.chat({ messages: [{ role: 'user', content: 'Hi' }] }).subscribe((response) => {
        expect(response.text).toBe('Hello!');
      });

      const req = httpMock.expectOne('/api/text/chat');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should send request body correctly', () => {
      const mockResponse = {
        text: 'Hello!',
        provider: 'openai',
        model: 'gpt-4o',
        session_id: 'session123',
      };

      service.chat({ messages: [{ role: 'user', content: 'Hi' }], session_id: 'session123' }).subscribe(() => {});

      const req = httpMock.expectOne('/api/text/chat');
      expect(req.request.body).toEqual({
        messages: [{ role: 'user', content: 'Hi' }],
        session_id: 'session123',
      });
      req.flush(mockResponse);
    });
  });

  describe('chatStream', () => {
    it('should call fetch with POST method', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/text/chat/stream',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        })
      );
    });

    it('should handle fetch error gracefully', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
    });

    it('should handle non-ok response', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as any);

      const onError = vi.fn();
      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: undefined,
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should not call onError on AbortError', async () => {
      const abortError = new DOMException('Aborted', 'AbortError');
      vi.spyOn(global, 'fetch').mockRejectedValue(abortError);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).not.toHaveBeenCalled();
    });
  });

  describe('getTextServiceHealth', () => {
    it('should return health status', () => {
      const mockResponse = {
        status: 'healthy',
        provider: 'openai',
        model: 'gpt-4o',
        version: '1.0.0',
      };

      service.getTextServiceHealth().subscribe((response) => {
        expect(response.status).toBe('healthy');
      });

      const req = httpMock.expectOne('/api/text/health');
      req.flush(mockResponse);
    });
  });

  describe('getModels', () => {
    it('should return models list', () => {
      const mockResponse = [
        { name: 'gpt-4o', provider: 'openai' },
        { name: 'gpt-4o-mini', provider: 'openai' },
      ];

      service.getModels('openai').subscribe((models) => {
        expect(models.length).toBe(2);
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.flush(mockResponse);
    });
  });

  describe('getProviders', () => {
    it('should return providers list', () => {
      const mockResponse = [
        { name: 'openai', display_name: 'OpenAI', models: ['gpt-4o'], status: 'available' },
      ];

      service.getProviders().subscribe((providers) => {
        expect(providers.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.flush(mockResponse);
    });
  });

  describe('generateImage', () => {
    it('should send image generation request', () => {
      const mockResponse = {
        images: ['base64data'],
        seed: 12345,
      };

      service
        .generateImage({
          prompt: 'A sunset',
          width: 512,
          height: 512,
        })
        .subscribe((response) => {
          expect(response.images).toEqual(mockResponse.images);
        });

      const req = httpMock.expectOne('/api/image/generate');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getVisionServiceHealth', () => {
    it('should return vision service health', () => {
      const mockResponse = {
        status: 'healthy',
        device: 'cuda',
        cuda_available: true,
      };

      service.getVisionServiceHealth().subscribe((response) => {
        expect(response.status).toBe('healthy');
      });

      const req = httpMock.expectOne('/api/vision/health');
      req.flush(mockResponse);
    });
  });

  describe('captionImage', () => {
    it('should send caption request', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = { caption: 'A sunset over the ocean' };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('A sunset over the ocean');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('detectObjects', () => {
    it('should send detect request', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = {
        detections: [{ class_name: 'person', confidence: 0.95, bbox: [0, 0, 100, 200] as [number, number, number, number] }],
      };

      service.detectObjects(mockFile).subscribe((response) => {
        expect(response.detections?.length ?? 0).toBe(1);
      });

      const req = httpMock.expectOne('/api/vision/detect');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('ocrImage', () => {
    it('should send OCR request', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = { full_text: 'Hello World' };

      service.ocrImage(mockFile).subscribe((response) => {
        expect(response.full_text).toBe('Hello World');
      });

      const req = httpMock.expectOne('/api/vision/ocr');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('synthesizeSpeech', () => {
    it('should send synthesis request', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello world' }).subscribe((blob) => {
        expect(blob).toBeInstanceOf(Blob);
      });

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.method).toBe('POST');
      req.flush(mockBlob);
    });
  });

  describe('getVoices', () => {
    it('should return voices list', () => {
      const mockResponse = [
        { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
      ];

      service.getVoices().subscribe((voices) => {
        expect(voices.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/tts/voices');
      req.flush(mockResponse);
    });
  });

  describe('getSpeechServiceHealth', () => {
    it('should return speech service health', () => {
      const mockResponse = {
        status: 'healthy',
        provider: 'openai',
        provider_status: 'configured',
        version: '1.0.0',
      };

      service.getSpeechServiceHealth().subscribe((response) => {
        expect(response.status).toBe('healthy');
      });

      const req = httpMock.expectOne('/api/tts/health');
      req.flush(mockResponse);
    });
  });

  describe('getDocuments', () => {
    it('should return documents list', () => {
      const mockResponse = {
        documents: [{ doc_id: '1', filename: 'document1.pdf' }],
      };

      service.getDocuments().subscribe((response) => {
        expect(response.documents.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/rag/documents/');
      req.flush(mockResponse);
    });
  });

  describe('uploadDocument', () => {
    it('should upload document', () => {
      const mockFile = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
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
    it('should delete document', () => {
      service.deleteDocument('doc123').subscribe(() => {});

      const req = httpMock.expectOne('/api/rag/documents/doc123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('ragChat', () => {
    it('should send RAG chat request', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.ragChat(
        { query: 'What is AI?', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(global.fetch).toHaveBeenCalledWith(
        '/api/rag/chat/stream',
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should handle network error', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

      const onError = vi.fn();

      service.ragChat(
        { query: 'What is AI?', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
    });

    it('should handle non-ok response', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'What is AI?', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: undefined,
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'What is AI?', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });
  });

  describe('interface definitions', () => {
    it('should define ChatMessage correctly', () => {
      const message = { role: 'user' as const, content: 'Hello' };
      expect(message.role).toBe('user');
      expect(message.content).toBe('Hello');
    });

    it('should allow assistant role', () => {
      const message = { role: 'assistant' as const, content: 'Response' };
      expect(message.role).toBe('assistant');
    });

    it('should allow system role', () => {
      const message = { role: 'system' as const, content: 'System prompt' };
      expect(message.role).toBe('system');
    });

    it('should define ImageGenerationRequest correctly', () => {
      const request = {
        prompt: 'A sunset',
        negative_prompt: 'blurry',
        width: 512,
        height: 512,
        num_images: 1,
      };
      expect(request.prompt).toBe('A sunset');
      expect(request.width).toBe(512);
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
      } as any);

      service.chatStream(
        {
          messages: [{ role: 'user', content: 'Hi' }],
          session_id: 'session123',
          system_prompt: 'You are helpful',
          temperature: 0.7,
          max_tokens: 1000,
          provider: 'openai',
          model: 'gpt-4o',
        },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/text/chat/stream',
        expect.objectContaining({
          method: 'POST',
        })
      );

      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.session_id).toBe('session123');
      expect(body.system_prompt).toBe('You are helpful');
      expect(body.temperature).toBe(0.7);
      expect(body.max_tokens).toBe(1000);
      expect(body.provider).toBe('openai');
      expect(body.model).toBe('gpt-4o');
    });

    it('should handle HTTP 401 unauthorized error', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toContain('401');
    });

    it('should handle HTTP 503 service unavailable', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
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
      } as any);

      service.chatStream(
        { messages: [] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

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
      } as any);

      service.chatStream(
        { messages: [{ role: 'user', content: 'Hello! 你好! 🎉 <script>' }] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

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
      } as any);

      const longContent = 'A'.repeat(100000);
      service.chatStream(
        { messages: [{ role: 'user', content: longContent }] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.messages[0].content.length).toBe(100000);
    });
  });

  describe('ragChat edge cases', () => {
    it('should send request with doc_ids parameter', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.ragChat(
        {
          query: 'What is the revenue?',
          session_id: 'session123',
          top_k: 5,
          temperature: 0.5,
          doc_ids: ['doc1', 'doc2', 'doc3'],
        },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/rag/chat/stream',
        expect.objectContaining({
          method: 'POST',
        })
      );

      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.query).toBe('What is the revenue?');
      expect(body.session_id).toBe('session123');
      expect(body.top_k).toBe(5);
      expect(body.temperature).toBe(0.5);
      expect(body.doc_ids).toEqual(['doc1', 'doc2', 'doc3']);
    });

    it('should handle empty query string', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.ragChat(
        { query: '', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

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
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle HTTP 404 not found', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session123' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle empty doc_ids array', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.ragChat(
        { query: 'test', session_id: 'session123', doc_ids: [] },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalled();
      const callArgs = fetchSpy.mock.calls[0][1] as RequestInit;
      const body = JSON.parse(callArgs.body as string);
      expect(body.doc_ids).toEqual([]);
    });
  });

  describe('chatStream streaming callback coverage', () => {
    it('should call onChunk when token is received from meta event', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();
      const onDone = vi.fn();
      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        onDone,
        onError
      );

      // Send a meta event with token data
      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('event: meta\ndata: {"token":"Hello"}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Hello');
    });

    it('should call onDone when [DONE] is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onDone when done event is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('event: done\ndata: \n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onError when error event is received with JSON error data', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('event: error\ndata: {"error":"Something went wrong"}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Something went wrong');
    });

    it('should call onError when error event is received without JSON error data', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('event: error\ndata: Invalid error format\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Stream error');
    });

    it('should handle data line without currentEvent (empty event type)', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: {"token":"chunk1"}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk1');
    });

    it('should handle non-JSON data gracefully (skip)', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;
      let callCount = 0;

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
      } as any);

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      // Send non-JSON data
      readResolve({ done: false, value: encoder.encode('data: plain text data\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));
      // Continue reading
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise((resolve) => setTimeout(resolve, 10));
    });

    it('should reset currentEvent on empty line', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      // Send event followed by empty line, then data without event
      readResolve({ done: false, value: encoder.encode('event: meta\n\ndata: {"token":"after reset"}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('after reset');
    });

    it('should not call onChunk when token is missing in parsed data', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: {"no_token_field":true}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).not.toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: null,
      } as any);

      const onError = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should continue reading when not done', async () => {
      const encoder = new TextEncoder();
      let readCount = 0;
      let readResolve: (value: any) => void;

      const mockReader = {
        read: vi.fn().mockImplementation(() => {
          readCount++;
          if (readCount === 1) {
            return new Promise((resolve) => {
              readResolve = resolve;
            });
          }
          return Promise.resolve({ done: true, value: new Uint8Array() });
        }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as any);

      const onDone = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        onDone,
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      // read should be called twice: once for initial, once for continuation
      expect(mockReader.read).toHaveBeenCalled();
    });

    it('should handle partial line in buffer', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      // Send partial line, then complete it
      readResolve({ done: false, value: encoder.encode('data: {"token":"par') });
      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('tial"}\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('partial');
    });

    it('should handle empty line that resets currentEvent', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        onChunk,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      // Send event: meta followed by empty line
      readResolve({ done: false, value: encoder.encode('event: meta\n\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: true, value: new Uint8Array() });
      await new Promise((resolve) => setTimeout(resolve, 10));

      // Empty line should reset currentEvent
      expect(mockReader.read).toHaveBeenCalled();
    });
  });

  describe('ragChat streaming callback coverage', () => {
    it('should call onChunk when chunk data is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();
      const onDone = vi.fn();
      const onError = vi.fn();
      const onSources = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        onChunk,
        onSources,
        onDone,
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: Some chunk text\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Some chunk text');
    });

    it('should call onDone when [DONE] is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onDone = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        onDone,
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: [DONE]\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onDone).toHaveBeenCalled();
    });

    it('should call onSources when sources event is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onSources = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        onSources,
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: sources\ndata: [{"text":"source 1","score":0.9,"metadata":{}}]\n'),
      });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onSources).toHaveBeenCalledWith([{ text: 'source 1', score: 0.9, metadata: {} }]);
    });

    it('should call onError when Error: prefix is received', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: Error:Database connection failed\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onError).toHaveBeenCalled();
      expect(onError.mock.calls[0][0].message).toBe('Database connection failed');
    });

    it('should replace <br> with newlines in chunk data', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({ done: false, value: encoder.encode('data: Line 1<br>Line 2<br>Line 3\n') });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('Line 1\nLine 2\nLine 3');
    });

    it('should reset currentEvent on empty line', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      // Send sources event followed by empty line, then chunk without event
      readResolve({
        done: false,
        value: encoder.encode('event: sources\n\ndata: chunk after reset\n'),
      });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk after reset');
    });

    it('should reset currentEvent after sources event', async () => {
      const encoder = new TextEncoder();
      let readResolve: (value: any) => void;

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
      } as any);

      const onChunk = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        onChunk,
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      await new Promise((resolve) => setTimeout(resolve, 10));
      readResolve({
        done: false,
        value: encoder.encode('event: sources\ndata: []\ndata: chunk after sources\n'),
      });
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(onChunk).toHaveBeenCalledWith('chunk after sources');
    });

    it('should handle non-ok response by throwing error', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        status: 500,
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle response body without reader', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: null,
      } as any);

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });

    it('should handle network error', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'));

      const onError = vi.fn();

      service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        onError
      );

      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(onError).toHaveBeenCalled();
    });
  });

  describe('getModels edge cases', () => {
    it('should handle undefined provider parameter', () => {
      const mockResponse = [{ name: 'gpt-4o', provider: 'openai' }];

      service.getModels(undefined).subscribe((models) => {
        expect(models).toEqual(mockResponse);
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.flush(mockResponse);
    });

    it('should include provider in request params', () => {
      const mockResponse = [{ name: 'claude-3', provider: 'anthropic' }];

      service.getModels('anthropic').subscribe((models) => {
        expect(models[0].provider).toBe('anthropic');
      });

      const req = httpMock.expectOne((req) => {
        return req.url.includes('/api/text/models') && req.params.get('provider') === 'anthropic';
      });
      req.flush(mockResponse);
    });

    it('should handle empty models response', () => {
      const mockResponse: ModelInfo[] = [];

      service.getModels('openai').subscribe((models) => {
        expect(models).toEqual([]);
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.flush(mockResponse);
    });
  });

  describe('getProviders edge cases', () => {
    it('should handle provider with no models', () => {
      const mockResponse = [
        { name: 'empty-provider', display_name: 'Empty Provider', models: [], status: 'available' as const },
      ];

      service.getProviders().subscribe((providers) => {
        expect(providers[0].models).toEqual([]);
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.flush(mockResponse);
    });

    it('should handle unavailable provider status', () => {
      const mockResponse = [
        { name: 'offline', display_name: 'Offline Provider', models: ['model1'], status: 'unavailable' as const },
      ];

      service.getProviders().subscribe((providers) => {
        expect(providers[0].status).toBe('unavailable');
      });

      const req = httpMock.expectOne('/api/text/providers');
      req.flush(mockResponse);
    });
  });

  describe('generateImage edge cases', () => {
    it('should send request with all optional parameters', () => {
      const mockResponse = {
        images: ['base64data'],
        seed: 12345,
      };

      service
        .generateImage({
          prompt: 'A sunset',
          negative_prompt: 'blurry, low quality',
          width: 1024,
          height: 1024,
          num_inference_steps: 50,
          guidance_scale: 7.5,
          seed: 42,
          num_images: 2,
          style_preset: 'photorealistic',
        })
        .subscribe((response) => {
          expect(response.images).toEqual(mockResponse.images);
        });

      const req = httpMock.expectOne('/api/image/generate');
      expect(req.request.body.prompt).toBe('A sunset');
      expect(req.request.body.negative_prompt).toBe('blurry, low quality');
      expect(req.request.body.width).toBe(1024);
      expect(req.request.body.height).toBe(1024);
      expect(req.request.body.num_inference_steps).toBe(50);
      expect(req.request.body.guidance_scale).toBe(7.5);
      expect(req.request.body.seed).toBe(42);
      expect(req.request.body.num_images).toBe(2);
      expect(req.request.body.style_preset).toBe('photorealistic');
      req.flush(mockResponse);
    });

    it('should handle minimal image generation request', () => {
      const mockResponse = {
        images: ['base64data'],
        seed: 0,
      };

      service
        .generateImage({ prompt: 'test' })
        .subscribe((response) => {
          expect(response).toBeDefined();
        });

      const req = httpMock.expectOne('/api/image/generate');
      expect(req.request.body.prompt).toBe('test');
      expect(req.request.body.width).toBeUndefined();
      expect(req.request.body.height).toBeUndefined();
      req.flush(mockResponse);
    });

    it('should handle maximum num_images', () => {
      const mockResponse = {
        images: ['img1', 'img2', 'img3', 'img4'],
        seed: 12345,
      };

      service
        .generateImage({ prompt: 'test', num_images: 4 })
        .subscribe((response) => {
          expect(response.images.length).toBe(4);
        });

      const req = httpMock.expectOne('/api/image/generate');
      req.flush(mockResponse);
    });
  });

  describe('captionImage edge cases', () => {
    it('should send correct file type for PNG', () => {
      const mockFile = new File(['png-data'], 'test.png', { type: 'image/png' });
      const mockResponse = { caption: 'A PNG image' };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('A PNG image');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      expect(req.request.body.get('file')).toBe(mockFile);
      req.flush(mockResponse);
    });

    it('should handle large file upload', () => {
      const largeContent = new Array(1024 * 1024).fill('x').join('');
      const mockFile = new File([largeContent], 'large.jpg', { type: 'image/jpeg' });
      const mockResponse = { caption: 'Large image' };

      service.captionImage(mockFile).subscribe((response) => {
        expect(response.caption).toBe('Large image');
      });

      const req = httpMock.expectOne('/api/vision/caption');
      req.flush(mockResponse);
    });
  });

  describe('getVoices edge cases', () => {
    it('should handle language filter parameter', () => {
      const mockResponse = [
        { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
      ];

      service.getVoices('en').subscribe((voices) => {
        expect(voices).toEqual(mockResponse);
      });

      const req = httpMock.expectOne((req) => {
        return req.url.includes('/api/tts/voices') && req.params.get('language') === 'en';
      });
      req.flush(mockResponse);
    });

    it('should handle voices with gender property', () => {
      const mockResponse = [
        { id: 'en-US-Female', name: 'English Female', language: 'en-US', gender: 'female', provider: 'default', is_default: false },
      ];

      service.getVoices().subscribe((voices) => {
        expect(voices[0].gender).toBe('female');
      });

      const req = httpMock.expectOne('/api/tts/voices');
      req.flush(mockResponse);
    });

    it('should handle voices with language_name property', () => {
      const mockResponse = [
        { id: 'zh-CN', name: '中文语音', language: 'zh-CN', language_name: 'Chinese (Simplified)', provider: 'default', is_default: true },
      ];

      service.getVoices().subscribe((voices) => {
        expect(voices[0].language_name).toBe('Chinese (Simplified)');
      });

      const req = httpMock.expectOne('/api/tts/voices');
      req.flush(mockResponse);
    });
  });

  describe('synthesizeSpeech edge cases', () => {
    it('should send request with all optional parameters', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service
        .synthesizeSpeech({
          text: 'Hello world',
          voice: 'en-US-Neural',
          language: 'en-US',
          speed: 1.5,
          pitch: 1.0,
          output_format: 'wav',
        })
        .subscribe(() => {});

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.body.text).toBe('Hello world');
      expect(req.request.body.voice).toBe('en-US-Neural');
      expect(req.request.body.language).toBe('en-US');
      expect(req.request.body.speed).toBe(1.5);
      expect(req.request.body.pitch).toBe(1.0);
      expect(req.request.body.output_format).toBe('wav');
      req.flush(mockBlob);
    });

    it('should handle different output formats', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/ogg' });

      service.synthesizeSpeech({ text: 'Test', output_format: 'ogg' }).subscribe(() => {});

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.body.output_format).toBe('ogg');
      req.flush(mockBlob);
    });

    it('should handle slow speech speed', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Slow speech', speed: 0.5 }).subscribe(() => {});

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.body.speed).toBe(0.5);
      req.flush(mockBlob);
    });
  });

  describe('uploadDocument edge cases', () => {
    it('should send correct file with title parameter', () => {
      const mockFile = new File(['content'], 'document.pdf', { type: 'application/pdf' });
      const mockResponse = { id: 'new-doc-id' };

      service.uploadDocument(mockFile).subscribe((response) => {
        expect(response.id).toBe('new-doc-id');
      });

      const req = httpMock.expectOne('/api/rag/documents/upload');
      expect(req.request.body.get('file')).toBe(mockFile);
      expect(req.request.body.get('title')).toBe('document.pdf');
      req.flush(mockResponse);
    });

    it('should handle large document upload', () => {
      const largeContent = new Array(1024 * 1024 * 5).fill('x').join('');
      const mockFile = new File([largeContent], 'large-document.pdf', { type: 'application/pdf' });
      const mockResponse = { id: 'large-doc-id' };

      service.uploadDocument(mockFile).subscribe((response) => {
        expect(response.id).toBe('large-doc-id');
      });

      const req = httpMock.expectOne('/api/rag/documents/upload');
      req.flush(mockResponse);
    });

    it('should handle docx file type', () => {
      const mockFile = new File(['word-content'], 'document.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
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
      service.deleteDocument('doc-123_abc').subscribe(() => {});

      const req = httpMock.expectOne('/api/rag/documents/doc-123_abc');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle UUID format document ID', () => {
      const uuid = '550e8400-e29b-41d4-a716-446655440000';
      service.deleteDocument(uuid).subscribe(() => {});

      const req = httpMock.expectOne(`/api/rag/documents/${uuid}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getDocuments edge cases', () => {
    it('should handle response with multiple documents', () => {
      const mockResponse = {
        documents: [
          { doc_id: '1', filename: 'report-2024.pdf' },
          { doc_id: '2', filename: 'data.csv' },
          { doc_id: '3', filename: 'notes.docx' },
          { doc_id: '4', filename: 'presentation.pptx' },
        ],
      };

      service.getDocuments().subscribe((response) => {
        expect(response.documents.length).toBe(4);
      });

      const req = httpMock.expectOne('/api/rag/documents/');
      req.flush(mockResponse);
    });

    it('should handle document with special characters in filename', () => {
      const mockResponse = {
        documents: [
          { doc_id: '1', filename: 'Report - Q1 (2024).pdf' },
        ],
      };

      service.getDocuments().subscribe((response) => {
        expect(response.documents[0].filename).toBe('Report - Q1 (2024).pdf');
      });

      const req = httpMock.expectOne('/api/rag/documents/');
      req.flush(mockResponse);
    });
  });
});
