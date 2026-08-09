import { describe, expect, it, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ChatSessionListService } from './chat-session-list.service';
import { ChatService } from './chat.service';

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

  beforeEach(() => {
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

  it('should delegate selectSession', () => {
    service.selectSession('s2');
    expect(chatService.selectSession).toHaveBeenCalledWith('s2');
  });

  it('should delegate deleteSession', () => {
    service.deleteSession('s1');
    expect(chatService.deleteSession).toHaveBeenCalledWith('s1');
  });
});
