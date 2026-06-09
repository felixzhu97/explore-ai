import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';



// ==================== Service URLs ====================

const TEXT_SERVICE_URL = '/api/text';
const VISION_SERVICE_URL = '/api/vision';
const RAG_SERVICE_URL = '/api/rag';
const SPEECH_SERVICE_URL = '/api/tts';
const MEDIA_GEN_SERVICE_URL = '/api/image';

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
  provider?: string;
  model?: string;
}

export interface ModelInfo {
  name: string;
  provider: string;
}

export interface ProviderInfo {
  name: string;
  display_name: string;
  models: string[];
  status: string;
}

export interface ImageGenerationRequest {
  prompt: string;
  negative_prompt?: string;
  width?: number;
  height?: number;
  num_images?: number;
}

export interface ImageGenerationResponse {
  images: string[];
  seed: number;
}

export interface Voice {
  id: string;
  name: string;
  language: string;
  provider: string;
  is_default: boolean;
}

export interface SynthesizeRequest {
  text: string;
  voice?: string;
  speed?: number;
  output_format?: 'mp3' | 'wav' | 'ogg' | 'flac';
}

export interface SourceDocument {
  text: string;
  score: number;
  metadata: Record<string, unknown>;
}

export interface Detection {
  class_name: string;
  confidence: number;
  bbox: [number, number, number, number];
}

