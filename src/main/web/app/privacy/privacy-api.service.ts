import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

@Injectable({ providedIn: 'root' })
export class PrivacyApiService {
  private readonly http = inject(HttpClient);

  eraseAllSessions(): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/privacy/sessions`);
  }

  resetIdentity(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/privacy/reset-identity`, null);
  }
}
