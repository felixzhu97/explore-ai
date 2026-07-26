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
          lastActivityAt: '2026-01-01T00:00:00.000Z',
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

  it('should_map_sessions_to_sidebar_format', () => {
    expect(service.sessions()).toEqual([
      {
        id: 's1',
        title: 'First',
        timestamp: new Date('2026-01-01T00:00:00.000Z'),
        pinned: false,
      },
    ]);
  });

  it('should_expose_active_session_id', () => {
    expect(service.activeSessionId()).toBe('s1');
  });

  it('should_delegate_initializeSessions', () => {
    service.initializeSessions();
    expect(chatService.initializeSessions).toHaveBeenCalled();
  });

  it('should_delegate_createSession', () => {
    service.createSession();
    expect(chatService.createSession).toHaveBeenCalled();
  });

  it('should_delegate_selectSession', () => {
    service.selectSession('s2');
    expect(chatService.selectSession).toHaveBeenCalledWith('s2');
  });

  it('should_delegate_deleteSession', () => {
    service.deleteSession('s1');
    expect(chatService.deleteSession).toHaveBeenCalledWith('s1');
  });
});
