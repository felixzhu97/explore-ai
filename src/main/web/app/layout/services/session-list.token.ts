import { InjectionToken, Signal } from '@angular/core';
import type { SidebarSession } from '../sidebar-session.model';

/** Layout-level contract: sidebar reads session list without importing chat feature. */
export interface SessionList {
  readonly sessions: Signal<SidebarSession[]>;
  readonly activeSessionId: Signal<string | null>;
  initializeSessions(): void;
  createSession(): void;
  selectSession(sessionId: string): void;
  deleteSession(sessionId: string): void;
}

export const SESSION_LIST = new InjectionToken<SessionList>('SessionList');
