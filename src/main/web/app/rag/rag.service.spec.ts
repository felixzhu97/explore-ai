import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { I18nService } from '@core/i18n';
import { RagService } from './rag.service';
import type { RagQuery, SourceDocument } from './rag.model';

describe('RagService', () => {
  let service: RagService;
  let api: {
    getDocuments: ReturnType<typeof vi.fn>;
    deleteDocument: ReturnType<typeof vi.fn>;
    ragChat: ReturnType<typeof vi.fn>;
  };
  let notifications: {
    showError: ReturnType<typeof vi.fn>;
    showInfo: ReturnType<typeof vi.fn>;
    showSuccess: ReturnType<typeof vi.fn>;
  };

  const i18n = {
    t: () => ({
      common: {
        loadFailed: 'Failed to load data',
      },
      ragChat: {
        deleteFailed: 'Failed to delete document',
        documentDeleted: 'Document deleted',
      },
    }),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00.000Z'));

    api = {
      getDocuments: vi.fn(),
      deleteDocument: vi.fn(),
      ragChat: vi.fn(),
    };
    notifications = {
      showError: vi.fn(),
      showInfo: vi.fn(),
      showSuccess: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        RagService,
        { provide: ApiService, useValue: api },
        { provide: NotificationService, useValue: notifications },
        { provide: I18nService, useValue: i18n },
      ],
    });

    service = TestBed.inject(RagService);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  describe('fetchAvailableDocs', () => {
    it('should_select_all_loaded_documents_when_fetch_succeeds', () => {
      api.getDocuments.mockReturnValue(of({
        documents: [
          { id: 'doc-1', title: 'Architecture Guide' },
          { id: 'doc-2', title: '' },
        ],
      }));

      service.fetchAvailableDocs();

      expect(service.isLoadingDocs()).toBe(false);
      expect(service.availableDocs()).toEqual([
        { id: 'doc-1', title: 'Architecture Guide' },
        { id: 'doc-2', title: 'Untitled' },
      ]);
      expect(Array.from(service.selectedDocIds())).toEqual(['doc-1', 'doc-2']);
    });

    it('should_clear_documents_and_notify_when_fetch_fails', () => {
      service.availableDocs.set([{ id: 'stale-doc', title: 'Stale' }]);
      api.getDocuments.mockReturnValue(throwError(() => new Error('network')));

      service.fetchAvailableDocs();

      expect(service.availableDocs()).toEqual([]);
      expect(notifications.showError).toHaveBeenCalledWith('Failed to load data');
    });
  });

  describe('deleteDocument', () => {
    it('should_reject_invalid_document_id_without_calling_api', () => {
      service.deleteDocument('undefined');

      expect(api.deleteDocument).not.toHaveBeenCalled();
      expect(notifications.showError).toHaveBeenCalledWith('Cannot delete: document ID is invalid');
    });

    it('should_remove_document_from_lists_when_delete_succeeds', () => {
      service.availableDocs.set([
        { id: 'doc-1', title: 'Keep' },
        { id: 'doc-2', title: 'Delete' },
      ]);
      service.selectedDocIds.set(new Set(['doc-1', 'doc-2']));
      api.deleteDocument.mockReturnValue(of(undefined));

      service.deleteDocument('doc-2');

      expect(service.deletingDocIds().has('doc-2')).toBe(true);

      vi.advanceTimersByTime(200);

      expect(service.availableDocs()).toEqual([{ id: 'doc-1', title: 'Keep' }]);
      expect(Array.from(service.selectedDocIds())).toEqual(['doc-1']);
      expect(service.deletingDocIds().has('doc-2')).toBe(false);
      expect(notifications.showSuccess).toHaveBeenCalledWith('Document deleted');
    });
  });

  describe('sendMessage', () => {
    it('should_send_selected_documents_and_images_in_rag_query', async () => {
      const sources: SourceDocument[] = [
        { text: 'source text', score: 0.87, metadata: { title: 'Architecture Guide' } },
      ];
      api.ragChat.mockImplementation((
        request: RagQuery,
        onChunk: (text: string) => void,
        onSources: (nextSources: SourceDocument[]) => void,
        onDone: () => void,
      ) => {
        onChunk('The answer');
        onSources(sources);
        onDone();
        return { abort: vi.fn() };
      });
      service.setInput('How does the system work?');
      service.selectedDocIds.set(new Set(['doc-1', 'doc-2']));
      service.pendingImages.set(['data:image/png;base64,abc123']);

      await service.sendMessage();

      expect(api.ragChat).toHaveBeenCalledOnce();
      const request = api.ragChat.mock.calls[0][0] as RagQuery;
      expect(request).toMatchObject({
        query: 'How does the system work?',
        sessionId: 'session_1767225600000',
        topK: 5,
        temperature: 0.7,
        docIds: ['doc-1', 'doc-2'],
        images: ['data:image/png;base64,abc123'],
      });
      expect(service.pendingImages()).toEqual([]);
      expect(service.isLoading()).toBe(false);
      expect(service.messages()).toEqual([
        expect.objectContaining({
          id: 'user_1767225600000',
          role: 'user',
          content: 'How does the system work?',
          images: ['data:image/png;base64,abc123'],
        }),
        expect.objectContaining({
          id: 'assistant_1767225600000',
          role: 'assistant',
          content: 'The answer',
          sources,
        }),
      ]);
      expect(service.streamingMessageIds().size).toBe(0);
    });
  });
});
