import { computed, inject, Injectable, signal } from '@angular/core';
import { STORAGE_KEYS } from '../storage-keys';
import type {
  SessionList,
  SidebarSession,
} from '../layout/session-list.token';
import { ChatService } from './chat.service';

@Injectable()
export class ChatSessionListService implements SessionList {
  private readonly chatService = inject(ChatService);

  private readonly pinnedIds = signal<string[]>(readPinnedIds());

  readonly sessions = computed<SidebarSession[]>(() => {
    const pinned = new Set(this.pinnedIds());
    return this.chatService.sessions()
      .filter(session => session.messageCount > 0)
      .map(session => ({
        id: session.sessionId,
        title: session.title,
        timestamp: new Date(session.lastActivityAt),
        pinned: pinned.has(session.sessionId),
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
    this.chatService.selectSession(sessionId, { navigateToChat: true });
  }

  deleteSession(sessionId: string): void {
    this.removePinnedId(sessionId);
    this.chatService.deleteSession(sessionId);
  }

  togglePin(sessionId: string): void {
    const current = this.pinnedIds();
    if (current.includes(sessionId)) {
      this.writePinnedIds(current.filter(id => id !== sessionId));
      return;
    }
    this.writePinnedIds([...current, sessionId]);
  }

  private removePinnedId(sessionId: string): void {
    const current = this.pinnedIds();
    if (!current.includes(sessionId)) {
      return;
    }
    this.writePinnedIds(current.filter(id => id !== sessionId));
  }

  private writePinnedIds(ids: string[]): void {
    this.pinnedIds.set(ids);
    try {
      localStorage.setItem(STORAGE_KEYS.CHAT_PINNED_SESSION_IDS, JSON.stringify(ids));
    } catch {
      // Ignore quota / private-mode failures; in-memory pin state still works.
    }
  }
}

function readPinnedIds(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.CHAT_PINNED_SESSION_IDS);
    if (!raw) {
      return [];
    }
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.filter((id): id is string => typeof id === 'string');
  } catch {
    return [];
  }
}
