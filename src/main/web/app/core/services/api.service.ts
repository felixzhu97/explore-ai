import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, catchError } from 'rxjs';
import type {
  ChatStreamRequest,
  RagQuery,
  ImageGenerateParams,
  ImageGenerationApiResponse,
  ImageGenerationResult,
  TtsRequest,
  SourceDocument,
  ProviderInfo,
  ModelInfo,
  Voice,
  DocumentListResponse,
  SessionInfo,
  ChatMessageData,
  VisionResult,
} from '@shared/models';
import { environment } from '@env/environment';

// ==================== Service URL ====================

const BASE_URL = environment.apiBaseUrl;

/** SSE data: JSON objects/strings vs plain token text (e.g. numeric chunks). */
function parseSseToken(data: string): string | null {
  if (data === '') {
    return '\n';
  }

  const first = data.trimStart()[0];
  if (first === '{' || first === '[') {
    try {
      const parsed = JSON.parse(data) as { token?: string | number };
      if (parsed && typeof parsed === 'object' && 'token' in parsed) {
        const token = parsed.token;
        if (token !== null && token !== undefined) {
          return String(token);
        }
      }
    } catch {
      return data;
    }
    return null;
  }

  if (first === '"') {
    try {
      const parsed = JSON.parse(data);
      return typeof parsed === 'string' ? parsed : data;
    } catch {
      return data;
    }
  }

  return data;
}

interface SseEventPayload {
  eventType: string;
  data: string;
}

/** Accumulates SSE data lines until a blank line, joining with `\n` per SSE spec. */
class SseEventAssembler {
  private eventType = '';
  private dataLines: string[] = [];

  pushLine(line: string): SseEventPayload | null {
    if (!line) {
      return this.flush();
    }

    if (line.startsWith('event:')) {
      this.eventType = line.slice(6).trim();
      return null;
    }

    if (line.startsWith('data:')) {
      let data = line.slice(5);
      if (data.startsWith(' ')) {
        data = data.slice(1);
      }
      this.dataLines.push(data);
    }

    return null;
  }

  flush(): SseEventPayload | null {
    if (this.dataLines.length === 0) {
      this.eventType = '';
      return null;
    }

    const payload: SseEventPayload = {
      eventType: this.eventType,
      data: this.dataLines.join('\n'),
    };
    this.dataLines = [];
    this.eventType = '';
    return payload;
  }
}

// ==================== Default Providers ====================

