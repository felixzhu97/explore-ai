import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ChatService } from '../chat/chat.service';
import { API_BASE_URL } from './api.constants';
import type { AccountMe, OAuthProviderId } from './account-api.service';
import { I18nService } from './i18n';
import { NotificationService } from './notification.service';

const OAUTH_RETURN_KEY = 'ea_oauth_return';

/**
 * Shared account state for guest + optional OAuth (Google / GitHub).
 * Login uses a full-page redirect; return is signaled via `?login=`.
 */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly i18n = inject(I18nService);
  private readonly chat = inject(ChatService);

  private readonly accountSignal = signal<AccountMe | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly loadedSignal = signal(false);

  readonly account = this.accountSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly loaded = this.loadedSignal.asReadonly();

  readonly isAuthenticated = computed(() => this.accountSignal()?.mode === 'authenticated');
  readonly loginAvailable = computed(() => !!this.accountSignal()?.loginAvailable);
  readonly loginProviders = computed(() => this.accountSignal()?.loginProviders ?? []);
  readonly showLogin = computed(
    () => !!this.accountSignal()?.loginAvailable && this.accountSignal()?.mode !== 'authenticated',
  );

  readonly showLogout = computed(() => this.accountSignal()?.mode === 'authenticated');

  load(): void {
    if (this.loadingSignal()) {
      return;
    }
    this.loadingSignal.set(true);
    this.http
      .get<AccountMe>(`${API_BASE_URL}/account/me`)
      .pipe(finalize(() => this.loadingSignal.set(false)))
      .subscribe({
        next: (me) => {
          this.accountSignal.set(me);
          this.loadedSignal.set(true);
        },
        error: () => {
          this.accountSignal.set(null);
          this.loadedSignal.set(true);
        },
      });
  }

  reload(): void {
    this.load();
  }

  /**
   * Full navigation so the browser follows OAuth redirects with cookies.
   * @param assign injectable for tests (defaults to {@code location.assign})
   */
  startOAuthLogin(
    provider: OAuthProviderId,
    assign: (url: string) => void = url => window.location.assign(url),
  ): void {
    const { pathname, search, hash } = window.location;
    const returnTo = `${pathname}${search}${hash}`;
    sessionStorage.setItem(OAUTH_RETURN_KEY, returnTo || '/chat');
    assign(`/oauth2/authorization/${provider}`);
  }

  /** @deprecated Prefer {@link startOAuthLogin} */
  startGoogleLogin(
    assign: (url: string) => void = url => window.location.assign(url),
  ): void {
    this.startOAuthLogin('google', assign);
  }

  logout(): void {
    this.http.post<void>(`${API_BASE_URL}/account/logout`, {}).subscribe({
      next: () => {
        this.reload();
        this.chat.resetForOwnerChange();
        this.notifications.showSuccess(this.i18n.t().account.logoutSuccess);
      },
      error: () => {
        this.reload();
        this.chat.resetForOwnerChange();
        this.notifications.showError(this.i18n.t().account.logoutFailed);
      },
    });
  }

  /**
   * After OAuth redirect, backend sends `?login=success|error`.
   * Show toast, restore path, and refresh `/api/account/me`.
   */
  /**
   * After OAuth redirect, backend sends {@code ?login=success|error}.
   * @param search injectable for tests (defaults to {@code location.search})
   * @param path injectable for tests (defaults to {@code location.pathname})
   */
  consumeLoginReturn(
    search: string = window.location.search,
    path: string = window.location.pathname,
  ): void {
    const params = new URLSearchParams(search);
    const login = params.get('login');
    if (!login) {
      if (!this.loadedSignal()) {
        this.load();
      }
      return;
    }

    params.delete('login');
    const query = params.toString();
    const cleanUrl = query ? `${path}?${query}` : path;
    void this.router.navigateByUrl(cleanUrl, { replaceUrl: true });

    this.reload();
    this.chat.resetForOwnerChange();

    if (login === 'success') {
      this.notifications.showSuccess(this.i18n.t().account.loginSuccess);
      const returnTo = sessionStorage.getItem(OAUTH_RETURN_KEY);
      sessionStorage.removeItem(OAUTH_RETURN_KEY);
      if (returnTo && returnTo !== cleanUrl) {
        void this.router.navigateByUrl(returnTo);
      }
    } else {
      this.notifications.showError(this.i18n.t().account.loginFailed);
      sessionStorage.removeItem(OAUTH_RETURN_KEY);
    }
  }
}
