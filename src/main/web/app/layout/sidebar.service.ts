import { Injectable, signal, effect } from '@angular/core';

export interface Session {
  id: string;
  title: string;
  timestamp: Date;
  pinned: boolean;
}

const STORAGE_KEY = 'sidebar_sessions';

interface StoredSession {
  id: string;
  title: string;
  timestamp: string;
  pinned: boolean;
}

interface StorageData {
  pinned: StoredSession[];
  recent: StoredSession[];
  activeId: string | null;
}

@Injectable({ providedIn: 'root' })
export class SidebarService {
  readonly mobileOpen = signal(false);
  readonly collapsed = signal(false);

  private readonly _pinnedSessions = signal<Session[]>([]);
  private readonly _recentSessions = signal<Session[]>([]);
  private readonly _activeSessionId = signal<string | null>(null);

  readonly pinnedSessions = this._pinnedSessions.asReadonly();
  readonly recentSessions = this._recentSessions.asReadonly();
  readonly activeSessionId = this._activeSessionId.asReadonly();

  constructor() {
    this.loadFromStorage();
    this.setupStorageSync();
  }

  open() {
    this.mobileOpen.set(true);
  }

  close() {
    this.mobileOpen.set(false);
  }

  toggle() {
    this.mobileOpen.update(v => !v);
  }

  addSession(title?: string): Session {
    const session: Session = {
      id: crypto.randomUUID(),
      title: title || this.generateTitle(),
      timestamp: new Date(),
      pinned: false,
    };

    this._recentSessions.update(sessions => [session, ...sessions]);
    this.setActiveSession(session.id);
    this.saveToStorage();
    return session;
  }

  removeSession(id: string): void {
    this._pinnedSessions.update(
      sessions => sessions.filter((s: Session) => s.id !== id),
    );
    this._recentSessions.update(
      sessions => sessions.filter((s: Session) => s.id !== id),
    );
    if (this._activeSessionId() === id) {
      this._activeSessionId.set(null);
    }
    this.saveToStorage();
  }

  togglePin(id: string): void {
    const pinned = this._pinnedSessions().find((s: Session) => s.id === id);
    const recent = this._recentSessions().find((s: Session) => s.id === id);

    if (pinned) {
      this._pinnedSessions.update(
        sessions => sessions.filter((s: Session) => s.id !== id),
      );
      this._recentSessions.update(
        sessions => [{ ...pinned, pinned: false }, ...sessions],
      );
    } else if (recent) {
      this._recentSessions.update(
        sessions => sessions.filter((s: Session) => s.id !== id),
      );
      this._pinnedSessions.update(
        sessions => [{ ...recent, pinned: true }, ...sessions],
      );
    }
    this.saveToStorage();
  }

  setActiveSession(id: string | null): void {
    this._activeSessionId.set(id);
    if (id) {
      const session = [...this._pinnedSessions(), ...this._recentSessions()]
        .find((s: Session) => s.id === id);
      if (session) {
        this.updateSessionTimestamp(id);
      }
    }
  }

  getActiveSession(): Session | null {
    const id = this._activeSessionId();
    if (!id) return null;
    return [...this._pinnedSessions(), ...this._recentSessions()]
      .find((s: Session) => s.id === id) || null;
  }

  updateSessionTitle(id: string, title: string): void {
    this._pinnedSessions.update(sessions => sessions.map(
      (s: Session) => s.id === id ? { ...s, title } : s,
    ));
    this._recentSessions.update(sessions => sessions.map(
      (s: Session) => s.id === id ? { ...s, title } : s,
    ));
    this.saveToStorage();
  }

  private updateSessionTimestamp(id: string): void {
    const timestamp = new Date();
    this._recentSessions.update(sessions => sessions
      .map((s: Session) => s.id === id ? { ...s, timestamp } : s)
      .sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime()),
    );
    this.saveToStorage();
  }

  private generateTitle(): string {
    const count = this._recentSessions().length
      + this._pinnedSessions().length
      + 1;
    return `New Chat ${count}`;
  }

  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const data: StorageData = JSON.parse(stored);
        this._pinnedSessions.set(
          (data.pinned || []).map((s: StoredSession) => ({
            ...s,
            timestamp: new Date(s.timestamp),
          })),
        );
        this._recentSessions.set(
          (data.recent || []).map((s: StoredSession) => ({
            ...s,
            timestamp: new Date(s.timestamp),
          })),
        );
        this._activeSessionId.set(data.activeId || null);
      }
    } catch (e) {
      console.warn('Failed to load sessions from storage:', e);
    }
  }

  private saveToStorage(): void {
    try {
      const data: StorageData = {
        pinned: this._pinnedSessions().map((s: Session) => ({
          id: s.id,
          title: s.title,
          timestamp: s.timestamp.toISOString(),
          pinned: s.pinned,
        })),
        recent: this._recentSessions().map((s: Session) => ({
          id: s.id,
          title: s.title,
          timestamp: s.timestamp.toISOString(),
          pinned: s.pinned,
        })),
        activeId: this._activeSessionId(),
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch (e) {
      console.warn('Failed to save sessions to storage:', e);
    }
  }

  private setupStorageSync(): void {
    effect(() => {
      this.saveToStorage();
    });
  }
}
