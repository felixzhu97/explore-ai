import { computed, inject, Injectable, signal } from '@angular/core';
import { STORAGE_KEYS } from '../storage-keys';
import type {
  SessionList,
  SidebarSession,
} from '../layout/session-list.token';
import { ChatService } from './chat.service';
import { hasText } from '../shared/presence';

@Injectable()
export class ChatSessionListService implements SessionList {
  readonly #chatService = inject(ChatService);

  readonly #pinnedIds = signal<string[]>(readPinnedIds());

  readonly sessions = computed<SidebarSession[]>(() => {
    const pinned = new Set(this.#pinnedIds());
    return this.#chatService.sessions()
      .filter(session => session.messageCount > 0)
      .map(session => ({
        id: session.sessionId,
        title: session.title,
        timestamp: session.lastActivityAt,
        pinned: pinned.has(session.sessionId),
      }));
  });

  readonly activeSessionId = this.#chatService.activeSessionId;

  /** Loads the chat sessions once. */
  initializeSessions(): void {
    this.#chatService.initializeSessions();
  }

  /** Starts a new chat session. */
  createSession(): void {
    this.#chatService.createSession();
  }

  /** Opens a chat session. */
  selectSession(sessionId: string): void {
    this.#chatService.selectSession(sessionId, { navigateToChat: true });
  }

  /** Deletes a chat session and unpins it. */
  deleteSession(sessionId: string): void {
    this.#removePinnedId(sessionId);
    this.#chatService.deleteSession(sessionId);
  }

  /** Pins or unpins a chat session. */
  togglePin(sessionId: string): void {
    const current = this.#pinnedIds();
    if (current.includes(sessionId)) {
      this.#writePinnedIds(current.filter(id => id !== sessionId));
      return;
    }
    this.#writePinnedIds([...current, sessionId]);
  }

  #removePinnedId(sessionId: string): void {
    const current = this.#pinnedIds();
    if (!current.includes(sessionId)) {
      return;
    }
    this.#writePinnedIds(current.filter(id => id !== sessionId));
  }

  #writePinnedIds(ids: string[]): void {
    this.#pinnedIds.set(ids);
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
    if (!hasText(raw)) {
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
