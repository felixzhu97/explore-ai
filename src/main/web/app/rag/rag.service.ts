import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import { NotificationService } from '../core/notification.service';
import { I18nService } from '../core/i18n';
import { streamSsePost } from '../core/streaming/sse-client';
import { SourceDocument, RagQuery, DocumentListResponse } from './rag.model';

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
  timestamp: number;
}

const DEFAULT_TEMPERATURE = 0.7;
const DEFAULT_TOP_K = 5;

@Injectable({ providedIn: 'root' })
export class RagService {
  private readonly http = inject(HttpClient);
  private readonly notifications = inject(NotificationService);
  private readonly i18n = inject(I18nService);
  private readonly sessionId = `session_${Date.now()}`;

  // Document state
  readonly availableDocs = signal<RagDocumentItem[]>([]);
  readonly selectedDocIds = signal<Set<string>>(new Set());
  readonly deletingDocIds = signal<Set<string>>(new Set());
  readonly isLoadingDocs = signal(true);

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

  fetchAvailableDocs(): void {
    this.isLoadingDocs.set(true);
    this.getDocuments().subscribe({
      next: (data) => {
        const docs = (data.documents || []).map(d => ({
          id: d.id,
          title: d.title || 'Untitled',
        }));
        this.availableDocs.set(docs);
        const ids = new Set<string>();
        docs.forEach(d => ids.add(d.id));
        this.selectedDocIds.set(ids);
      },
      error: () => {
        this.notifications.showError(this.i18n.t().common.loadFailed);
        this.availableDocs.set([]);
      },
      complete: () => {
        this.isLoadingDocs.set(false);
      },
    });
  }

  toggleDocSelection(docId: string): void {
    this.selectedDocIds.update((ids) => {
      const next = new Set(ids);
      if (next.has(docId)) {
        next.delete(docId);
      } else {
        next.add(docId);
      }
      return next;
    });
  }

  selectAllDocs(): void {
    const docs = this.availableDocs();
    const ids = new Set<string>();
    docs.forEach(d => ids.add(d.id));
    this.selectedDocIds.set(ids);
  }

  clearDocSelection(): void {
    this.selectedDocIds.set(new Set());
  }

  deleteDocument(docId: string): void {
    if (!docId || docId === 'undefined' || docId === 'null') {
      this.notifications.showError('Cannot delete: document ID is invalid');
      return;
    }

    this.deletingDocIds.update((ids) => {
      return new Set(ids).add(docId);
    });

    this.deleteDocumentRequest(docId).subscribe({
      next: () => {
        setTimeout(() => {
          this.availableDocs.update(docs => docs.filter(d => d.id !== docId));
          this.selectedDocIds.update((ids) => {
            const next = new Set(ids);
            next.delete(docId);
            return next;
          });
          this.deletingDocIds.update((ids) => {
            const next = new Set(ids);
            next.delete(docId);
            return next;
          });
          this.notifications.showSuccess(this.i18n.t().ragChat.documentDeleted);
        }, 200);
      },
      error: () => {
        this.deletingDocIds.update((ids) => {
          const next = new Set(ids);
          next.delete(docId);
          return next;
        });
        this.notifications.showError(this.i18n.t().ragChat.deleteFailed);
      },
    });
  }

  onFileSelect(files: File[]): void {
    const newFiles = files.filter(
      f => !this.pendingFiles().some(pf => pf.name === f.name),
    );
    this.pendingFiles.update(prev => [...prev, ...newFiles]);
    this.notifications.showInfo(
      this.i18n.t().ragChat.fileSelected.replace('{count}', newFiles.length.toString()),
    );
  }

  removePendingFile(index: number): void {
    this.pendingFiles.update(files => files.filter((_, i) => i !== index));
  }

  getUploadStatus(filename: string): UploadStatus | undefined {
    return this.uploadStatuses().get(filename);
  }

