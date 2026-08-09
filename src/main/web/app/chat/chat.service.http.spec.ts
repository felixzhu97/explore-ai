import { describe, expect, it, beforeEach, vi } from 'vitest';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { API_BASE_URL } from '../core/api.constants';
import { ChatService } from './chat.service';

vi.mock('../core/streaming/sse-client', () => ({
  parseChatStreamEvent: vi.fn(),
  streamSsePost: vi.fn(() => ({ abort: vi.fn() })),
}));

@Component({ standalone: true, template: '' })
class BlankHostComponent {}

describe('ChatService http flows', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'chat', pathMatch: 'full', component: BlankHostComponent },
          { path: 'chat/:sessionId', component: BlankHostComponent },
        ]),
        ChatService,
      ],
    });
    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  it('should_clearActiveChatAndReloadSessions_when_resetForOwnerChange', () => {
    service.sessions.set([
      {
        sessionId: 's-old',
        title: 'Old',
        messageCount: 1,
        createdAt: '2026-08-08T00:00:00Z',
        lastActivityAt: '2026-08-08T00:00:00Z',
      },
    ]);
    service.activeSessionId.set('s-old');
    service.messages.set([
      { id: 'm1', role: 'user', content: 'hi', timestamp: Date.now() },
    ]);
    service.selectedSkillIds.set(['skill-1']);

    service.resetForOwnerChange();

    expect(service.sessions()).toEqual([]);
    expect(service.activeSessionId()).toBeNull();
    expect(service.messages()).toEqual([]);
    expect(service.selectedSkillIds()).toEqual([]);

    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([]);
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 'draft',
      title: 'New Chat',
      messageCount: 0,
      createdAt: '2026-08-08T00:00:00Z',
      lastActivityAt: '2026-08-08T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/draft/messages`).flush([]);
    expect(service.activeSessionId()).toBe('draft');
    expect(service.sessionsReady()).toBe(true);
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

  it('should create session and keep bare chat url', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.createSession();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 's1',
      title: 'New',
      messageCount: 0,
      createdAt: '2026-07-01T00:00:00Z',
      lastActivityAt: '2026-07-01T00:00:00Z',
    });
    expect(service.isLoadingSession()).toBe(true);
    httpMock.expectOne(`${API_BASE_URL}/sessions/s1/messages`).flush([]);
    expect(service.activeSessionId()).toBe('s1');
    expect(service.sessions()[0].sessionId).toBe('s1');
    expect(service.isLoadingSession()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith('/chat');
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat/s1');
    expect(sessionStorage.getItem('explore-ai.chat.activeSessionId')).toBeNull();
  });

  it('should reuse existing empty session instead of creating another', () => {
    service.sessions.set([
      {
        sessionId: 'empty-1',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-02T00:00:00Z',
      },
      {
        sessionId: 'empty-2',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    service.createSession();
    httpMock.expectNone(`${API_BASE_URL}/sessions`);
    httpMock.expectOne(`${API_BASE_URL}/sessions/empty-1/messages`).flush([]);
    httpMock.expectOne(`${API_BASE_URL}/sessions/empty-2`).flush(null);
    expect(service.activeSessionId()).toBe('empty-1');
    expect(service.sessions().every(session => session.sessionId !== 'empty-2')).toBe(true);
  });

  it('should stay on empty session on bare chat refresh instead of opening history', async () => {
    await router.navigateByUrl('/chat');
    // Stale empty-session persistence must be ignored.
    sessionStorage.setItem('explore-ai.chat.activeSessionId', 'empty');
    service.loadSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 'older',
        title: 'Older',
        messageCount: 3,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-03T00:00:00Z',
      },
      {
        sessionId: 'empty',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-02T00:00:00Z',
        lastActivityAt: '2026-07-02T00:00:00Z',
      },
      {
        sessionId: 'empty-old',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    httpMock.expectOne(`${API_BASE_URL}/sessions/empty/messages`).flush([]);
    httpMock.expectOne(`${API_BASE_URL}/sessions/empty-old`).flush(null);
    expect(service.activeSessionId()).toBe('empty');
    expect(service.messages()).toEqual([]);
    expect(sessionStorage.getItem('explore-ai.chat.activeSessionId')).toBeNull();
    expect(service.sessions().some(session => session.sessionId === 'empty-old')).toBe(false);
  });

  it('should create empty session on bare chat when only history exists', async () => {
    await router.navigateByUrl('/chat');
    sessionStorage.setItem('explore-ai.chat.activeSessionId', 'older');
    service.initializeSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 'older',
        title: 'Older',
        messageCount: 3,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-03T00:00:00Z',
      },
    ]);
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 'fresh',
      title: 'New Chat',
      messageCount: 0,
      createdAt: '2026-07-04T00:00:00Z',
      lastActivityAt: '2026-07-04T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/fresh/messages`).flush([]);
    expect(service.activeSessionId()).toBe('fresh');
    expect(sessionStorage.getItem('explore-ai.chat.activeSessionId')).toBeNull();
  });

  it('should not demote deep link url while session history is loading', async () => {
    await router.navigateByUrl('/chat/s1');
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    // Sessions list not ready yet (refresh race) — must not jump to bare /chat.
    service.selectSession('s1');
    expect(service.isLoadingSession()).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat');
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat', expect.anything());

    httpMock.expectOne(`${API_BASE_URL}/sessions/s1/messages`).flush([
      { id: 'm1', role: 'user', content: 'hi', timestamp: '2026-07-02T00:00:00Z' },
    ]);
    expect(service.isLoadingSession()).toBe(false);
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat');
  });

  it('should load sessions and keep bare chat without opening history', async () => {
    await router.navigateByUrl('/chat');
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.loadSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 's2',
        title: 'Older',
        messageCount: 1,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
      {
        sessionId: 's1',
        title: 'Newer',
        messageCount: 2,
        createdAt: '2026-07-02T00:00:00Z',
        lastActivityAt: '2026-07-02T00:00:00Z',
      },
    ]);
    httpMock.expectNone(`${API_BASE_URL}/sessions/s1/messages`);
    expect(service.activeSessionId()).toBeNull();
    expect(service.messages()).toEqual([]);
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat/s1');
  });

  it('should prefer session from path when loading sessions', async () => {
    await router.navigateByUrl('/chat/s2');
    service.loadSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 's1',
        title: 'Newer',
        messageCount: 2,
        createdAt: '2026-07-02T00:00:00Z',
        lastActivityAt: '2026-07-02T00:00:00Z',
      },
      {
        sessionId: 's2',
        title: 'From URL',
        messageCount: 1,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    httpMock.expectOne(`${API_BASE_URL}/sessions/s2/messages`).flush([
      { id: 'm2', role: 'user', content: 'url', timestamp: '2026-07-01T00:00:00Z' },
    ]);
    expect(service.activeSessionId()).toBe('s2');
    expect(service.messages()[0].content).toBe('url');
    expect(service.isLoadingSession()).toBe(false);
  });

  it('should ignore stale history when switching sessions quickly', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.selectSession('s1');
    expect(service.isLoadingSession()).toBe(true);
    const first = httpMock.expectOne(`${API_BASE_URL}/sessions/s1/messages`);

    service.selectSession('s2');
    const second = httpMock.expectOne(`${API_BASE_URL}/sessions/s2/messages`);

    first.flush([
      { id: 'old', role: 'user', content: 'stale', timestamp: '2026-07-01T00:00:00Z' },
    ]);
    expect(service.activeSessionId()).toBe('s2');
    expect(service.messages()).toEqual([]);

    second.flush([
      { id: 'new', role: 'user', content: 'fresh', timestamp: '2026-07-02T00:00:00Z' },
    ]);
    expect(service.messages()[0].content).toBe('fresh');
    expect(service.isLoadingSession()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith('/chat/s2');
  });

  it('should keep bare url when selecting empty owned session', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.sessions.set([
      {
        sessionId: 'empty',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    service.selectSession('empty');
    httpMock.expectOne(`${API_BASE_URL}/sessions/empty/messages`).flush([]);
    expect(service.activeSessionId()).toBe('empty');
    expect(navigateSpy).toHaveBeenCalledWith('/chat');
    expect(navigateSpy).not.toHaveBeenCalledWith('/chat/empty');
    expect(service.error()).toBeNull();
  });

  it('should redirect to /chat when deep link session does not exist', async () => {
    await router.navigateByUrl('/chat/missing');
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.initializeSessions();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush([
      {
        sessionId: 'mine',
        title: 'Mine',
        messageCount: 2,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    expect(navigateSpy).toHaveBeenCalledWith('/chat', { replaceUrl: true });
    await Promise.resolve();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 'draft',
      title: 'New Chat',
      messageCount: 0,
      createdAt: '2026-07-04T00:00:00Z',
      lastActivityAt: '2026-07-04T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/draft/messages`).flush([]);

    expect(service.isLoadingSession()).toBe(false);
    httpMock.expectNone(`${API_BASE_URL}/sessions/missing/messages`);
  });

  it('should redirect to /chat when session id is not owned', async () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.sessions.set([
      {
        sessionId: 'mine',
        title: 'Mine',
        messageCount: 2,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    service.activeSessionId.set('mine');
    service.messages.set([
      { id: 'm1', role: 'user', content: 'hi', timestamp: 1 },
    ]);

    service.selectSession('foreign');

    httpMock.expectNone(`${API_BASE_URL}/sessions/foreign/messages`);
    expect(navigateSpy).toHaveBeenCalledWith('/chat', { replaceUrl: true });
    await Promise.resolve();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 'draft',
      title: 'New Chat',
      messageCount: 0,
      createdAt: '2026-07-04T00:00:00Z',
      lastActivityAt: '2026-07-04T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/draft/messages`).flush([]);
    expect(service.error()).toBeNull();
  });

  it('should redirect to /chat when session messages return 404', async () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.sessions.set([]);
    service.activeSessionId.set(null);

    service.selectSession('gone');
    const req = httpMock.expectOne(`${API_BASE_URL}/sessions/gone/messages`);
    req.flush(null, { status: 404, statusText: 'Not Found' });
    expect(navigateSpy).toHaveBeenCalledWith('/chat', { replaceUrl: true });
    await Promise.resolve();
    httpMock.expectOne(`${API_BASE_URL}/sessions`).flush({
      sessionId: 'draft',
      title: 'New Chat',
      messageCount: 0,
      createdAt: '2026-07-05T00:00:00Z',
      lastActivityAt: '2026-07-05T00:00:00Z',
    });
    httpMock.expectOne(`${API_BASE_URL}/sessions/draft/messages`).flush([]);
    expect(service.error()).toBeNull();
    expect(service.isLoadingSession()).toBe(false);
  });

  it('should promote url when first message is sent on bare chat', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    service.sessions.set([
      {
        sessionId: 's1',
        title: 'New Chat',
        messageCount: 0,
        createdAt: '2026-07-01T00:00:00Z',
        lastActivityAt: '2026-07-01T00:00:00Z',
      },
    ]);
    service.activeSessionId.set('s1');
    service.providers.set([
      {
        name: 'openai',
        displayName: 'DeepSeek',
        models: ['deepseek-v4-flash'],
        status: 'available',
      },
    ]);
    service.selectedProvider.set('openai');
    service.selectedModel.set('deepseek-v4-flash');

    service.sendMessage('hello');

    expect(navigateSpy).toHaveBeenCalledWith('/chat/s1');
    expect(service.messages().some(message => message.role === 'user')).toBe(true);
  });

  it('should_delete_active_session_and_clear_when_empty', () => {
    service.sessions.set([
      {
        sessionId: 's1',
        title: 'Only',
        messageCount: 1,
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
