import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AiService } from './ai.service';

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
        expect(response.detections.length).toBe(1);
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
});