// ==================== API Service ====================

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // ==================== Default Providers & Models ====================

  private readonly defaultProviders: ProviderInfo[] = [
    {
      name: 'openai',
      display_name: 'OpenAI',
      models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
      status: 'available',
    },
    {
      name: 'anthropic',
      display_name: 'Anthropic Claude',
      models: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-3-5-sonnet-20241022'],
      status: 'available',
    },
    {
      name: 'ollama',
      display_name: 'Ollama (Local)',
      models: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b', 'llama3.1:8b', 'mistral:7b'],
      status: 'available',
    },
  ];

  // ==================== Providers ====================

  getProviders(): Observable<ProviderInfo[]> {
    return this.http.get<ProviderInfo[]>(`${TEXT_SERVICE_URL}/providers`).pipe(
      catchError(() => {
        return new Observable<ProviderInfo[]>((subscriber) => {
          subscriber.next(this.defaultProviders);
          subscriber.complete();
        });
      })
    );
  }

  getModels(provider: string): Observable<ModelInfo[]> {
    return this.http.get<ModelInfo[]>(`${TEXT_SERVICE_URL}/models`, {
      params: { provider },
    }).pipe(
      catchError(() => {
        const providerData: Record<string, string[]> = {
          openai: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
          anthropic: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-3-5-sonnet-20241022'],
          ollama: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b', 'llama3.1:8b', 'mistral:7b'],
        };
        const modelList = providerData[provider] || providerData['openai'];
        return new Observable<ModelInfo[]>((subscriber) => {
          subscriber.next(modelList.map((name) => ({ name, provider })));
          subscriber.complete();
        });
      })
    );
  }

  // ==================== Chat Streaming ====================

  chatStream(
    request: ChatRequest,
    onChunk: (text: string) => void,
    onDone: () => void,
    onError: (error: Error) => void
  ): { abort: () => void } {
    const abortController = new AbortController();

    fetch(`${TEXT_SERVICE_URL}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal: abortController.signal,
    })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.body?.getReader();
      })
      .then((reader) => {
        if (!reader) throw new Error('Response body not available');

        const decoder = new TextDecoder();
        let buffer = '';
        let currentEvent = '';

        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              onDone();
              return;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
              if (line.startsWith('event: ')) {
                currentEvent = line.slice(7).trim();
              } else if (line.startsWith('data: ')) {
                const data = line.slice(6);

                if (data === '[DONE]') {
                  onDone();
                  break;
                }

                if (currentEvent === 'done') {
                  onDone();
                } else if (currentEvent === 'error') {
                  try {
                    const errorData = JSON.parse(data);
                    onError(new Error(errorData.error || 'Stream error'));
                  } catch {
                    onError(new Error('Stream error'));
                  }
                } else if (!currentEvent || currentEvent === 'meta') {
                  try {
                    const parsed = JSON.parse(data);
                    if (parsed.token) {
                      onChunk(parsed.token);
                    }
                  } catch {
                    // Skip non-JSON data
                  }
                }
              } else if (line.trim() === '') {
                currentEvent = '';
              }
            }

            if (!done) read();
          });
        };

        read();
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          onError(error);
        }
      });

    return {
      abort: () => abortController.abort(),
    };
  }

  // ==================== Image Generation ====================

  generateImage(request: ImageGenerationRequest): Observable<ImageGenerationResponse> {
    return this.http.post<ImageGenerationResponse>(`${MEDIA_GEN_SERVICE_URL}/generate`, request);
  }

  // ==================== TTS ====================

  getVoices(): Observable<Voice[]> {
    return this.http.get<Voice[]>(`${SPEECH_SERVICE_URL}/voices`).pipe(
      catchError(() => {
        return new Observable<Voice[]>((subscriber) => {
          subscriber.next([
            { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
            { id: 'en-GB', name: 'English (UK)', language: 'en-GB', provider: 'default', is_default: false },
            { id: 'zh-CN', name: '中文', language: 'zh-CN', provider: 'default', is_default: false },
            { id: 'ja-JP', name: '日本語', language: 'ja-JP', provider: 'default', is_default: false },
          ]);
          subscriber.complete();
        });
      })
    );
  }

  synthesizeSpeech(request: SynthesizeRequest): Observable<Blob> {
    return this.http.post(`${SPEECH_SERVICE_URL}/synthesize`, request, {
      responseType: 'blob',
    });
  }

  // ==================== Vision ====================

  captionImage(file: File): Observable<{ caption: string; processing_time_ms?: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ caption: string; processing_time_ms?: number }>(
      `${VISION_SERVICE_URL}/caption`,
      formData
    );
  }

  detectObjects(file: File): Observable<{
    detections: { class_name: string; confidence: number; bbox: [number, number, number, number] }[];
    processing_time_ms?: number;
  }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{
      detections: { class_name: string; confidence: number; bbox: [number, number, number, number] }[];
      processing_time_ms?: number;
    }>(`${VISION_SERVICE_URL}/detect`, formData);
  }

  ocrImage(file: File): Observable<{ full_text: string; processing_time_ms?: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ full_text: string; processing_time_ms?: number }>(
      `${VISION_SERVICE_URL}/ocr`,
      formData
    );
  }

  // ==================== RAG ====================

  getDocuments(): Observable<{ documents: { doc_id: string; filename: string }[] }> {
    return this.http.get<{ documents: { doc_id: string; filename: string }[] }>(
      `${RAG_SERVICE_URL}/documents/`
    ).pipe(
      catchError(() => of({ documents: [] }))
    );
  }

  uploadDocument(file: File): Observable<{ id: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', file.name);
    return this.http.post<{ id: string }>(`${RAG_SERVICE_URL}/documents/upload`, formData);
  }

  deleteDocument(docId: string): Observable<void> {
    return this.http.delete<void>(`${RAG_SERVICE_URL}/documents/${docId}`);
  }

  ragChat(
    request: {
      query: string;
      session_id: string;
      top_k?: number;
      temperature?: number;
      doc_ids?: string[];
    },
    onChunk: (text: string) => void,
    onSources: (sources: SourceDocument[]) => void,
    onDone: () => void,
    onError: (error: Error) => void
  ): { abort: () => void } {
    const abortController = new AbortController();

    fetch(`${RAG_SERVICE_URL}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal: abortController.signal,
    })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.body?.getReader();
      })
      .then((reader) => {
        if (!reader) throw new Error('Response body not available');

        const decoder = new TextDecoder();
        let buffer = '';
        let currentEvent = '';

        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              onDone();
              return;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
              if (line.startsWith('event: ')) {
                currentEvent = line.slice(7).trim();
              } else if (line.startsWith('data: ')) {
                const data = line.slice(6);

                if (data === '[DONE]') {
                  onDone();
                  break;
                } else if (currentEvent === 'sources') {
                  try {
                    onSources(JSON.parse(data));
                  } catch {
                    // Ignore parse errors
                  }
                  currentEvent = '';
                } else if (data.startsWith('Error:')) {
                  onError(new Error(data.slice(6)));
                  break;
                } else {
                  const text = data.replace(/<br>/g, '\n');
                  onChunk(text);
                }
              } else if (line.trim() === '') {
                currentEvent = '';
              }
            }

            if (!done) read();
          });
        };

        read();
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          onError(error);
        }
      });

    return {
      abort: () => abortController.abort(),
    };
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
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: 'image/png' });
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
