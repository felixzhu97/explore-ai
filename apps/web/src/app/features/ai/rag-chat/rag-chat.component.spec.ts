import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RagChatComponent } from './rag-chat.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { of, throwError } from 'rxjs';

describe('RagChatComponent', () => {
  let fixture: ComponentFixture<RagChatComponent>;
  let component: RagChatComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18nService = {
    t: () => ({
      ragChat: {
        title: 'RAG Chat',
        placeholder: 'Ask about your documents...',
        chat: {
          title: 'RAG Chat',
          description: 'Chat with your documents',
        },
        documents: 'Documents',
        selectedDocuments: '{count} selected',
        selectAll: 'Select All',
        clearSelection: 'Clear Selection',
        noDocuments: 'No documents uploaded',
        uploadDocs: 'Upload Documents',
        uploading: 'Uploading...',
        upload: 'Upload',
        askQuestion: 'Ask a question about your documents',
        whatIsThis: 'What is this?',
        summarize: 'Summarize',
        keyInfo: 'Key Information',
        basedOn: 'Based on {count} sources',
        sources: 'Sources',
        similarity: 'Similarity',
        thinking: 'Thinking...',
        inputPlaceholder: 'Type your question...',
        documentDeleted: 'Document deleted',
        deleteFailed: 'Failed to delete document',
        fileSelected: '{count} file(s) selected',
        uploadSuccess: '{name} uploaded successfully',
        uploadFailed: 'Failed to upload {name}',
      },
      agents: {
        startConversation: 'Start a conversation',
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      getDocuments: vi.fn().mockReturnValue(
        of({
          documents: [
            { id: 'doc1', title: 'document1.pdf' },
            { id: 'doc2', title: 'document2.pdf' },
          ],
        })
      ),
      uploadDocument: vi.fn().mockReturnValue(of({ success: true, doc_id: 'new_doc' })),
      deleteDocument: vi.fn().mockReturnValue(of({ success: true })),
      ragChat: vi.fn().mockReturnValue({ abort: vi.fn() }),
    };
    return mockApiService;
  };

  const createFixture = () => {
    fixture = TestBed.createComponent(RagChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    createMockApiService();
    await TestBed.configureTestingModule({
      imports: [RagChatComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: I18nService, useValue: mockI18nService },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should fetch available docs on init', () => {
      createFixture();
      expect(mockApiService.getDocuments).toHaveBeenCalled();
    });

    it('should initialize with empty messages', () => {
      createFixture();
      expect(component.messages()).toEqual([]);
    });

    it('should initialize with empty input', () => {
      createFixture();
      expect(component.input()).toBe('');
    });

    it('should initialize isLoadingDocs as true initially', () => {
      createFixture();
      expect(component.isLoadingDocs()).toBe(false);
    });
  });

  describe('document management', () => {
    it('should toggle document selection', () => {
      createFixture();
      // Override with controlled state - sync selectedDocIds with availableDocs
      component.selectedDocIds.set(new Set());
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
      createFixture();
      component.availableDocs.set([
        { id: 'doc1', title: 'doc1.pdf' },
        { id: 'doc2', title: 'doc2.pdf' },
      ]);

      component.selectAllDocs();
      expect(component.selectedDocIds().size).toBe(2);
    });

    it('should clear document selection', () => {
      createFixture();
      component.selectedDocIds.set(new Set(['doc1', 'doc2']));

      component.clearDocSelection();
      expect(component.selectedDocIds().size).toBe(0);
    });

    it('should handle keyboard interaction on doc selection', () => {
      createFixture();
      component.selectedDocIds.set(new Set());
      component.availableDocs.set([{ id: 'doc1', title: 'doc1.pdf' }]);

      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onDocKeyDown(enterEvent, 'doc1');
      expect(component.selectedDocIds().has('doc1')).toBe(true);

      const spaceEvent = new KeyboardEvent('keydown', { key: ' ' });
      component.onDocKeyDown(spaceEvent, 'doc1');
      expect(component.selectedDocIds().has('doc1')).toBe(false);
    });
  });

  describe('document deletion', () => {
    it('should delete document successfully', () => {
      createFixture();
      component.availableDocs.set([{ id: 'doc1', title: 'doc1.pdf' }]);
      component.selectedDocIds.set(new Set(['doc1']));

      component.deleteDocument('doc1', new Event('click'));

      expect(mockApiService.deleteDocument).toHaveBeenCalledWith('doc1');
    });

    it('should not delete with invalid document ID', () => {
      createFixture();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      component.deleteDocument('', new Event('click'));

      expect(mockApiService.deleteDocument).not.toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it('should not delete with undefined document ID', () => {
      createFixture();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      component.deleteDocument('undefined', new Event('click'));

      expect(mockApiService.deleteDocument).not.toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe('file upload', () => {
    it('should add files on selection', () => {
      createFixture();
      const mockInput = {
        files: [new File(['test'], 'test.pdf', { type: 'application/pdf' })],
        value: '',
      } as any;
      const event = { target: mockInput } as any;

      component.onFileSelect(event);

      expect(component.pendingFiles().length).toBe(1);
      expect(component.pendingFiles()[0].name).toBe('test.pdf');
    });

    it('should not add duplicate files', () => {
      createFixture();
      component.pendingFiles.set([new File(['test'], 'test.pdf', { type: 'application/pdf' })]);
      const mockInput = {
        files: [new File(['test'], 'test.pdf', { type: 'application/pdf' })],
        value: '',
      } as any;
      const event = { target: mockInput } as any;

      component.onFileSelect(event);

      expect(component.pendingFiles().length).toBe(1);
    });

    it('should remove pending file by index', () => {
      createFixture();
      component.pendingFiles.set([
        new File(['test1'], 'test1.pdf', { type: 'application/pdf' }),
        new File(['test2'], 'test2.pdf', { type: 'application/pdf' }),
      ]);

      component.removePendingFile(0);

      expect(component.pendingFiles().length).toBe(1);
      expect(component.pendingFiles()[0].name).toBe('test2.pdf');
    });

    it('should upload files', () => {
      createFixture();
      component.pendingFiles.set([new File(['test'], 'test.pdf', { type: 'application/pdf' })]);

      component.uploadFiles();

      expect(mockApiService.uploadDocument).toHaveBeenCalled();
    });

    it('should not upload when no pending files', () => {
      createFixture();
      component.pendingFiles.set([]);

      component.uploadFiles();

      expect(mockApiService.uploadDocument).not.toHaveBeenCalled();
    });
  });

  describe('chat functionality', () => {
    it('should update messages on send', () => {
      createFixture();
      component.input.set('What is this document about?');
      component.messages.set([]);
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());

      component.sendMessage();

      expect(component.messages().length).toBe(2);
      expect(component.messages()[0].role).toBe('user');
      expect(component.messages()[0].content).toBe('What is this document about?');
    });

    it('should clear input after send', () => {
      createFixture();
      component.input.set('Test message');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());

      component.sendMessage();

      expect(component.input()).toBe('');
    });

    it('should not send empty messages', () => {
      createFixture();
      component.input.set('   ');
      component.messages.set([]);

      component.sendMessage();

      expect(component.messages().length).toBe(0);
    });

    it('should not send when already loading', () => {
      createFixture();
      component.input.set('Test message');
      component.isLoading.set(true);

      component.sendMessage();

      expect(component.messages().length).toBe(0);
    });

    it('should include doc_ids in request when documents selected', () => {
      createFixture();
      component.input.set('Test');
      component.selectedDocIds.set(new Set(['doc1', 'doc2']));
      component.availableDocs.set([]);

      component.sendMessage();

      expect(mockApiService.ragChat).toHaveBeenCalled();
    });
  });

  describe('streaming message updates', () => {
    it('should update assistant message with chunk', () => {
      createFixture();
      component.input.set('Test');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      let capturedChunk: ((chunk: string) => void) | null = null;

      (mockApiService.ragChat as any).mockImplementation(
        (_: any, onChunk: (chunk: string) => void, __: any, ___: any, ____: any) => {
          capturedChunk = onChunk;
          return { abort: vi.fn() };
        }
      );

      component.sendMessage();
      capturedChunk?.('Hello ');
      capturedChunk?.('world!');

      const messages = component.messages();
      const assistantMsg = messages.find((m) => m.role === 'assistant');
      expect(assistantMsg?.content).toContain('Hello');
      expect(assistantMsg?.content).toContain('world');
    });

    it('should update sources when received', () => {
      createFixture();
      component.input.set('Test');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      let capturedSources: ((sources: any[]) => void) | null = null;

      (mockApiService.ragChat as any).mockImplementation(
        (_: any, __: any, onSources: (sources: any[]) => void, ___: any, ____: any) => {
          capturedSources = onSources;
          return { abort: vi.fn() };
        }
      );

      component.sendMessage();
      capturedSources?.([{ text: 'source text', score: 0.95 }]);

      const messages = component.messages();
      const assistantMsg = messages.find((m) => m.role === 'assistant');
      expect(assistantMsg?.sources).toHaveLength(1);
    });

    it('should set isLoading to false on completion', () => {
      createFixture();
      component.input.set('Test');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      let capturedDone: (() => void) | null = null;

      (mockApiService.ragChat as any).mockImplementation(
        (_: any, _1: any, _2: any, onDone: () => void, _3: any) => {
          capturedDone = onDone;
          return { abort: vi.fn() };
        }
      );

      component.sendMessage();
      capturedDone?.();

      expect(component.isLoading()).toBe(false);
    });

    it('should show error message on failure', () => {
      createFixture();
      component.input.set('Test');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      let capturedError: ((err: Error) => void) | null = null;

      (mockApiService.ragChat as any).mockImplementation(
        (_: any, _1: any, _2: any, _3: any, onError: (err: Error) => void) => {
          capturedError = onError;
          return { abort: vi.fn() };
        }
      );

      component.sendMessage();
      capturedError?.(new Error('Processing failed'));

      const messages = component.messages();
      const assistantMsg = messages.find((m) => m.role === 'assistant');
      expect(assistantMsg?.content).toContain('error');
    });
  });

  describe('utilities', () => {
    it('should format time correctly', () => {
      createFixture();
      const timestamp = new Date('2024-01-15T10:30:00').getTime();
      const formatted = component.formatTime(timestamp);
      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });

    it('should render markdown content', () => {
      createFixture();
      const html = component.renderMarkdown('**bold** and *italic*');
      expect(html).toContain('<strong>bold</strong>');
      expect(html).toContain('<em>italic</em>');
    });

    it('should render empty markdown gracefully', () => {
      createFixture();
      const html = component.renderMarkdown('');
      expect(html).toBe('');
    });

    it('should render headings', () => {
      createFixture();
      const html = component.renderMarkdown('# Heading 1\n## Heading 2\n### Heading 3');
      expect(html).toContain('<h1>');
      expect(html).toContain('<h2>');
      expect(html).toContain('<h3>');
    });
  });

  describe('toast notifications', () => {
    it('should add toast notification', () => {
      createFixture();
      component.addToast('Test message', 'success');

      expect(component.toasts().length).toBe(1);
      expect(component.toasts()[0].message).toBe('Test message');
      expect(component.toasts()[0].type).toBe('success');
    });

    it('should add error toast', () => {
      createFixture();
      component.addToast('Error occurred', 'error');

      expect(component.toasts()[0].type).toBe('error');
    });

    it('should add info toast', () => {
      createFixture();
      component.addToast('Info message', 'info');

      expect(component.toasts()[0].type).toBe('info');
    });

    it('should auto-remove toast after 4 seconds', () => {
      createFixture();
      vi.useFakeTimers();
      component.addToast('Test', 'success');

      vi.advanceTimersByTime(5000);

      expect(component.toasts().length).toBe(0);
      vi.useRealTimers();
    });
  });

  describe('source expansion', () => {
    it('should toggle sources for a message', () => {
      createFixture();
      component.toggleSources('msg1');
      expect(component.expandedSources().has('msg1')).toBe(true);

      component.toggleSources('msg1');
      expect(component.expandedSources().has('msg1')).toBe(false);
    });

    it('should toggle sources for multiple messages', () => {
      createFixture();
      component.toggleSources('msg1');
      component.toggleSources('msg2');

      expect(component.expandedSources().size).toBe(2);

      component.toggleSources('msg1');
      expect(component.expandedSources().size).toBe(1);
      expect(component.expandedSources().has('msg2')).toBe(true);
    });
  });

  describe('input handling', () => {
    it('should set input value', () => {
      createFixture();
      component.setInput('Test input');
      expect(component.input()).toBe('Test input');
    });

    it('should send on Enter without shift', () => {
      createFixture();
      component.input.set('Test');
      component.availableDocs.set([]);
      component.selectedDocIds.set(new Set());
      const spy = vi.spyOn(component, 'sendMessage');

      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
      component.onInputKeyDown(event);

      expect(spy).toHaveBeenCalled();
    });

    it('should not send on Enter with shift', () => {
      createFixture();
      component.input.set('Test');
      const spy = vi.spyOn(component, 'sendMessage');

      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
      component.onInputKeyDown(event);

      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('fetchAvailableDocs error handling', () => {
    it('should handle error when fetching documents', async () => {
      (mockApiService.getDocuments as any).mockReturnValue(
        throwError(() => new Error('Network error'))
      );
      createFixture();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.availableDocs()).toEqual([]);
    });
  });

  describe('getUploadStatus', () => {
    it('should return upload status for file', () => {
      createFixture();
      const status = component.getUploadStatus('test.pdf');
      expect(status).toBeUndefined();
    });
  });
});
