import { InjectionToken, type Signal } from '@angular/core';
import type { Instant } from '@js-joda/core';

export interface SidebarSession {
  id: string;
  title: string;
  timestamp: Instant;
  pinned: boolean;
}

/** Layout-level contract: sidebar reads session list without importing chat feature. */
export interface SessionList {
  readonly sessions: Signal<SidebarSession[]>;
  readonly activeSessionId: Signal<string | null>;
  initializeSessions(): void;
  createSession(): void;
  selectSession(sessionId: string): void;
  deleteSession(sessionId: string): void;
  togglePin(sessionId: string): void;
}

export const SESSION_LIST = new InjectionToken<SessionList>('SessionList');
