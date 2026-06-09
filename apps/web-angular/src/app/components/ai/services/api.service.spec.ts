import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';

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
  });

  describe('service creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('getProviders', () => {
    it('should return providers from API', () => {
      const mockProviders = [
        { name: 'openai', display_name: 'OpenAI', models: ['gpt-4o'], status: 'available' },
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

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.flush(mockResponse);
    });

    it('should return default models on error', () => {
      service.getModels('openai').subscribe((models) => {
        expect(models.length).toBeGreaterThan(0);
        expect(models[0].provider).toBe('openai');
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });

    it('should use anthropic defaults for anthropic provider', () => {
      service.getModels('anthropic').subscribe((models) => {
        expect(models.some((m) => m.name.includes('claude'))).toBe(true);
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });

    it('should use openai defaults for unknown provider', () => {
      service.getModels('unknown').subscribe((models) => {
        expect(models.some((m) => m.name.includes('gpt'))).toBe(true);
      });

      const req = httpMock.expectOne((req) => req.url.includes('/api/text/models'));
      req.error(new ProgressEvent('error'));
    });
  });

  describe('chatStream', () => {
    it('should return abort controller', () => {
      const result = service.chatStream(
        { messages: [{ role: 'user', content: 'test' }] },
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      expect(result).toHaveProperty('abort');
      expect(typeof result.abort).toBe('function');
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
          expect(response.seed).toBe(mockResponse.seed);
        });

      const req = httpMock.expectOne('/api/image/generate');
      expect(req.request.method).toBe('POST');
      expect(req.request.body.prompt).toBe('A sunset');
      req.flush(mockResponse);
    });
  });

  describe('getVoices', () => {
    it('should return voices from API', () => {
      const mockVoices = [
        { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
      ];

      service.getVoices().subscribe((voices) => {
        expect(voices).toEqual(mockVoices);
      });

      const req = httpMock.expectOne('/api/tts/voices');
      req.flush(mockVoices);
    });

    it('should return default voices on error', () => {
      service.getVoices().subscribe((voices) => {
        expect(voices.length).toBeGreaterThan(0);
        expect(voices[0].id).toBe('en-US');
      });

      const req = httpMock.expectOne('/api/tts/voices');
      req.error(new ProgressEvent('error'));
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
      expect(req.request.body.text).toBe('Hello world');
      req.flush(mockBlob);
    });

    it('should include voice parameter when provided', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello', voice: 'en-US' }).subscribe(() => {});

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.body.voice).toBe('en-US');
      req.flush(mockBlob);
    });

    it('should include speed parameter when provided', () => {
      const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });

      service.synthesizeSpeech({ text: 'Hello', speed: 1.5 }).subscribe(() => {});

      const req = httpMock.expectOne('/api/tts/synthesize');
      expect(req.request.body.speed).toBe(1.5);
      req.flush(mockBlob);
    });
  });

  describe('captionImage', () => {
    it('should send caption request with file', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = { caption: 'A beautiful sunset', processing_time_ms: 500 };

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
          { class_name: 'person', confidence: 0.95, bbox: [0, 0, 100, 200] as [number, number, number, number] },
        ],
      };

      service.detectObjects(mockFile).subscribe((response) => {
        expect(response.detections.length).toBe(1);
        expect(response.detections[0].class_name).toBe('person');
      });

      const req = httpMock.expectOne('/api/vision/detect');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('ocrImage', () => {
    it('should send OCR request with file', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const mockResponse = { full_text: 'Hello World', processing_time_ms: 300 };

      service.ocrImage(mockFile).subscribe((response) => {
        expect(response.full_text).toBe('Hello World');
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

      const req = httpMock.expectOne('/api/rag/documents/');
      req.flush(mockResponse);
    });

    it('should return empty documents on error', () => {
      service.getDocuments().subscribe((response) => {
        expect(response.documents).toEqual([]);
      });

      const req = httpMock.expectOne('/api/rag/documents/');
      req.error(new ProgressEvent('error'));
    });
  });

  describe('uploadDocument', () => {
    it('should upload document file', () => {
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
    it('should delete document by id', () => {
      service.deleteDocument('doc123').subscribe(() => {});

      const req = httpMock.expectOne('/api/rag/documents/doc123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('ragChat', () => {
    it('should return abort controller', () => {
      const result = service.ragChat(
        { query: 'test', session_id: 'session1' },
        vi.fn(),
        vi.fn(),
        vi.fn(),
        vi.fn()
      );

      expect(result).toHaveProperty('abort');
      expect(typeof result.abort).toBe('function');
    });
  });

  describe('downloadBlob', () => {
    it('should create download link and trigger click', () => {
      const blob = new Blob(['test'], { type: 'text/plain' });
      const createElementSpy = vi.spyOn(document, 'createElement');
      const appendChildSpy = vi.fn();
      const removeChildSpy = vi.fn();
      const clickSpy = vi.fn();

      Object.defineProperty(document.body, 'appendChild', { value: appendChildSpy });
      Object.defineProperty(document.body, 'removeChild', { value: removeChildSpy });

      (createElementSpy as any).mockReturnValue({
        href: '',
        download: '',
        click: clickSpy,
      });

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

  describe('default providers', () => {
    it('should have openai provider configured', () => {
      expect(service['defaultProviders']).toBeDefined();
      const openai = service['defaultProviders'].find((p) => p.name === 'openai');
      expect(openai).toBeDefined();
      expect(openai?.display_name).toBe('OpenAI');
      expect(openai?.models).toContain('gpt-4o');
    });

    it('should have anthropic provider configured', () => {
      const anthropic = service['defaultProviders'].find((p) => p.name === 'anthropic');
      expect(anthropic).toBeDefined();
      expect(anthropic?.display_name).toBe('Anthropic Claude');
    });

    it('should have ollama provider configured', () => {
      const ollama = service['defaultProviders'].find((p) => p.name === 'ollama');
      expect(ollama).toBeDefined();
      expect(ollama?.display_name).toBe('Ollama (Local)');
    });
  });
});
