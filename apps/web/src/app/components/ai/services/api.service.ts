import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { catchError } from 'rxjs/operators';



// ==================== Service URL ====================

const SPRINGAI_SERVICE_URL = '/api';

// ==================== Types ====================

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatRequest {
  messages: ChatMessage[];
  session_id?: string;
  system_prompt?: string;
  temperature?: number;
  max_tokens?: number;
}

export interface ChatResponse {
  response: string;
}

// ==================== API Service ====================

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // ==================== Health Check ====================

  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${SPRINGAI_SERVICE_URL}/health`).pipe(
      catchError(() => of({ status: 'DOWN' }))
    );
  }

  // ==================== Chat ====================

  chat(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${SPRINGAI_SERVICE_URL}/chat`, { message }).pipe(
      catchError(() => this.fallbackChat(message))
    );
  }

  /**
   * Simulates a chat response when backend is unavailable.
   * Provides contextual responses based on message content.
   */
  private fallbackChat(message: string): Observable<ChatResponse> {
    const lowerMessage = message.toLowerCase();

    let response: string;
    if (lowerMessage.includes('hello') || lowerMessage.includes('hi')) {
      response = 'Hello! I\'m your AI assistant running on SpringAI. ' +
                 'The backend service appears to be unavailable, so I\'m providing a simulated response.';
    } else if (lowerMessage.includes('help')) {
      response = 'I can help you with various tasks including answering questions, ' +
                 'writing code, analysis, and more. The backend is currently in fallback mode.';
    } else if (lowerMessage.includes('status')) {
      response = 'Backend service is currently unavailable. ' +
                 'This is a simulated response. Please check if the SpringAI service is running.';
    } else {
      response = `I received your message: "${message}". ` +
                 'The backend service is currently unavailable, so this is a simulated response. ' +
                 'Please ensure the SpringAI backend is running to get actual AI responses.';
    }

    return of({ response }).pipe(delay(500));
  }

  // ==================== Utility ====================

  downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  base64ToBlob(base64: string, mimeType: string = 'image/png'): Blob {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: mimeType });
  }
}
