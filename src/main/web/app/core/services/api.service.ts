import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  ChatStreamRequest,
  RagQuery,
  ImageGenerateParams,
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
import { ApiChatService } from './api-chat.service';
import { ApiHealthService } from './api-health.service';
import { ApiMediaService } from './api-media.service';
import { ApiRagService } from './api-rag.service';

export { DEFAULT_PROVIDERS } from './api.constants';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private chatApi = inject(ApiChatService);
  private healthApi = inject(ApiHealthService);
  private mediaApi = inject(ApiMediaService);
  private ragApi = inject(ApiRagService);

  health(): Observable<{ status: string }> {
    return this.healthApi.health();
  }

  getProviders(): Observable<ProviderInfo[]> {
    return this.chatApi.getProviders();
  }

  getModels(provider: string): Observable<ModelInfo[]> {
    return this.chatApi.getModels(provider);
  }

  createSession(title?: string): Observable<SessionInfo> {
    return this.chatApi.createSession(title);
  }

  getSessions(): Observable<SessionInfo[]> {
    return this.chatApi.getSessions();
  }

  getSessionMessages(sessionId: string): Observable<ChatMessageData[]> {
    return this.chatApi.getSessionMessages(sessionId);
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.chatApi.deleteSession(sessionId);
  }

  chatStream(
    request: ChatStreamRequest,
    onChunk: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void,
  ): { abort: () => void } {
    return this.chatApi.chatStream(request, onChunk, onDone, onError);
  }

  getDocuments(): Observable<DocumentListResponse> {
    return this.ragApi.getDocuments();
  }

  uploadDocument(file: File, title?: string): Observable<{ id: string }> {
    return this.ragApi.uploadDocument(file, title);
  }

  deleteDocument(docId: string): Observable<void> {
    return this.ragApi.deleteDocument(docId);
  }

  ragChat(
    query: RagQuery,
    onChunk: (text: string) => void,
    onSources: (sources: SourceDocument[]) => void,
    onDone: () => void,
    onError: (err: Error) => void,
  ): { abort: () => void } {
    return this.ragApi.ragChat(query, onChunk, onSources, onDone, onError);
  }

  generateImage(params: ImageGenerateParams): Observable<ImageGenerationResult> {
    return this.mediaApi.generateImage(params);
  }

  captionImage(file: File): Observable<Pick<VisionResult, 'caption' | 'processingTimeMs'>> {
    return this.mediaApi.captionImage(file);
  }

  detectObjects(file: File): Observable<Pick<VisionResult, 'detections' | 'processingTimeMs'>> {
    return this.mediaApi.detectObjects(file);
  }

  ocrImage(file: File): Observable<Pick<VisionResult, 'fullText' | 'processingTimeMs'>> {
    return this.mediaApi.ocrImage(file);
  }

  getVoices(): Observable<Voice[]> {
    return this.mediaApi.getVoices();
  }

  synthesizeSpeech(params: TtsRequest): Observable<Blob> {
    return this.mediaApi.synthesizeSpeech(params);
  }

  downloadBlob(blob: Blob, filename: string): void {
    this.mediaApi.downloadBlob(blob, filename);
  }

  downloadBase64Image(base64: string, filename = 'image.png'): void {
    this.downloadBlob(this.base64ToBlob(base64), filename);
  }

  base64ToBlob(base64: string, mimeType = 'image/png'): Blob {
    return this.mediaApi.base64ToBlob(base64, mimeType);
  }
}
