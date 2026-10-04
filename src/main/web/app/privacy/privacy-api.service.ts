import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';

@Service()
export class PrivacyApiService {
  readonly #http = inject(HttpClient);

  eraseAllSessions(): Observable<void> {
    return this.#http.delete<void>(`${API_BASE_URL}/privacy/sessions`);
  }

  resetIdentity(): Observable<void> {
    return this.#http.post<void>(`${API_BASE_URL}/privacy/reset-identity`, null);
  }
}
