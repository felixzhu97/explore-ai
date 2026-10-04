import { inject, Service, signal } from '@angular/core';
import { Instant } from '@js-joda/core';
import { HttpClient, type HttpEvent, HttpEventType } from '@angular/common/http';
import { type Observable, of, catchError, finalize } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import { parseSseToken, streamSsePost } from '../http/sse-client';
import { textOr } from '../shared/presence';

/** POST /api/rag/chat/stream */
export interface RagChatRequest {
  question: string;
  sessionId?: string;
  topK?: number;
  temperature?: number;
  documentIds?: string[];
  images?: string[];
}

/** Matches SourceDocumentResponse / SSE sources event */
export interface SourceDocument {
  id: string;
  content: string;
  score: number;
  metadata: Record<string, unknown>;
}

export type DocumentStatus = 'UPLOADING' | 'PROCESSING' | 'READY' | 'FAILED';

export interface DocumentSummaryResponse {
  id: string;
  title: string | null;
  status: DocumentStatus;
  createdAt: string;
  chunkCount: number;
}

export interface DocumentListResponse {
  documents: DocumentSummaryResponse[];
}

export interface UploadDocumentResponse {
  id: string;
  title: string | null;
  status: DocumentStatus;
  chunkCount: number;
  createdAt: string;
}

export interface RagDocumentItem {
  id: string;
  title: string;
}

export interface UploadStatus {
  id: string;
  title: string;
  status: 'uploading' | 'success' | 'error';
  progress?: number;
  error?: string;
}

export interface RagChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: SourceDocument[];
  timestamp: Instant;
}

const DEFAULT_TEMPERATURE = 0.7;
const DEFAULT_TOP_K = 5;

@Service()
export class RagService {
  readonly #http = inject(HttpClient);
  readonly #notifications = inject(NotificationService);
  readonly #i18n = inject(I18nService);
  readonly #sessionId = `session_${String(Instant.now().toEpochMilli())}`;

  // Document state
  readonly availableDocuments = signal<RagDocumentItem[]>([]);
  readonly selectedDocumentIds = signal<Set<string>>(new Set());
  readonly deletingDocumentIds = signal<Set<string>>(new Set());
  readonly isLoadingDocuments = signal(true);

  // Upload state
  readonly pendingFiles = signal<File[]>([]);
  readonly uploadStatuses = signal<Map<string, UploadStatus>>(new Map());
  readonly isUploading = signal(false);

  // Chat state
  readonly messages = signal<RagChatMessage[]>([]);
  readonly input = signal('');
  readonly isLoading = signal(false);

  // Streaming state
  readonly streamingMessageIds = signal<Set<string>>(new Set());

