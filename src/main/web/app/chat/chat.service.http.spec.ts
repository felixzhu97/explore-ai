import { describe, expect, it, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../core/api.constants';
import { ChatService } from './chat.service';

vi.mock('../core/streaming/sse-client', () => ({
  parseChatStreamEvent: vi.fn(),
  streamSsePost: vi.fn(() => ({ abort: vi.fn() })),
}));

describe('ChatService http flows', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ChatService],
    });
    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should_load_providers_and_models_when_api_succeeds', () => {
    service.loadProviders();
    httpMock.expectOne(`${API_BASE_URL}/text/providers`).flush([
      {
        name: 'openai',
        displayName: 'DeepSeek',
        models: ['deepseek-v4-flash'],
        status: 'available',
      },
    ]);
    httpMock
      .expectOne(
        req => req.url === `${API_BASE_URL}/text/models` && req.params.get('provider') === 'openai',
      )
      .flush({
        provider: 'openai',
        models: [{ name: 'deepseek-v4-flash', provider: 'openai' }],
        count: 1,
      });

    expect(service.providers()).toHaveLength(1);
    expect(service.selectedProvider()).toBe('openai');
    expect(service.models()[0].name).toBe('deepseek-v4-flash');
  });

  it('should_fallback_providers_when_api_fails', () => {
    service.loadProviders();
    httpMock.expectOne(`${API_BASE_URL}/text/providers`).error(new ProgressEvent('error'));
    expect(service.providers()[0].name).toBe('openai');
    expect(service.selectedModel()).toBe('deepseek-v4-flash');
  });

  it('should_set_error_when_provider_unavailable', () => {
    service.providers.set([
      {
        name: 'ollama',
        displayName: 'Ollama',
        models: [],
        status: 'unavailable',
      },
    ]);
    service.setProvider('ollama');
    expect(service.error()).toContain('not configured');
  });

  it('should_set_model_and_tools_flags', () => {
    service.setModel('deepseek-v4-pro');
    service.setToolsEnabled(false);
    expect(service.selectedModel()).toBe('deepseek-v4-pro');
    expect(service.toolsEnabled()).toBe(false);
  });

  it('should_create_and_select_session', () => {
    service.createSession();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 's1',
      title: 'New',
      createdAt: '2026-07-01T00:00:00Z',
      lastActivityAt: '2026-07-01T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/s1/messages`).flush([]);
    expect(service.activeSessionId()).toBe('s1');
    expect(service.sessions()[0].sessionId).toBe('s1');
  });

  it('should_load_sessions_and_select_first', () => {
    service.loadSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 's2',
        title: 'Older',
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
      {
        sessionId: 's1',
        title: 'Newer',
        createdAt: '2026-07-02T00:00:00Z',
        lastActivityAt: '2026-07-02T00:00:00Z',
      },
    ]);
    httpMock.expectOne(`${API_BASE_URL}/sessions/s1/messages`).flush([
      { id: 'm1', role: 'user', content: 'hi', timestamp: '2026-07-02T00:00:00Z' },
    ]);
    expect(service.activeSessionId()).toBe('s1');
    expect(service.messages()[0].content).toBe('hi');
  });

  it('should_delete_active_session_and_clear_when_empty', () => {
    service.sessions.set([
      {
        sessionId: 's1',
        title: 'Only',
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    service.activeSessionId.set('s1');
    service.messages.set([{ id: '1', role: 'user', content: 'x', timestamp: 1 }]);

    service.deleteSession('s1');
    httpMock.expectOne(`${API_BASE_URL}/sessions/s1`).flush(null);
    expect(service.sessions()).toHaveLength(0);
    expect(service.activeSessionId()).toBeNull();
    expect(service.messages()).toHaveLength(0);
  });

  it('should_not_send_message_when_no_session', () => {
    service.sendMessage('hello');
    httpMock.expectNone(`${API_BASE_URL}/text/chat/stream`);
  });

  it('should_set_error_when_sending_with_unavailable_provider', () => {
    service.activeSessionId.set('s1');
    service.providers.set([
      {
        name: 'openai',
        displayName: 'DeepSeek',
        models: [],
        status: 'unavailable',
      },
    ]);
    service.selectedProvider.set('openai');
    service.sendMessage('hello');
    expect(service.error()).toContain('not configured');
  });
});
