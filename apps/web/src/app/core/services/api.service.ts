import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, catchError } from 'rxjs';
import {
  ChatRequest,
  RagQuery,
  ImageGenerateParams,
  TTSRequest,
  SourceDocument,
  ProviderInfo,
  ModelInfo,
  Voice,
  Detection,
  VisionResult,
  DocumentListResponse,
} from '@shared/models';

// ==================== Service URL ====================

const BASE_URL = '/api';

// ==================== Default Providers ====================

export const DEFAULT_PROVIDERS: ProviderInfo[] = [
  {
    name: 'openai',
    display_name: 'OpenAI',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
    status: 'available',
  },
  {
    name: 'anthropic',
    display_name: 'Anthropic Claude',
    models: ['claude-3-5-sonnet', 'claude-3-opus', 'claude-3-haiku'],
    status: 'available',
  },
  {
    name: 'ollama',
    display_name: 'Ollama (Local)',
    models: ['llama3', 'mistral', 'codellama'],
    status: 'unavailable',
  },
];

const defaultModels: Record<string, ModelInfo[]> = {
  anthropic: [
    { name: 'claude-3-5-sonnet', provider: 'anthropic' },
    { name: 'claude-3-opus', provider: 'anthropic' },
    { name: 'claude-3-haiku', provider: 'anthropic' },
  ],
  openai: [
    { name: 'gpt-4o', provider: 'openai' },
    { name: 'gpt-4o-mini', provider: 'openai' },
    { name: 'gpt-4-turbo', provider: 'openai' },
  ],
};

const defaultVoices: Voice[] = [
  {
    id: 'en-US',
    name: 'English (US)',
    language: 'en-US',
    provider: 'default',
    is_default: true,
  },
  {
    id: 'zh-CN',
    name: 'Chinese (Mandarin)',
    language: 'zh-CN',
    provider: 'default',
    is_default: false,
  },
];

