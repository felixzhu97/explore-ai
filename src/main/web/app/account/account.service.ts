import { HttpClient } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ChatService } from '../chat/chat.service';
import { API_BASE_URL } from '../http/api.constants';
import { STORAGE_KEYS } from '../storage-keys';
import { I18nService } from '../i18n';
import { NotificationService } from '../ui/notification.service';
import { hasText, textOr } from '../shared/presence';

export type LoginProvider = 'google' | 'github' | 'explore-iam';
export type AccountMode = 'anonymous' | 'authenticated';
export type AccountPlan = 'free' | 'pro';

export interface AccountMeResponse {
  mode: AccountMode;
  clientId: string | null;
  userId: string | null;
  email: string | null;
  /** Name to show for the account; may be a login handle rather than an email. */
  displayName: string | null;
  plan: AccountPlan;
  loginAvailable: boolean;
  loginProviders: LoginProvider[];
}

/**
 * Shared account state for guest + optional OAuth (Google / GitHub).
 * Login uses a full-page redirect; return is signaled via `?login=`.
 */
@Service()
export class AccountService {
  readonly #http = inject(HttpClient);
  readonly #router = inject(Router);
  readonly #notifications = inject(NotificationService);
  readonly #i18n = inject(I18nService);
  readonly #chat = inject(ChatService);

  readonly #accountState = signal<AccountMeResponse | null>(null);
  readonly #isLoadingState = signal(false);
  readonly #isLoadedState = signal(false);

  readonly account = this.#accountState.asReadonly();
  readonly isLoading = this.#isLoadingState.asReadonly();
  readonly isLoaded = this.#isLoadedState.asReadonly();

  readonly isAuthenticated = computed(() => this.#accountState()?.mode === 'authenticated');
  readonly loginAvailable = computed(() => this.#accountState()?.loginAvailable === true);
  readonly loginProviders = computed(() => this.#accountState()?.loginProviders ?? []);
  readonly showLogin = computed(
    () => this.#accountState()?.loginAvailable === true && this.#accountState()?.mode !== 'authenticated',
  );

  readonly showLogout = computed(() => this.#accountState()?.mode === 'authenticated');

  /** Loads the current account unless a load is already running. */
  load(): void {
    if (this.#isLoadingState()) {
      return;
    }
    this.#isLoadingState.set(true);
    this.#http
      .get<AccountMeResponse>(`${API_BASE_URL}/account/me`)
      .pipe(finalize(() => this.#isLoadingState.set(false)))
      .subscribe({
        next: (me) => {
          this.#accountState.set(me);
          this.#isLoadedState.set(true);
        },
        error: () => {
          this.#accountState.set(null);
          this.#isLoadedState.set(true);
        },
      });
  }

  /** Loads the current account again. */
  reload(): void {
    this.load();
  }

  /**
   * Full navigation so the browser follows OAuth redirects with cookies.
   * @param assign injectable for tests (defaults to {@code location.assign})
   */
  startOAuthLogin(
    provider: LoginProvider,
    assign: (url: string) => void = url => window.location.assign(url),
  ): void {
    const { pathname, search, hash } = window.location;
    const returnTo = `${pathname}${search}${hash}`;
    sessionStorage.setItem(STORAGE_KEYS.OAUTH_RETURN_URL, textOr(returnTo, '/chat'));
    assign(`/oauth2/authorization/${provider}`);
  }

  /** @deprecated Prefer {@link startOAuthLogin} */
  startGoogleLogin(
    assign: (url: string) => void = url => window.location.assign(url),
  ): void {
    this.startOAuthLogin('google', assign);
  }

  /** Signs out and reloads the account. */
  logout(): void {
    this.#http.post<void>(`${API_BASE_URL}/account/logout`, {}).subscribe({
      next: () => {
        this.reload();
        this.#chat.resetForOwnerChange();
        this.#notifications.showSuccess(this.#i18n.t().account.logoutSuccess);
      },
      error: () => {
        this.reload();
        this.#chat.resetForOwnerChange();
        this.#notifications.showError(this.#i18n.t().account.errors.logoutFailed);
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
    if (!hasText(login)) {
      if (!this.#isLoadedState()) {
        this.load();
      }
      return;
    }

    params.delete('login');
    const query = params.toString();
    const cleanUrl = query !== '' ? `${path}?${query}` : path;
    void this.#router.navigateByUrl(cleanUrl, { replaceUrl: true });

    this.reload();
    this.#chat.resetForOwnerChange();

    if (login === 'success') {
      this.#notifications.showSuccess(this.#i18n.t().account.loginSuccess);
      const returnTo = sessionStorage.getItem(STORAGE_KEYS.OAUTH_RETURN_URL);
      sessionStorage.removeItem(STORAGE_KEYS.OAUTH_RETURN_URL);
      if (hasText(returnTo) && returnTo !== cleanUrl) {
        void this.#router.navigateByUrl(returnTo);
      }
    } else {
      this.#notifications.showError(this.#i18n.t().account.errors.loginFailed);
      sessionStorage.removeItem(STORAGE_KEYS.OAUTH_RETURN_URL);
    }
  }
}