  uploadFiles(onComplete?: () => void): void {
    if (this.pendingFiles().length === 0) return;

    this.isUploading.set(true);

    this.pendingFiles().forEach((file, index) => {
      const docId = `doc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      this.uploadStatuses.update((statuses) => {
        const next = new Map(statuses);
        next.set(file.name, {
          id: docId,
          title: file.name,
          status: 'uploading',
          progress: 0,
        });
        return next;
      });

      this.uploadDocument(file).subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            const progress = Math.round((100 * event.loaded) / event.total);
            this.uploadStatuses.update((statuses) => {
              const next = new Map(statuses);
              const current = next.get(file.name);
              if (current) {
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
              id: docId,
              title: file.name,
              status: 'success',
            });
            return next;
          });
          this.notifications.showSuccess(
            this.i18n.t().ragChat.uploadSuccess.replace('{name}', file.name),
          );

          if (index === this.pendingFiles().length - 1) {
            this.pendingFiles.set([]);
            this.fetchAvailableDocs();
            setTimeout(() => {
              this.uploadStatuses.set(new Map());
            }, 2000);
          }
        },
        error: () => {
          this.uploadStatuses.update((statuses) => {
            const next = new Map(statuses);
            next.set(file.name, {
              id: docId,
              title: file.name,
              status: 'error',
              error: this.i18n.t().ragChat.uploadFailed.replace('{name}', file.name),
            });
            return next;
          });
          this.notifications.showError(
            this.i18n.t().ragChat.uploadFailed.replace('{name}', file.name),
          );
        },
        complete: () => {
          if (index === this.pendingFiles().length - 1) {
            this.isUploading.set(false);
            onComplete?.();
          }
        },
      });
    });
  }

  // ==================== Chat ====================

  setInput(text: string): void {
    this.input.set(text);
  }

  async sendMessage(): Promise<void> {
    if (!this.input().trim()) return;
    if (this.isLoading()) return;

    const userMessage: RagChatMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: this.input().trim(),
      timestamp: Date.now(),
    };

    this.messages.update(msgs => [...msgs, userMessage]);
    this.input.set('');
    this.isLoading.set(true);

    const assistantMessageId = `assistant_${Date.now()}`;
    this.messages.update(msgs => [
      ...msgs,
      {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      },
    ]);
    this.streamingMessageIds.update(ids => new Set(ids).add(assistantMessageId));

    const requestBody: RagQuery = {
      query: userMessage.content,
      sessionId: this.sessionId,
      topK: DEFAULT_TOP_K,
      temperature: DEFAULT_TEMPERATURE,
    };

    if (this.selectedDocIds().size > 0) {
      requestBody.docIds = Array.from(this.selectedDocIds());
    }

    this.ragChat(
      requestBody,
      (chunk: string) => {
        this.messages.update(msgs => msgs.map(msg => msg.id === assistantMessageId
          ? { ...msg, content: msg.content + chunk }
          : msg,
        ),
        );
      },
      (sources: SourceDocument[]) => {
        this.messages.update((msgs) => {
          return msgs.map((msg) => {
            return msg.id === assistantMessageId ? { ...msg, sources } : msg;
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
        this.messages.update((msgs) => {
          return msgs.map((msg) => {
            return msg.id === assistantMessageId
              ? { ...msg, content: 'An error occurred while processing your request.' }
              : msg;
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

  private getDocuments(): Observable<DocumentListResponse> {
    return this.http
      .get<DocumentListResponse>(`${API_BASE_URL}/rag/documents`)
      .pipe(catchError(() => of({ documents: [] })));
  }

  /** Needs app-level withXhr(); FetchBackend cannot emit upload progress (AI-249). */
  private uploadDocument(
    file: File,
    title?: string,
  ): Observable<HttpEvent<{ id: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title ?? file.name);
    return this.http.post<{ id: string }>(`${API_BASE_URL}/rag/documents/upload`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  private deleteDocumentRequest(docId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/rag/documents/${docId}`);
  }

  private ragChat(
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
