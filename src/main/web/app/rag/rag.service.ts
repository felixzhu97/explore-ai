import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { I18nService } from '@core/i18n';
import { SourceDocument } from './rag.model';

export interface DocumentItem {
  id: string;
  title: string;
}

interface DocumentResponse {
  documents?: {
    id?: string;
    doc_id?: string;
    title?: string;
    filename?: string;
    name?: string;
  }[];
}

export interface UploadStatus {
  id: string;
  title: string;
  status: 'uploading' | 'success' | 'error';
  progress?: number;
  error?: string;
}

export interface ChatMessage {
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
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  private readonly i18n = inject(I18nService);
  private readonly sessionId = `session_${Date.now()}`;

  // Document state
  readonly availableDocs = signal<DocumentItem[]>([]);
  readonly selectedDocIds = signal<Set<string>>(new Set());
  readonly deletingDocIds = signal<Set<string>>(new Set());
  readonly isLoadingDocs = signal(true);

  // Upload state
  readonly pendingFiles = signal<File[]>([]);
  readonly uploadStatuses = signal<Map<string, UploadStatus>>(new Map());
  readonly isUploading = signal(false);

  // Chat state
  readonly messages = signal<ChatMessage[]>([]);
  readonly input = signal('');
  readonly isLoading = signal(false);
  readonly expandedSources = signal<Set<string>>(new Set());

  // Streaming state
  readonly streamingMessageIds = signal<Set<string>>(new Set());

  fetchAvailableDocs(): void {
    this.isLoadingDocs.set(true);
    this.api.getDocuments().subscribe({
      next: (data: DocumentResponse) => {
        const docs = (data.documents || []).map((d) => {
          return { id: d.id || d.doc_id || '', title: d.title || d.filename || d.name || 'Untitled' };
        });
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

    this.api.deleteDocument(docId).subscribe({
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

      this.api.uploadDocument(file).subscribe({
        next: () => {
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

  sendMessage(): void {
    if (!this.input().trim() || this.isLoading()) return;

    const userMessage: ChatMessage = {
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

    const requestBody: {
      query: string;
      session_id: string;
      top_k: number;
      temperature: number;
      doc_ids?: string[];
    } = {
      query: userMessage.content,
      session_id: this.sessionId,
      top_k: DEFAULT_TOP_K,
      temperature: DEFAULT_TEMPERATURE,
    };

    if (this.selectedDocIds().size > 0) {
      requestBody.doc_ids = Array.from(this.selectedDocIds());
    }

    this.api.ragChat(
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

  toggleSources(messageId: string): void {
    this.expandedSources.update((ids) => {
      const next = new Set(ids);
      if (next.has(messageId)) {
        next.delete(messageId);
      } else {
        next.add(messageId);
      }
      return next;
    });
  }
}