// ==================== API Service ====================

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // ==================== Health Check ====================

  health(): Observable<{ status: string }> {
    return this.http
      .get<{ status: string }>(`${BASE_URL}/health`)
      .pipe(catchError(() => of({ status: 'DOWN' })));
  }

  // ==================== Text AI ====================

  getProviders(): Observable<ProviderInfo[]> {
    return this.http
      .get<ProviderInfo[]>(`${BASE_URL}/text/providers`)
      .pipe(catchError(() => of(DEFAULT_PROVIDERS)));
  }

  getModels(provider: string): Observable<ModelInfo[]> {
    return this.http
      .get<{ provider: string; models: ModelInfo[]; count: number }>(`${BASE_URL}/text/models`, {
        params: { provider },
      })
      .pipe(
        map((res) => res.models ?? []),
        catchError(() => of(defaultModels[provider] ?? defaultModels['openai'] ?? []))
      );
  }

  chatStream(
    request: ChatRequest,
    onChunk: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void
  ): { abort: () => void } {
    const controller = new AbortController();
    let currentEvent = '';

    const readerPromise = fetch(`${BASE_URL}/text/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal: controller.signal,
    }).then(async (response) => {
      if (!response.ok) {
        onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
        return;
      }

      if (!response.body) {
        onError(new Error('No response body'));
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      const processBuffer = () => {
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed) {
            currentEvent = '';
            continue;
          }

          if (trimmed.startsWith('event:')) {
            currentEvent = trimmed.slice(6).trim();
            continue;
          }

          if (trimmed.startsWith('data:')) {
            const data = trimmed.slice(5).trim();
            if (data === '[DONE]') {
              onDone();
              return true;
            }

            if (currentEvent === 'done') {
              onDone();
              return true;
            }

            if (currentEvent === 'error') {
              let msg = 'Stream error';
              try {
                const parsed = JSON.parse(data);
                msg = parsed.error ?? parsed.message ?? msg;
              } catch {
                /* ignore */
              }
              onError(new Error(msg));
              return true;
            }

              // Default: treat as token - decode preserved paragraph breaks
              let token: string | null = null;
              try {
                const parsed = JSON.parse(data);
                token = parsed.token ?? (typeof parsed === 'string' ? parsed : null);
              } catch {
                token = data;
              }
              if (token && token.length > 0) {
                // Decode \u0001\u0001 back to \n\n (paragraph break)
                token = token.replace(/\u0001\u0001/g, '\n\n');
                onChunk(token);
              }
          }
        }
        return false;
      };

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          if (processBuffer()) break;
        }
        onDone();
      } catch (err) {
        if ((err as Error).name !== 'AbortError') {
          onError(err as Error);
        }
      }
    });

    readerPromise.catch((err) => {
      if ((err as Error).name !== 'AbortError') {
        onError(err as Error);
      }
    });

    return {
      abort: () => controller.abort(),
    };
  }

  // ==================== RAG ====================

  getDocuments(): Observable<DocumentListResponse> {
    return this.http
      .get<DocumentListResponse>(`${BASE_URL}/rag/documents/`)
      .pipe(catchError(() => of({ documents: [] })));
  }

  uploadDocument(file: File, title?: string): Observable<{ id: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title ?? file.name);
    return this.http.post<{ id: string }>(`${BASE_URL}/rag/documents/upload`, formData);
  }

  deleteDocument(docId: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/rag/documents/${docId}`);
  }

  ragChat(
    query: RagQuery,
    onChunk: (text: string) => void,
    onSources: (sources: SourceDocument[]) => void,
    onDone: () => void,
    onError: (err: Error) => void
  ): { abort: () => void } {
    const controller = new AbortController();

    fetch(`${BASE_URL}/rag/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(query),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
          return;
        }

        if (!response.body) {
          onError(new Error('No response body'));
          return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const processBuffer = () => {
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) continue;

            // JSON-Lines format: strip SSE "data:" prefix, then expect a complete JSON object
            const jsonStr = trimmed.startsWith('data:') ? trimmed.slice(5).trim() : trimmed;

            // Handle [DONE] signal
            if (jsonStr === '[DONE]') {
              onDone();
              return;
            }

            if (!jsonStr.startsWith('{') || !jsonStr.endsWith('}')) {
              continue;
            }

            try {
              const parsed = JSON.parse(jsonStr);

              // Handle sources wrapper format: {"sources":[{...}]}
              if (Array.isArray(parsed.sources)) {
                onSources(parsed.sources);
                continue;
              }

              // Handle chunk format: {"type":"chunk","text":"..."}
              if (parsed.type === 'chunk' && typeof parsed.text === 'string') {
                onChunk(parsed.text);
                continue;
              }

              // Handle plain text chunk (legacy format)
              if (typeof parsed === 'string') {
                onChunk(parsed);
                continue;
              }

              // Handle error format: {"type":"error","message":"..."}
              if (parsed.type === 'error' && parsed.message) {
                onError(new Error(parsed.message));
                continue;
              }
            } catch {
              /* ignore parse errors */
            }
          }
        };

        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            processBuffer();
          }
          onDone();
        } catch (err) {
          if ((err as Error).name !== 'AbortError') {
            onError(err as Error);
          }
        }
      })
      .catch((err) => {
        if ((err as Error).name !== 'AbortError') {
          onError(err as Error);
        }
      });

    return {
      abort: () => controller.abort(),
    };
  }

  // ==================== Image Generation ====================

  generateImage(params: ImageGenerateParams): Observable<{ images: string[]; seed?: number }> {
    return this.http.post<{ images: string[]; seed?: number }>(
      `${BASE_URL}/image/generate`,
      params
    );
  }

  // ==================== Vision ====================

  captionImage(file: File): Observable<{ caption: string; processing_time_ms?: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ caption: string; processing_time_ms?: number }>(
      `${BASE_URL}/vision/caption`,
      formData
    );
  }

  detectObjects(file: File): Observable<{ detections: Detection[] }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ detections: Detection[] }>(`${BASE_URL}/vision/detect`, formData);
  }

  ocrImage(file: File): Observable<{ full_text: string; processing_time_ms?: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ full_text: string; processing_time_ms?: number }>(
      `${BASE_URL}/vision/ocr`,
      formData
    );
  }

  // ==================== TTS ====================

  getVoices(): Observable<Voice[]> {
    return this.http
      .get<Voice[]>(`${BASE_URL}/tts/voices`)
      .pipe(catchError(() => of(defaultVoices)));
  }

  synthesizeSpeech(params: TTSRequest): Observable<Blob> {
    return this.http.post<Blob>(`${BASE_URL}/tts/synthesize`, params, {
      responseType: 'blob' as 'json',
    });
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

  downloadBase64Image(base64: string, filename: string = 'image.png'): void {
    const mimeType = base64.startsWith('/9j/') ? 'image/jpeg' : 'image/png';
    const blob = this.base64ToBlob(base64, mimeType);
    this.downloadBlob(blob, filename);
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
