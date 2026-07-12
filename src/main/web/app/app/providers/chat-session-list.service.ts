import { computed, inject, Injectable } from '@angular/core';
import type { SessionList } from '../../layout/services/session-list.token';
import type { SidebarSession } from '../../layout/models/sidebar-session.model';
import { ChatService } from '../../ai-hub/chat.service';

@Injectable()
export class ChatSessionListService implements SessionList {
  private readonly chatService = inject(ChatService);

  readonly sessions = computed<SidebarSession[]>(() => {
    return this.chatService.sessions().map(session => ({
      id: session.sessionId,
      title: session.title,
      timestamp: new Date(session.lastActivityAt),
      pinned: false,
    }));
  });

  readonly activeSessionId = this.chatService.activeSessionId;

  initializeSessions(): void {
    this.chatService.initializeSessions();
  }

  createSession(): void {
    this.chatService.createSession();
  }

  selectSession(sessionId: string): void {
    this.chatService.selectSession(sessionId);
  }

  deleteSession(sessionId: string): void {
    this.chatService.deleteSession(sessionId);
  }
}
