import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RagChatComponent } from './rag-chat.component';
import { ApiService, SourceDocument } from '../services/api.service';

describe('RagChatComponent', () => {
  let fixture: ComponentFixture<RagChatComponent>;
  let component: RagChatComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RagChatComponent, HttpClientTestingModule],
      providers: [ApiService],
    }).compileComponents();

    fixture = TestBed.createComponent(RagChatComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should fetch available docs on init', () => {
      const req = httpMock.expectOne('/api/rag/documents');
      expect(req.request.method).toBe('GET');
      req.flush({ documents: [] });
    });

    it('should initialize with empty messages', () => {
      expect(component.messages()).toEqual([]);
    });

    it('should initialize with empty input', () => {
      expect(component.input()).toBe('');
    });

    it('should set isLoadingDocs to true initially', () => {
      expect(component.isLoadingDocs()).toBe(true);
    });

    it('should complete loading docs after fetch', fakeAsync(() => {
      const req = httpMock.expectOne('/api/rag/documents');
      req.flush({ documents: [] });
      tick();
      fixture.detectChanges();
      
      expect(component.isLoadingDocs()).toBe(false);
    }));
  });

  describe('document management', () => {
    it('should display documents from API', fakeAsync(() => {
      const mockDocs = [
        { doc_id: 'doc1', filename: 'document1.pdf' },
        { doc_id: 'doc2', filename: 'document2.pdf' },
      ];
      
      const req = httpMock.expectOne('/api/rag/documents');
      req.flush({ documents: mockDocs });
      tick();
      fixture.detectChanges();
      
      expect(component.availableDocs().length).toBe(2);
      expect(component.availableDocs()[0].title).toBe('document1.pdf');
    }));

    it('should toggle document selection', () => {
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
        { id: 'doc2', title: 'doc2.pdf' },
      ]);
      
      component.toggleDocSelection('doc1');
      expect(component.selectedDocIds().has('doc1')).toBe(true);
      
      component.toggleDocSelection('doc1');
      expect(component.selectedDocIds().has('doc1')).toBe(false);
    });

    it('should select all documents', () => {
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
        { id: 'doc2', title: 'doc2.pdf' },
        { id: 'doc3', title: 'doc3.pdf' },
      ]);
      
      component.selectAllDocs();
      expect(component.selectedDocIds().size).toBe(3);
      expect(component.selectedDocIds().has('doc1')).toBe(true);
      expect(component.selectedDocIds().has('doc2')).toBe(true);
      expect(component.selectedDocIds().has('doc3')).toBe(true);
    });

    it('should clear document selection', () => {
      component.selectedDocIds.set(new Set(['doc1', 'doc2']));
      
      component.clearDocSelection();
      expect(component.selectedDocIds().size).toBe(0);
    });

    it('should delete document and update list', fakeAsync(() => {
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
        { id: 'doc2', title: 'doc2.pdf' },
      ]);
      component.selectedDocIds.set(new Set(['doc1', 'doc2']));
      
      const deleteReq = httpMock.expectOne('/api/rag/documents/doc1');
      deleteReq.flush({ success: true });
      tick(300);
      
      expect(component.availableDocs().length).toBe(1);
      expect(component.availableDocs()[0].id).toBe('doc2');
    }));

    it('should handle document deletion error', fakeAsync(() => {
      const deleteReq = httpMock.expectOne('/api/rag/documents/doc1');
      deleteReq.error(new ProgressEvent('error'));
      tick();
      
      expect(component.toasts().some(t => t.type === 'error')).toBe(true);
    }));
  });

  describe('file upload', () => {
    it('should add pending files on selection', () => {
      const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const event = {
        target: {
          files: [mockFile],
          value: '',
        },
      } as unknown as Event;
      
      component.onFileSelect(event);
      
      expect(component.pendingFiles().length).toBe(1);
      expect(component.pendingFiles()[0].name).toBe('test.pdf');
    });

    it('should remove pending file by index', () => {
      component.pendingFiles.set([
        new File(['content1'], 'file1.pdf', { type: 'application/pdf' }),
        new File(['content2'], 'file2.pdf', { type: 'application/pdf' }),
      ]);
      
      component.removePendingFile(0);
      
      expect(component.pendingFiles().length).toBe(1);
      expect(component.pendingFiles()[0].name).toBe('file2.pdf');
    });

    it('should upload files via API', fakeAsync(() => {
      component.pendingFiles.set([
        new File(['content'], 'test.pdf', { type: 'application/pdf' }),
      ]);
      
      component.uploadFiles();
      
      const uploadReq = httpMock.expectOne('/api/rag/upload');
      expect(uploadReq.request.method).toBe('POST');
      uploadReq.flush({ success: true, doc_id: 'new_doc' });
      tick(300);
      
      expect(component.pendingFiles().length).toBe(0);
    }));

    it('should filter duplicate files', () => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      component.pendingFiles.set([file]);
      
      const event = {
        target: {
          files: [file],
          value: '',
        },
      } as unknown as Event;
      
      component.onFileSelect(event);
      
      expect(component.pendingFiles().length).toBe(1);
    });
  });

  describe('chat functionality', () => {
    it('should update messages on send', () => {
      component.input.set('What is this document about?');
      component.messages.set([]);
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
      ]);
      component.selectedDocIds.set(new Set(['doc1']));
      
      component.sendMessage();
      
      expect(component.messages().length).toBe(2);
      expect(component.messages()[0].role).toBe('user');
      expect(component.messages()[0].content).toBe('What is this document about?');
      expect(component.messages()[1].role).toBe('assistant');
    });

    it('should clear input after send', () => {
      component.input.set('Test message');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      
      component.sendMessage();
      
      expect(component.input()).toBe('');
    });

    it('should set loading state during request', () => {
      component.input.set('Test query');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      
      component.sendMessage();
      expect(component.isLoading()).toBe(true);
      
      const req = httpMock.expectOne('/api/rag/query');
      req.flush({ answer: 'Test answer', sources: [] });
      
      expect(component.isLoading()).toBe(false);
    });

    it('should not send empty messages', () => {
      component.input.set('   ');
      component.messages.set([]);
      
      component.sendMessage();
      
      expect(component.messages().length).toBe(0);
    });

    it('should not send messages while loading', () => {
      component.input.set('Test');
      component.isLoading.set(true);
      
      component.sendMessage();
      
      expect(component.messages().length).toBe(0);
    });

    it('should handle Enter key without Shift', () => {
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      spyOn(event, 'preventDefault');
      component.input.set('Test message');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      
      component.onInputKeyDown(event);
      
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should not prevent default on Shift+Enter', () => {
      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
      spyOn(event, 'preventDefault');
      
      component.onInputKeyDown(event);
      
      expect(event.preventDefault).not.toHaveBeenCalled();
    });
  });

  describe('toast notifications', () => {
    it('should add toast notification', () => {
      component.addToast('Test message', 'success');
      
      expect(component.toasts().length).toBe(1);
      expect(component.toasts()[0].message).toBe('Test message');
      expect(component.toasts()[0].type).toBe('success');
    });

    it('should auto-remove toast after 4 seconds', fakeAsync(() => {
      component.addToast('Test', 'info');
      expect(component.toasts().length).toBe(1);
      
      tick(4000);
      expect(component.toasts().length).toBe(0);
    }));

    it('should display multiple toasts', () => {
      component.addToast('Success toast', 'success');
      component.addToast('Error toast', 'error');
      component.addToast('Info toast', 'info');
      
      expect(component.toasts().length).toBe(3);
    });

    it('should show upload success toast', fakeAsync(() => {
      component.pendingFiles.set([
        new File(['content'], 'test.pdf', { type: 'application/pdf' }),
      ]);
      
      component.uploadFiles();
      
      const uploadReq = httpMock.expectOne('/api/rag/upload');
      uploadReq.flush({ success: true });
      tick();
      
      expect(component.toasts().some(t => t.type === 'success' && t.message.includes('test.pdf'))).toBe(true);
    }));
  });

  describe('source expansion', () => {
    it('should toggle sources for a message', () => {
      component.toggleSources('msg1');
      expect(component.expandedSources().has('msg1')).toBe(true);
      
      component.toggleSources('msg1');
      expect(component.expandedSources().has('msg1')).toBe(false);
    });

    it('should expand multiple message sources independently', () => {
      component.toggleSources('msg1');
      component.toggleSources('msg2');
      
      expect(component.expandedSources().has('msg1')).toBe(true);
      expect(component.expandedSources().has('msg2')).toBe(true);
    });
  });

  describe('utilities', () => {
    it('should format time correctly', () => {
      const timestamp = new Date('2024-01-15T10:30:00').getTime();
      const formatted = component.formatTime(timestamp);
      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });

    it('should render markdown content', () => {
      const html = component.renderMarkdown('**bold** and *italic*');
      expect(html).toContain('<strong>bold</strong>');
      expect(html).toContain('<em>italic</em>');
    });

    it('should render code blocks in markdown', () => {
      const content = '```javascript\nconst x = 1;\n```';
      const html = component.renderMarkdown(content);
      expect(html).toContain('<pre><code>');
    });

    it('should render inline code in markdown', () => {
      const html = component.renderMarkdown('Use `console.log()`');
      expect(html).toContain('<code>console.log()</code>');
    });
  });

  describe('keyboard navigation', () => {
    it('should toggle doc selection on Enter key', () => {
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
      ]);
      
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onDocKeyDown(event, 'doc1');
      
      expect(component.selectedDocIds().has('doc1')).toBe(true);
    });

    it('should toggle doc selection on Space key', () => {
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
      ]);
      
      const event = new KeyboardEvent('keydown', { key: ' ' });
      component.onDocKeyDown(event, 'doc1');
      
      expect(component.selectedDocIds().has('doc1')).toBe(true);
    });
  });

  describe('setInput method', () => {
    it('should update input value', () => {
      component.setInput('New input');
      expect(component.input()).toBe('New input');
    });

    it('should handle empty string', () => {
      component.setInput('');
      expect(component.input()).toBe('');
    });
  });

  describe('document deletion state', () => {
    it('should track deleting document IDs', () => {
      component['deletingDocIds'].set(new Set(['doc1']));
      expect(component.deletingDocIds().has('doc1')).toBe(true);
    });
  });
});
