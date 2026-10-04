import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.constants';

export interface EvaluationRequest {
  userMessage: string;
  assistantResponse: string;
  referenceDocuments?: string[];
}

export interface EvaluationResponse {
  coherenceScore: number;
  relevanceScore: number;
  helpfulnessScore: number;
  factualityScore: number | null;
  factualityAvailable: boolean;
  overallScore: number;
  hasSafetyIssues: boolean;
  safetyFlags: string[];
  suggestions: string[];
}

@Injectable({ providedIn: 'root' })
export class EvalService {
  private readonly http = inject(HttpClient);

  evaluate(request: EvaluationRequest): Observable<EvaluationResponse> {
    return this.http.post<EvaluationResponse>(`${API_BASE_URL}/eval/chat`, request);
  }
}