  fetchAvailableDocuments(): void {
    this.isLoadingDocuments.set(true);
    this.#getDocuments().subscribe({
      next: (data) => {
        const documents = data.documents.map(item => ({
          id: item.id,
          title: textOr(item.title, 'Untitled'),
        }));
        this.availableDocuments.set(documents);
        const ids = new Set<string>();
        documents.forEach(item => ids.add(item.id));
        this.selectedDocumentIds.set(ids);
      },
      error: () => {
        this.#notifications.showError(this.#i18n.t().common.errors.loadFailed);
        this.availableDocuments.set([]);
      },
      complete: () => {
        this.isLoadingDocuments.set(false);
      },
    });
  }

  toggleDocumentSelection(documentId: string): void {
    this.selectedDocumentIds.update((ids) => {
      const next = new Set(ids);
      if (next.has(documentId)) {
        next.delete(documentId);
      } else {
        next.add(documentId);
      }
      return next;
    });
  }

  selectAllDocuments(): void {
    const documents = this.availableDocuments();
    const ids = new Set<string>();
    documents.forEach(item => ids.add(item.id));
    this.selectedDocumentIds.set(ids);
  }

  clearDocumentSelection(): void {
    this.selectedDocumentIds.set(new Set());
  }

  deleteDocument(documentId: string): void {
    if (documentId === '' || documentId === 'undefined' || documentId === 'null') {
      this.#notifications.showError('Cannot delete: document ID is invalid');
      return;
    }

    this.deletingDocumentIds.update((ids) => {
      return new Set(ids).add(documentId);
    });

    this.#deleteDocumentRequest(documentId).subscribe({
      next: () => {
        setTimeout(() => {
          this.availableDocuments.update((documents) => {
            return documents.filter(item => item.id !== documentId);
          });
          this.selectedDocumentIds.update((ids) => {
            const next = new Set(ids);
            next.delete(documentId);
            return next;
          });
          this.deletingDocumentIds.update((ids) => {
            const next = new Set(ids);
            next.delete(documentId);
            return next;
          });
          this.#notifications.showSuccess(this.#i18n.t().rag.documentDeleted);
        }, 200);
      },
      error: () => {
        this.deletingDocumentIds.update((ids) => {
          const next = new Set(ids);
          next.delete(documentId);
          return next;
        });
        this.#notifications.showError(this.#i18n.t().rag.errors.deleteFailed);
      },
    });
  }

  onFileSelect(files: File[]): void {
    const newFiles = files.filter(
      f => !this.pendingFiles().some(pf => pf.name === f.name),
    );
    this.pendingFiles.update(prev => [...prev, ...newFiles]);
    this.#notifications.showInfo(
      this.#i18n.t().rag.fileSelected.replace('{count}', newFiles.length.toString()),
    );
  }

  removePendingFile(index: number): void {
    this.pendingFiles.update(files => files.filter((_, i) => i !== index));
  }

  getUploadStatus(filename: string): UploadStatus | undefined {
    return this.uploadStatuses().get(filename);
  }

  uploadFiles(): void {
    if (this.pendingFiles().length === 0) {
      return;
    }

    this.isUploading.set(true);
    let remaining = this.pendingFiles().length;
    const settle = () => {
      remaining -= 1;
      if (remaining === 0) {
        this.isUploading.set(false);
        this.fetchAvailableDocuments();
      }
    };

    this.pendingFiles().forEach((file, index) => {
      const documentId = `doc_${String(Instant.now().toEpochMilli())}_${Math.random().toString(36).slice(2, 11)}`;

      this.uploadStatuses.update((statuses) => {
        const next = new Map(statuses);
        next.set(file.name, {
          id: documentId,
          title: file.name,
          status: 'uploading',
          progress: 0,
        });
        return next;
      });

      this.#uploadDocument(file).pipe(finalize(settle)).subscribe({
        next: (event) => {
          if (
            event.type === HttpEventType.UploadProgress
            && event.total !== undefined
            && event.total > 0
          ) {
            const progress = Math.round((100 * event.loaded) / event.total);
            this.uploadStatuses.update((statuses) => {
              const next = new Map(statuses);
              const current = next.get(file.name);
              if (current !== undefined) {
                next.set(file.name, { ...current, progress });
              }
              return next;
            });
            return;
          }

          if (event.type !== HttpEventType.Response) {
            return;
          }

          this.uploadStatuses.update((statuses) => {
            const next = new Map(statuses);
            next.set(file.name, {
              id: documentId,
              title: file.name,
              status: 'success',
            });
            return next;
          });
          this.#notifications.showSuccess(
            this.#i18n.t().rag.uploadSuccess.replace('{name}', file.name),
          );

          if (index === this.pendingFiles().length - 1) {
            this.pendingFiles.set([]);
            setTimeout(() => {
              this.uploadStatuses.set(new Map());
            }, 2000);
          }
        },
        error: () => {
          this.uploadStatuses.update((statuses) => {
            const next = new Map(statuses);
            next.set(file.name, {
              id: documentId,
              title: file.name,
              status: 'error',
              error: this.#i18n.t().rag.errors.uploadFailed.replace('{name}', file.name),
            });
            return next;
          });
          this.#notifications.showError(
            this.#i18n.t().rag.errors.uploadFailed.replace('{name}', file.name),
          );
        },
      });
    });
  }

  // ==================== Chat ====================

  setInput(text: string): void {
    this.input.set(text);
  }

  sendMessage(): void {
    if (this.input().trim() === '') {
      return;
    }
    if (this.isLoading()) {
      return;
    }

    const now = Instant.now();
    const userMessage: RagChatMessage = {
      id: `user_${String(now.toEpochMilli())}`,
      role: 'user',
      content: this.input().trim(),
      timestamp: now,
    };

    this.messages.update(messages => [...messages, userMessage]);
    this.input.set('');
    this.isLoading.set(true);

    const assistantMessageId = `assistant_${String(now.toEpochMilli())}`;
    this.messages.update(messages => [
      ...messages,
      {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        timestamp: now,
      },
    ]);
    this.streamingMessageIds.update(ids => new Set(ids).add(assistantMessageId));

    const requestBody: RagChatRequest = {
      question: userMessage.content,
      sessionId: this.#sessionId,
      topK: DEFAULT_TOP_K,
      temperature: DEFAULT_TEMPERATURE,
    };

    if (this.selectedDocumentIds().size > 0) {
      requestBody.documentIds = Array.from(this.selectedDocumentIds());
    }

    this.#ragChat(
      requestBody,
      (chunk: string) => {
        this.messages.update(messages => messages.map((message) => {
          return message.id === assistantMessageId
            ? { ...message, content: message.content + chunk }
            : message;
        }));
      },
      (sources: SourceDocument[]) => {
        this.messages.update((messages) => {
          return messages.map((message) => {
            return message.id === assistantMessageId ? { ...message, sources } : message;
          });
        });
      },
      () => {
        this.streamingMessageIds.update((ids) => {
          const next = new Set(ids);
          next.delete(assistantMessageId);
          return next;
        });
        this.isLoading.set(false);
      },
      () => {
        this.messages.update((messages) => {
          return messages.map((message) => {
            return message.id === assistantMessageId
              ? { ...message, content: 'An error occurred while processing your request.' }
              : message;
          });
        });
        this.streamingMessageIds.update((ids) => {
          const next = new Set(ids);
          next.delete(assistantMessageId);
          return next;
        });
        this.isLoading.set(false);
      },
    );
  }

  #getDocuments(): Observable<DocumentListResponse> {
    return this.#http
      .get<DocumentListResponse>(`${API_BASE_URL}/rag/documents`)
      .pipe(catchError(() => of({ documents: [] })));
  }

  /** Needs app-level withXhr(); FetchBackend cannot emit upload progress (AI-249). */
  #uploadDocument(
    file: File,
    title?: string,
  ): Observable<HttpEvent<UploadDocumentResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title ?? file.name);
    return this.#http.post<UploadDocumentResponse>(`${API_BASE_URL}/rag/documents/upload`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  #deleteDocumentRequest(documentId: string): Observable<void> {
    return this.#http.delete<void>(`${API_BASE_URL}/rag/documents/${documentId}`);
  }

  #ragChat(
    query: RagChatRequest,
    onChunk: (text: string) => void,
    onSources: (sources: SourceDocument[]) => void,
    onDone: () => void,
    onError: (error: Error) => void,
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
            const sources: unknown = JSON.parse(data);
            onSources(Array.isArray(sources) ? sources as SourceDocument[] : []);
          } catch {
            /* ignore */
          }
          return false;
        }

        const token = parseSseToken(data);
        if (token !== null) {
          onChunk(token.replace(/<br\s*\/?>/gi, '\n'));
        }
        return false;
      },
      onDone,
      onError,
    });
  }
}
