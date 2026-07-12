import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import { ApiChatService } from './api-chat.service';
import { ApiHealthService } from './api-health.service';
import { ApiMediaService } from './api-media.service';
import { ApiRagService } from './api-rag.service';
import { of } from 'rxjs';

describe('ApiService', () => {
  let service: ApiService;
  const chatApi = {
    getProviders: vi.fn(() => of([])),
    getModels: vi.fn(() => of([])),
    createSession: vi.fn(() => of({ id: 's1', title: 'Chat' })),
    getSessions: vi.fn(() => of([])),
    getSessionMessages: vi.fn(() => of([])),
    deleteSession: vi.fn(() => of(void 0)),
    chatStream: vi.fn(() => ({ abort: vi.fn() })),
  };
  const healthApi = { health: vi.fn(() => of({ status: 'UP' })) };
  const mediaApi = {
    generateImage: vi.fn(() => of({ model: 'dall-e-3', prompt: 'p' })),
    captionImage: vi.fn(() => of({ caption: 'caption' })),
    detectObjects: vi.fn(() => of({ detections: [] })),
    ocrImage: vi.fn(() => of({ fullText: 'text' })),
    getVoices: vi.fn(() => of([])),
    synthesizeSpeech: vi.fn(() => of(new Blob())),
    downloadBlob: vi.fn(),
    base64ToBlob: vi.fn(() => new Blob()),
  };
  const ragApi = {
    getDocuments: vi.fn(() => of({ documents: [] })),
    uploadDocument: vi.fn(() => of({ id: 'doc-1' })),
    deleteDocument: vi.fn(() => of(void 0)),
    ragChat: vi.fn(() => ({ abort: vi.fn() })),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        ApiService,
        { provide: ApiChatService, useValue: chatApi },
        { provide: ApiHealthService, useValue: healthApi },
        { provide: ApiMediaService, useValue: mediaApi },
        { provide: ApiRagService, useValue: ragApi },
      ],
    });
    service = TestBed.inject(ApiService);
  });

  it('should delegate health checks', () => {
    service.health().subscribe();
    expect(healthApi.health).toHaveBeenCalled();
  });

  it('should delegate provider listing', () => {
    service.getProviders().subscribe();
    expect(chatApi.getProviders).toHaveBeenCalled();
  });

  it('should delegate rag chat streaming', () => {
    const query = { query: 'test' };
    const onChunk = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();
    const onAbort = vi.fn();
    service.ragChat(query, onChunk, onDone, onError, onAbort);
    expect(ragApi.ragChat).toHaveBeenCalledWith(
      query, onChunk, onDone, onError, onAbort,
    );
  });

  it('should delegate image generation', () => {
    const params = { prompt: 'cat', model: 'dall-e-3', quality: 'standard', width: 512, height: 512, n: 1 };
    service.generateImage(params).subscribe();
    expect(mediaApi.generateImage).toHaveBeenCalledWith(params);
  });
});
