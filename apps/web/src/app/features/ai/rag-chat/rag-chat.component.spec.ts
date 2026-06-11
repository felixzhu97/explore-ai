import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RagChatComponent } from './rag-chat.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { of } from 'rxjs';

describe('RagChatComponent', () => {
  let fixture: ComponentFixture<RagChatComponent>;
  let component: RagChatComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18nService = {
    t: () => ({
      ragChat: {
        title: 'RAG Chat',
        placeholder: 'Ask about your documents...',
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      getDocuments: vi.fn().mockReturnValue(of({ documents: [] })),
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
  });

  describe('document management', () => {
    it('should toggle document selection', () => {
      createFixture();
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
  });

  describe('toast notifications', () => {
    it('should add toast notification', () => {
      createFixture();
      component.addToast('Test message', 'success');
      
      expect(component.toasts().length).toBe(1);
      expect(component.toasts()[0].message).toBe('Test message');
      expect(component.toasts()[0].type).toBe('success');
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
  });
});
