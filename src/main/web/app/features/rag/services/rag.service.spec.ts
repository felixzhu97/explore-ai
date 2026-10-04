import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr, HttpEventType } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../../core/api.constants';
import { NotificationService } from '../../../core/services/notification.service';
import { I18nService } from '../../../core/i18n';
import { RagService } from './rag.service';
import * as sseClient from '../../../core/streaming/sse-client';

vi.mock('../../../core/streaming/sse-client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../core/streaming/sse-client')>();
  return {
    ...actual,
    streamSsePost: vi.fn(),
  };
});

describe('RagService', () => {
  let service: RagService;
  let httpMock: HttpTestingController;
  let notifications: {
    showError: ReturnType<typeof vi.fn>;
    showSuccess: ReturnType<typeof vi.fn>;
    showInfo: ReturnType<typeof vi.fn>;
  };
  const streamSsePostMock = vi.mocked(sseClient.streamSsePost);

  beforeEach(() => {
    TestBed.resetTestingModule();
    notifications = {
      showError: vi.fn(),
      showSuccess: vi.fn(),
      showInfo: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        RagService,
        { provide: NotificationService, useValue: notifications },
        {
          provide: I18nService,
          useValue: {
            t: () => ({
              common: { errors: { loadFailed: 'load failed' } },
              rag: {
                documentDeleted: 'deleted',
                fileSelected: '{count} files',
                uploadSuccess: '{name} ok',
                errors: {
                  deleteFailed: 'delete failed',
                  uploadFailed: '{name} fail',
                },
              },
            }),
          },
        },
      ],
    });
    service = TestBed.inject(RagService);
    httpMock = TestBed.inject(HttpTestingController);
    streamSsePostMock.mockReset();
    streamSsePostMock.mockReturnValue({ abort: vi.fn() });
  });

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  it('should fetch documents and select all', () => {
    service.fetchAvailableDocuments();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).flush({
      documents: [{ id: 'd1', title: 'Doc 1' }],
    });
    expect(service.availableDocuments()).toEqual([{ id: 'd1', title: 'Doc 1' }]);
    expect(service.selectedDocumentIds().has('d1')).toBe(true);
    expect(service.isLoadingDocuments()).toBe(false);
  });

  it('should return empty documents when api fails', () => {
    service.fetchAvailableDocuments();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).error(new ProgressEvent('error'));
    expect(service.availableDocuments()).toEqual([]);
    expect(service.isLoadingDocuments()).toBe(false);
  });

  it('should toggle clear and select all documents', () => {
    service.availableDocuments.set([
      { id: 'a', title: 'A' },
      { id: 'b', title: 'B' },
    ]);
    service.selectedDocumentIds.set(new Set(['a', 'b']));
    service.toggleDocumentSelection('a');
    expect(service.selectedDocumentIds().has('a')).toBe(false);
    service.clearDocumentSelection();
    expect(service.selectedDocumentIds().size).toBe(0);
    service.selectAllDocuments();
    expect(service.selectedDocumentIds().size).toBe(2);
  });

  it('should reject invalid document id on delete', () => {
    service.deleteDocument('undefined');
    expect(notifications.showError).toHaveBeenCalled();
    httpMock.expectNone(`${API_BASE_URL}/rag/documents/undefined`);
  });

  it('should delete document', async () => {
    vi.useFakeTimers();
    service.availableDocuments.set([{ id: 'd1', title: 'Doc' }]);
    service.deleteDocument('d1');
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/d1`).flush(null);
    await vi.advanceTimersByTimeAsync(200);
    expect(service.availableDocuments()).toEqual([]);
    expect(notifications.showSuccess).toHaveBeenCalledWith('deleted');
  });

  it('should handle delete document error', () => {
    service.deleteDocument('d1');
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/d1`).error(new ProgressEvent('error'));
    expect(notifications.showError).toHaveBeenCalledWith('delete failed');
  });

  it('should manage pending files when selected', () => {
    const file = new File(['x'], 'a.txt', { type: 'text/plain' });
    service.onFileSelect([file]);
    expect(service.pendingFiles()).toHaveLength(1);
    service.removePendingFile(0);
    expect(service.pendingFiles()).toHaveLength(0);
  });

  it('should upload files and refresh documents', async () => {
    vi.useFakeTimers();
    const file = new File(['data'], 'doc.pdf');
    service.pendingFiles.set([file]);
    service.uploadFiles();
    const uploadReq = httpMock.expectOne(`${API_BASE_URL}/rag/documents/upload`);
    uploadReq.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
    expect(service.getUploadStatus('doc.pdf')?.progress).toBe(50);
    uploadReq.flush({ id: 'new-id' });
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).flush({ documents: [] });
    await vi.advanceTimersByTimeAsync(2000);
    expect(notifications.showSuccess).toHaveBeenCalled();
    expect(service.pendingFiles()).toEqual([]);
  });

  it('should report upload progress during xhr upload', () => {
    vi.useFakeTimers();
    const file = new File(['data'], 'progress.pdf');
    service.pendingFiles.set([file]);
    service.uploadFiles();
    const uploadReq = httpMock.expectOne(`${API_BASE_URL}/rag/documents/upload`);
    uploadReq.event({ type: HttpEventType.UploadProgress, loaded: 25, total: 100 });
    expect(service.getUploadStatus('progress.pdf')?.progress).toBe(25);
    uploadReq.event({ type: HttpEventType.UploadProgress, loaded: 100, total: 100 });
    expect(service.getUploadStatus('progress.pdf')?.progress).toBe(100);
    uploadReq.flush({ id: 'progress-id' });
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).flush({ documents: [] });
    vi.useRealTimers();
  });

  it('should handle upload error', () => {
    const file = new File(['data'], 'bad.pdf');
    service.pendingFiles.set([file]);
    service.uploadFiles();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/upload`).error(new ProgressEvent('error'));
    expect(service.getUploadStatus('bad.pdf')?.status).toBe('error');
  });

  it('should send message via sse stream', async () => {
    streamSsePostMock.mockImplementation((_url, _body, handlers) => {
      handlers.onEvent({
        eventType: 'sources',
        data: '[{"id":"s1","content":"T","score":0.9,"metadata":{"url":"https://a.com"}}]',
      });
      handlers.onEvent({ eventType: 'message', data: 'Hello<br/>world' });
      handlers.onEvent({ eventType: 'message', data: '[DONE]' });
      return { abort: vi.fn() };
    });
    service.setInput('Question');
    await service.sendMessage();
    expect(service.messages()[1].content).toContain('Hello');
    expect(service.messages()[1].sources?.[0].content).toBe('T');
    expect(service.messages()[1].sources?.[0].metadata['url']).toBe('https://a.com');
    expect(service.isLoading()).toBe(false);
  });

  it('should handle stream error prefix', async () => {
    streamSsePostMock.mockImplementation((_url, _body, handlers) => {
      handlers.onEvent({ eventType: 'message', data: 'Error:failed' });
      return { abort: vi.fn() };
    });
    service.setInput('Fail');
    await service.sendMessage();
    expect(service.messages()[1].content).toContain('error occurred');
  });

  it('should skip empty send', async () => {
    service.setInput('   ');
    await service.sendMessage();
    expect(streamSsePostMock).not.toHaveBeenCalled();
  });

  it('should update input signal', () => {
    service.setInput('hello rag');
    expect(service.input()).toBe('hello rag');
  });
});
