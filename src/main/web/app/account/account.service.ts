import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ChatService } from '../chat/chat.service';
import { API_BASE_URL } from '../http/api.constants';
import { STORAGE_KEYS } from '../storage-keys';
import { I18nService } from '../i18n';
import { NotificationService } from '../ui/notification.service';

export type OAuthProviderId = 'google' | 'github' | 'explore-iam';

export interface AccountMe {
  mode: string;
  clientId: string;
  userId: string | null;
  email: string | null;
  plan: string;
  loginAvailable: boolean;
  loginProviders: OAuthProviderId[];
}

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

  private readonly accountState = signal<AccountMe | null>(null);
  private readonly isLoadingState = signal(false);
  private readonly isLoadedState = signal(false);

  readonly account = this.accountState.asReadonly();
  readonly isLoading = this.isLoadingState.asReadonly();
  readonly isLoaded = this.isLoadedState.asReadonly();

  readonly isAuthenticated = computed(() => this.accountState()?.mode === 'authenticated');
  readonly loginAvailable = computed(() => !!this.accountState()?.loginAvailable);
  readonly loginProviders = computed(() => this.accountState()?.loginProviders ?? []);
  readonly showLogin = computed(
    () => !!this.accountState()?.loginAvailable && this.accountState()?.mode !== 'authenticated',
  );

  readonly showLogout = computed(() => this.accountState()?.mode === 'authenticated');

  load(): void {
    if (this.isLoadingState()) {
      return;
    }
    this.isLoadingState.set(true);
    this.http
      .get<AccountMe>(`${API_BASE_URL}/account/me`)
      .pipe(finalize(() => this.isLoadingState.set(false)))
      .subscribe({
        next: (me) => {
          this.accountState.set(me);
          this.isLoadedState.set(true);
        },
        error: () => {
          this.accountState.set(null);
          this.isLoadedState.set(true);
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
    sessionStorage.setItem(STORAGE_KEYS.OAUTH_RETURN_URL, returnTo || '/chat');
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
        this.notifications.showError(this.i18n.t().account.errors.logoutFailed);
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
      if (!this.isLoadedState()) {
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
      const returnTo = sessionStorage.getItem(STORAGE_KEYS.OAUTH_RETURN_URL);
      sessionStorage.removeItem(STORAGE_KEYS.OAUTH_RETURN_URL);
      if (returnTo && returnTo !== cleanUrl) {
        void this.router.navigateByUrl(returnTo);
      }
    } else {
      this.notifications.showError(this.i18n.t().account.errors.loginFailed);
      sessionStorage.removeItem(STORAGE_KEYS.OAUTH_RETURN_URL);
    }
  }
}
