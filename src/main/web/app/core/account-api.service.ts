import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.constants';

export interface AccountMe {
  mode: string;
  clientId: string;
  userId: string | null;
  email: string | null;
  plan: string;
}

@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly http = inject(HttpClient);

  me(): Observable<AccountMe> {
    return this.http.get<AccountMe>(`${API_BASE_URL}/account/me`);
  }
}
