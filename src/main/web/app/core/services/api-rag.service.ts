import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import type { DocumentListResponse, RagQuery, SourceDocument } from '@shared/models';
import { streamSsePost } from '@core/streaming/sse-client';
import { API_BASE_URL } from './api.constants';

@Injectable({ providedIn: 'root' })
export class ApiRagService {
  private http = inject(HttpClient);

  getDocuments(): Observable<DocumentListResponse> {
    return this.http
      .get<DocumentListResponse>(`${API_BASE_URL}/rag/documents`)
      .pipe(catchError(() => of({ documents: [] })));
  }

  uploadDocument(file: File, title?: string): Observable<{ id: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title ?? file.name);
    return this.http.post<{ id: string }>(`${API_BASE_URL}/rag/documents/upload`, formData);
  }

  deleteDocument(docId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/rag/documents/${docId}`);
  }

  ragChat(
    query: RagQuery,
    onChunk: (text: string) => void,
    onSources: (sources: SourceDocument[]) => void,
    onDone: () => void,
    onError: (err: Error) => void,
  ): { abort: () => void } {
    return streamSsePost(`${API_BASE_URL}/rag/chat/stream`, query, {
      onEvent: ({ eventType, data }) => {
        if (data === '[DONE]') {
          onDone();
          return true;
        }

        if (data.startsWith('Error:')) {
          onError(new Error(data.slice(6)));
          return true;
        }

        if (eventType === 'sources') {
          try {
            const sources = JSON.parse(data);
            onSources(Array.isArray(sources) ? sources : []);
          } catch {
            /* ignore */
          }
          return false;
        }

        const displayData = data.replace(/<br\s*\/?>/gi, '\n');
        onChunk(displayData);
        return false;
      },
      onDone,
      onError,
    });
  }
}
