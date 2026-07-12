import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiRagService } from './api-rag.service';

describe('ApiRagService', () => {
  let service: ApiRagService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiRagService],
    });
    service = TestBed.inject(ApiRagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should list documents', () => {
    service.getDocuments().subscribe((response) => {
      expect(response.documents).toHaveLength(1);
    });
    const req = httpMock.expectOne('/api/rag/documents');
    req.flush({ documents: [{ id: 'd1', title: 'Doc', status: 'READY' }] });
  });

  it('should upload a document', () => {
    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    service.uploadDocument(file, 'Title').subscribe((result) => {
      expect(result.id).toBe('doc-1');
    });
    const req = httpMock.expectOne('/api/rag/documents/upload');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'doc-1' });
  });

  it('should return abort handle for rag chat', () => {
    const result = service.ragChat(
      { query: 'What is AI?' },
      vi.fn(),
      vi.fn(),
      vi.fn(),
      vi.fn(),
    );
    expect(result.abort).toBeTypeOf('function');
  });
});
