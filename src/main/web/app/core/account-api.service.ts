import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.constants';

export type OAuthProviderId = 'google' | 'github';

export interface AccountMe {
  mode: string;
  clientId: string;
  userId: string | null;
  email: string | null;
  plan: string;
  loginAvailable: boolean;
  loginProviders: OAuthProviderId[];
}

/** Thin HTTP client for account endpoints. Prefer {@link AccountService} in UI. */
@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly http = inject(HttpClient);

  me(): Observable<AccountMe> {
    return this.http.get<AccountMe>(`${API_BASE_URL}/account/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/account/logout`, {});
  }
}
