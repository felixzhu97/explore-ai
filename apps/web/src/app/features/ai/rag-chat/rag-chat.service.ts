import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { I18nService } from '@i18n';

export interface DocumentItem {
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

@Injectable({ providedIn: 'root' })
export class RagChatService {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  // Document state
  availableDocs = signal<DocumentItem[]>([]);
  selectedDocIds = signal<Set<string>>(new Set());
  deletingDocIds = signal<Set<string>>(new Set());
  isLoadingDocs = signal(true);

  // Upload state
  pendingFiles = signal<File[]>([]);
  uploadStatuses = signal<Map<string, UploadStatus>>(new Map());
  isUploading = signal(false);

  fetchAvailableDocs(): void {
    this.isLoadingDocs.set(true);
    this.api.getDocuments().subscribe({
      next: (data) => {
        const docs = (data.documents || []).map((d: any) => ({
          id: d.id || d.doc_id || '',
          title: d.title || d.filename || d.name || 'Untitled',
        }));
        this.availableDocs.set(docs);
        this.selectedDocIds.set(new Set(docs.map((d) => d.id)));
      },
      error: (err) => {
        console.error('[RAG] Failed to load docs:', err);
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
    this.selectedDocIds.set(new Set(this.availableDocs().map((d) => d.id)));
  }

  clearDocSelection(): void {
    this.selectedDocIds.set(new Set());
  }

  deleteDocument(docId: string): void {
    if (!docId || docId === 'undefined' || docId === 'null') {
      console.error('[RAG] Invalid document ID:', docId);
      this.notifications.showError('Cannot delete: document ID is invalid');
      return;
    }

    this.deletingDocIds.update((ids) => new Set(ids).add(docId));

    this.api.deleteDocument(docId).subscribe({
      next: () => {
        setTimeout(() => {
          this.availableDocs.update((docs) => docs.filter((d) => d.id !== docId));
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
      (f) => !this.pendingFiles().some((pf) => pf.name === f.name)
    );
    this.pendingFiles.update((prev) => [...prev, ...newFiles]);
    this.notifications.showInfo(
      this.i18n.t().ragChat.fileSelected.replace('{count}', newFiles.length.toString())
    );
  }

  removePendingFile(index: number): void {
    this.pendingFiles.update((files) => files.filter((_, i) => i !== index));
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
            this.i18n.t().ragChat.uploadSuccess.replace('{name}', file.name)
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
            this.i18n.t().ragChat.uploadFailed.replace('{name}', file.name)
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
}