export const DEFAULT_PROVIDERS: ProviderInfo[] = [
  {
    name: 'openai',
    displayName: 'OpenAI',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
    status: 'available',
  },
  {
    name: 'anthropic',
    displayName: 'Anthropic Claude',
    models: ['claude-3-5-sonnet', 'claude-3-opus', 'claude-3-haiku'],
    status: 'available',
  },
  {
    name: 'ollama',
    displayName: 'Ollama (Local)',
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
    id: 'alloy',
    name: 'Alloy',
    language: 'en',
    provider: 'openai',
    isDefault: true,
  },
  {
    id: 'nova',
    name: 'Nova',
    language: 'en',
    provider: 'openai',
    isDefault: false,
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
        map(res => res.models ?? []),
        catchError(() => of(defaultModels[provider] ?? defaultModels['openai'] ?? [])),
      );
  }

  createSession(title?: string): Observable<SessionInfo> {
    return this.http.post<SessionInfo>(`${BASE_URL}/sessions`, title ? { title } : {});
  }

  getSessions(): Observable<SessionInfo[]> {
    return this.http.get<SessionInfo[]>(`${BASE_URL}/sessions`);
  }

  getSessionMessages(sessionId: string): Observable<ChatMessageData[]> {
    return this.http.get<ChatMessageData[]>(`${BASE_URL}/sessions/${sessionId}/messages`).pipe(
      map(messages => messages.map(msg => ({
        ...msg,
        timestamp: new Date(msg.timestamp as unknown as string).getTime(),
      })),
      ),
    );
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/sessions/${sessionId}`);
  }

  chatStream(
    request: ChatStreamRequest,
    onChunk: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void,
  ): { abort: () => void } {
    const controller = new AbortController();
    const sseAssembler = new SseEventAssembler();

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

      const processBuffer = (flushPending = false) => {
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const rawLine of lines) {
          const line = rawLine.replace(/\r$/, '');
          const event = sseAssembler.pushLine(line);
          if (event !== null && handleSseEvent(event)) {
            return true;
          }
        }

        if (flushPending) {
          const event = sseAssembler.flush();
          if (event !== null && handleSseEvent(event)) {
            return true;
          }
        }

        return false;
      };

      const handleSseEvent = (event: SseEventPayload): boolean => {
        const { eventType, data } = event;

        if (data === '[DONE]') {
          finish();
          return true;
        }

        if (eventType === 'done') {
          finish();
          return true;
        }

        if (eventType === 'error') {
          let msg = 'Stream error';
          try {
            const parsed = JSON.parse(data);
            msg = parsed.error ?? parsed.message ?? msg;
          } catch {
            if (data) {
              msg = data;
            }
          }
          onError(new Error(msg));
          return true;
        }

        const token = parseSseToken(data);
        if (token !== null) {
          onChunk(token);
        }
        return false;
      };

      let finished = false;
      const finish = () => {
        if (!finished) {
          finished = true;
          onDone();
        }
      };

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (value) {
            buffer += decoder.decode(value, { stream: !done });
          }
          if (processBuffer()) {
            finished = true;
            break;
          }
          if (done) {
            buffer += decoder.decode(undefined, { stream: false });
            processBuffer(true);
            break;
          }
        }
        finish();
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
      .get<DocumentListResponse>(`${BASE_URL}/rag/documents`)
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
    onError: (err: Error) => void,
  ): { abort: () => void } {
    const controller = new AbortController();
    let currentEvent = '';

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

              if (data.startsWith('Error:')) {
                onError(new Error(data.slice(6)));
                return true;
              }

              if (currentEvent === 'sources') {
                try {
                  const sources = JSON.parse(data);
                  onSources(Array.isArray(sources) ? sources : []);
                } catch {
                  /* ignore */
                }
                currentEvent = ''; // Reset after processing sources
                continue;
              }

              // Replace <br> with newlines for chunk data
              const displayData = data.replace(/<br\s*\/?>/gi, '\n');
              if (displayData !== data) {
                onChunk(displayData);
              } else {
                onChunk(data);
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

  generateImage(
    params: ImageGenerateParams,
  ): Observable<ImageGenerationResult> {
    return this.http.post<ImageGenerationApiResponse>(
      `${BASE_URL}/images/generate`,
      {
        prompt: params.prompt,
        model: params.model,
        quality: params.quality,
        width: params.width,
        height: params.height,
        n: params.n ?? 1,
      },
    ).pipe(
      map(response => ({
        imageUrl: response.imageUrl ?? undefined,
        imageBase64: response.imageBase64 ?? undefined,
        model: response.model,
        prompt: response.prompt,
      })),
    );
  }

  // ==================== Vision ====================

  captionImage(file: File): Observable<Pick<VisionResult, 'caption' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'caption' | 'processingTimeMs'>>(
      `${BASE_URL}/vision/caption`,
      formData,
    );
  }

  detectObjects(file: File): Observable<Pick<VisionResult, 'detections'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'detections'>>(`${BASE_URL}/vision/detect`, formData);
  }

  ocrImage(file: File): Observable<Pick<VisionResult, 'fullText' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'fullText' | 'processingTimeMs'>>(
      `${BASE_URL}/vision/ocr`,
      formData,
    );
  }

  // ==================== TTS ====================

  getVoices(): Observable<Voice[]> {
    return this.http
      .get<{ voices: (Voice | string)[] }>(`${BASE_URL}/audio/voices`)
      .pipe(
        map(response => this.normalizeVoices(response.voices)),
        catchError(() => of(defaultVoices)),
      );
  }

  synthesizeSpeech(params: TtsRequest): Observable<Blob> {
    return this.http.post<Blob>(
      `${BASE_URL}/audio/speak`,
      {
        text: params.text,
        voice: params.voice,
        speed: params.speed,
        outputFormat: params.outputFormat,
      },
      {
        responseType: 'blob' as 'json',
      },
    );
  }

  private normalizeVoices(voices: (Voice | string)[]): Voice[] {
    if (!voices?.length) {
      return defaultVoices;
    }

    return voices.map((voice, index) => {
      if (typeof voice === 'string') {
        return {
          id: voice,
          name: voice.charAt(0).toUpperCase() + voice.slice(1),
          language: 'en',
          provider: 'openai',
          isDefault: index === 0,
        };
      }
      return {
        ...voice,
        provider: voice.provider ?? 'openai',
        isDefault: voice.isDefault ?? index === 0,
      };
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

  downloadBase64Image(base64: string, filename = 'image.png'): void {
    const mimeType = base64.startsWith('/9j/') ? 'image/jpeg' : 'image/png';
    const blob = this.base64ToBlob(base64, mimeType);
    this.downloadBlob(blob, filename);
  }

  base64ToBlob(base64: string, mimeType = 'image/png'): Blob {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: mimeType });
  }
}
