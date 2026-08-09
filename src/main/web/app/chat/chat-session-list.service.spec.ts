import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ChatSessionListService } from './chat-session-list.service';
import { ChatService } from './chat.service';

const PINNED_KEY = 'explore-ai.chat.pinnedSessionIds';

describe('ChatSessionListService', () => {
  let service: ChatSessionListService;
  let chatService: {
    sessions: ReturnType<typeof signal>;
    activeSessionId: ReturnType<typeof signal>;
    initializeSessions: ReturnType<typeof vi.fn>;
    createSession: ReturnType<typeof vi.fn>;
    selectSession: ReturnType<typeof vi.fn>;
    deleteSession: ReturnType<typeof vi.fn>;
  };
  let storage: Record<string, string>;

  beforeEach(() => {
    storage = {};
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage[key] ?? null,
      setItem: (key: string, value: string) => {
        storage[key] = value;
      },
      removeItem: (key: string) => {
        delete storage[key];
      },
    });

    chatService = {
      sessions: signal([
        {
          sessionId: 's1',
          title: 'First',
          messageCount: 2,
          lastActivityAt: '2026-01-01T00:00:00.000Z',
        },
        {
          sessionId: 'empty',
          title: 'New Chat',
          messageCount: 0,
          lastActivityAt: '2026-01-02T00:00:00.000Z',
        },
      ]),
      activeSessionId: signal('s1'),
      initializeSessions: vi.fn(),
      createSession: vi.fn(),
      selectSession: vi.fn(),
      deleteSession: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        ChatSessionListService,
        { provide: ChatService, useValue: chatService },
      ],
    });
    service = TestBed.inject(ChatSessionListService);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    TestBed.resetTestingModule();
  });

  it('should map sessions with history to sidebar format', () => {
    expect(service.sessions()).toEqual([
      {
        id: 's1',
        title: 'First',
        timestamp: new Date('2026-01-01T00:00:00.000Z'),
        pinned: false,
      },
    ]);
  });

  it('should hide empty sessions from the sidebar list', () => {
    expect(service.sessions().some(session => session.id === 'empty')).toBe(false);
  });

  it('should expose active session id', () => {
    expect(service.activeSessionId()).toBe('s1');
  });

  it('should delegate initializeSessions', () => {
    service.initializeSessions();
    expect(chatService.initializeSessions).toHaveBeenCalled();
  });

  it('should delegate createSession', () => {
    service.createSession();
    expect(chatService.createSession).toHaveBeenCalled();
  });

  it('should delegate selectSession with navigateToChat', () => {
    service.selectSession('s2');
    expect(chatService.selectSession).toHaveBeenCalledWith('s2', { navigateToChat: true });
  });

  it('should delegate deleteSession', () => {
    service.deleteSession('s1');
    expect(chatService.deleteSession).toHaveBeenCalledWith('s1');
  });

  it('should pin a session and persist ids in localStorage', () => {
    service.togglePin('s1');
    expect(service.sessions()[0]?.pinned).toBe(true);
    expect(JSON.parse(storage[PINNED_KEY] ?? '[]')).toEqual(['s1']);
  });

  it('should unpin a session when toggled again', () => {
    service.togglePin('s1');
    service.togglePin('s1');
    expect(service.sessions()[0]?.pinned).toBe(false);
    expect(JSON.parse(storage[PINNED_KEY] ?? '[]')).toEqual([]);
  });

  it('should restore pinned state from localStorage on create', () => {
    storage[PINNED_KEY] = JSON.stringify(['s1']);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        ChatSessionListService,
        { provide: ChatService, useValue: chatService },
      ],
    });
    const restored = TestBed.inject(ChatSessionListService);
    expect(restored.sessions()[0]?.pinned).toBe(true);
  });

  it('should clear pin when deleting a pinned session', () => {
    service.togglePin('s1');
    service.deleteSession('s1');
    expect(JSON.parse(storage[PINNED_KEY] ?? '[]')).toEqual([]);
  });
});
