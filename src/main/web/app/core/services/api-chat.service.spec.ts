import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiChatService } from './api-chat.service';

describe('ApiChatService', () => {
  let service: ApiChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiChatService],
    });
    service = TestBed.inject(ApiChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should fetch providers', () => {
    const mockProviders = [{ name: 'openai', displayName: 'DeepSeek', models: ['deepseek-v4-flash'], status: 'available' }];
    service.getProviders().subscribe((providers) => {
      expect(providers).toEqual(mockProviders);
    });
    const req = httpMock.expectOne('/api/text/providers');
    req.flush(mockProviders);
  });

  it('should create a session', () => {
    service.createSession('My Chat').subscribe((session) => {
      expect(session.title).toBe('My Chat');
    });
    const req = httpMock.expectOne('/api/sessions');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 's1', title: 'My Chat' });
  });

  it('should return abort handle for chat stream', () => {
    const result = service.chatStream(
      { messages: [{ role: 'user', content: 'hello' }] },
      vi.fn(),
      vi.fn(),
      vi.fn(),
    );
    expect(result.abort).toBeTypeOf('function');
  });
});
