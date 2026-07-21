import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type { EvaluationRequest, EvaluationResponse } from './eval.model';

@Injectable({ providedIn: 'root' })
export class EvalService {
  private readonly http = inject(HttpClient);

  evaluate(request: EvaluationRequest): Observable<EvaluationResponse> {
    return this.http.post<EvaluationResponse>(`${API_BASE_URL}/eval/chat`, request);
  }
}
