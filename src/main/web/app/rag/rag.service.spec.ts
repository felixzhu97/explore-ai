import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr, HttpEventType } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../core/api.constants';
import { NotificationService } from '../core/notification.service';
import { I18nService } from '../core/i18n';
import { RagService } from './rag.service';
import * as sseClient from '../core/streaming/sse-client';

vi.mock('../core/streaming/sse-client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../core/streaming/sse-client')>();
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
              common: { loadFailed: 'load failed' },
              ragChat: {
                documentDeleted: 'deleted',
                deleteFailed: 'delete failed',
                fileSelected: '{count} files',
                uploadSuccess: '{name} ok',
                uploadFailed: '{name} fail',
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

  it('should_fetch_documents_and_select_all', () => {
    service.fetchAvailableDocs();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).flush({
      documents: [{ id: 'd1', title: 'Doc 1' }],
    });
    expect(service.availableDocs()).toEqual([{ id: 'd1', title: 'Doc 1' }]);
    expect(service.selectedDocIds().has('d1')).toBe(true);
    expect(service.isLoadingDocs()).toBe(false);
  });

  it('should_return_empty_docs_when_api_fails', () => {
    service.fetchAvailableDocs();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents`).error(new ProgressEvent('error'));
    expect(service.availableDocs()).toEqual([]);
    expect(service.isLoadingDocs()).toBe(false);
  });

  it('should_toggle_clear_and_select_all_docs', () => {
    service.availableDocs.set([
      { id: 'a', title: 'A' },
      { id: 'b', title: 'B' },
    ]);
    service.selectedDocIds.set(new Set(['a', 'b']));
    service.toggleDocSelection('a');
    expect(service.selectedDocIds().has('a')).toBe(false);
    service.clearDocSelection();
    expect(service.selectedDocIds().size).toBe(0);
    service.selectAllDocs();
    expect(service.selectedDocIds().size).toBe(2);
  });

  it('should_reject_invalid_document_id_on_delete', () => {
    service.deleteDocument('undefined');
    expect(notifications.showError).toHaveBeenCalled();
    httpMock.expectNone(`${API_BASE_URL}/rag/documents/undefined`);
  });

  it('should_delete_document', async () => {
    vi.useFakeTimers();
    service.availableDocs.set([{ id: 'd1', title: 'Doc' }]);
    service.deleteDocument('d1');
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/d1`).flush(null);
    await vi.advanceTimersByTimeAsync(200);
    expect(service.availableDocs()).toEqual([]);
    expect(notifications.showSuccess).toHaveBeenCalledWith('deleted');
  });

  it('should_handle_delete_document_error', () => {
    service.deleteDocument('d1');
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/d1`).error(new ProgressEvent('error'));
    expect(notifications.showError).toHaveBeenCalledWith('delete failed');
  });

  it('should_manage_pending_files_and_images', () => {
    const file = new File(['x'], 'a.txt', { type: 'text/plain' });
    service.onFileSelect([file]);
    expect(service.pendingFiles()).toHaveLength(1);
    service.removePendingFile(0);
    expect(service.pendingFiles()).toHaveLength(0);
    expect(service.addImage('img1')).toBe(true);
    service.removeImage(0);
    service.clearImages();
    expect(service.pendingImages()).toEqual([]);
  });

  it('should_reject_images_over_max', () => {
    for (let i = 0; i < 5; i++) {
      service.addImage(`img${i}`);
    }
    expect(service.addImage('overflow')).toBe(false);
  });

  it('should_upload_files_and_refresh_docs', async () => {
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

  it('should_report_upload_progress_during_xhr_upload', () => {
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

  it('should_handle_upload_error', () => {
    const file = new File(['data'], 'bad.pdf');
    service.pendingFiles.set([file]);
    service.uploadFiles();
    httpMock.expectOne(`${API_BASE_URL}/rag/documents/upload`).error(new ProgressEvent('error'));
    expect(service.getUploadStatus('bad.pdf')?.status).toBe('error');
  });

  it('should_send_message_via_sse_stream', async () => {
    streamSsePostMock.mockImplementation((_url, _body, handlers) => {
      handlers.onEvent({
        eventType: 'sources',
        data: '[{"text":"T","score":0.9,"metadata":{"url":"https://a.com"}}]',
      });
      handlers.onEvent({ eventType: 'message', data: 'Hello<br/>world' });
      handlers.onEvent({ eventType: 'message', data: '[DONE]' });
      return { abort: vi.fn() };
    });
    service.setInput('Question');
    await service.sendMessage();
    expect(service.messages()[1].content).toContain('Hello');
    expect(service.messages()[1].sources?.[0].text).toBe('T');
    expect(service.messages()[1].sources?.[0].metadata['url']).toBe('https://a.com');
    expect(service.isLoading()).toBe(false);
  });

  it('should_handle_stream_error_prefix', async () => {
    streamSsePostMock.mockImplementation((_url, _body, handlers) => {
      handlers.onEvent({ eventType: 'message', data: 'Error:failed' });
      return { abort: vi.fn() };
    });
    service.setInput('Fail');
    await service.sendMessage();
    expect(service.messages()[1].content).toContain('error occurred');
  });

  it('should_skip_empty_send', async () => {
    service.setInput('   ');
    await service.sendMessage();
    expect(streamSsePostMock).not.toHaveBeenCalled();
  });

  it('should_toggle_expanded_sources', () => {
    service.toggleSources('m1');
    expect(service.expandedSources().has('m1')).toBe(true);
    service.toggleSources('m1');
    expect(service.expandedSources().has('m1')).toBe(false);
  });

  it('should_update_input_signal', () => {
    service.setInput('hello rag');
    expect(service.input()).toBe('hello rag');
  });
});
