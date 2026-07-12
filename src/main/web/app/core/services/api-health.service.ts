import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import { API_BASE_URL } from './api.constants';

@Injectable({ providedIn: 'root' })
export class ApiHealthService {
  private http = inject(HttpClient);

  health(): Observable<{ status: string }> {
    return this.http
      .get<{ status: string }>(`${API_BASE_URL}/health`)
      .pipe(catchError(() => of({ status: 'DOWN' })));
  }
}
