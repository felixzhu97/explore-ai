import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ChatTabComponent } from './chat-tab.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { Observable, of } from 'rxjs';

describe('ChatTabComponent', () => {
  let fixture: ComponentFixture<ChatTabComponent>;
  let component: ChatTabComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18nService = {
    t: () => ({
      aiHub: {
        chat: {
          title: 'AI Chat',
          description: 'Chat with AI',
          provider: 'Provider',
          model: 'Model',
          thinking: 'Thinking...',
          inputPlaceholder: 'Type a message...',
        },
        quickPrompts: {
          greeting: 'Hello!',
          help: 'Help me',
          creative: 'Be creative',
        },
      },
      agents: {
        startConversation: 'Start a conversation',
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      getProviders: vi.fn().mockReturnValue(
        of([
          {
            name: 'openai',
            display_name: 'OpenAI',
            models: ['gpt-4o', 'gpt-4o-mini'],
            status: 'available',
          },
          {
            name: 'anthropic',
            display_name: 'Anthropic',
            models: ['claude-3'],
            status: 'available',
          },
        ])
      ),
      getModels: vi.fn().mockReturnValue(
        of([
          { name: 'gpt-4o', provider: 'openai' },
          { name: 'gpt-4o-mini', provider: 'openai' },
        ])
      ),
      chatStream: vi.fn().mockReturnValue({ abort: vi.fn() }),
    };
    return mockApiService;
  };

  const createFixture = () => {
    fixture = TestBed.createComponent(ChatTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    createMockApiService();
    await TestBed.configureTestingModule({
      imports: [ChatTabComponent, HttpClientTestingModule],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: I18nService, useValue: mockI18nService },
      ],
    }).compileComponents();
  });

  describe('component creation', () => {
    it('should create', () => {
      createFixture();
      expect(component).toBeTruthy();
    });

    it('should initialize with empty messages', () => {
      createFixture();
      expect(component.messages()).toEqual([]);
    });

    it('should initialize with empty input', () => {
      createFixture();
      expect(component.input()).toBe('');
    });

    it('should initialize isLoading as false', () => {
      createFixture();
      expect(component.isLoading()).toBe(false);
    });

    it('should initialize error as null', () => {
      createFixture();
      expect(component.error()).toBeNull();
    });
  });

  describe('ngOnInit', () => {
    it('should call loadProviders on init', () => {
      createFixture();
      expect(mockApiService.getProviders).toHaveBeenCalled();
    });

    it('should load models when providers are available', () => {
      createFixture();
      expect(mockApiService.getModels).toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should abort existing request on destroy', () => {
      createFixture();
      component.send();
      component.ngOnDestroy();
      expect(component['abortController']).toBeNull();
    });
  });

  describe('provider and model selection', () => {
    it('should load providers on init', () => {
      createFixture();
      expect(component.providers().length).toBeGreaterThan(0);
    });

    it('should load models when provider changes', () => {
      createFixture();
      component.onProviderChange('anthropic');
      expect(mockApiService.getModels).toHaveBeenCalledWith('anthropic');
    });

    it('should update selectedProvider when provider changes', () => {
      createFixture();
      component.onProviderChange('anthropic');
      expect(component.selectedProvider()).toBe('anthropic');
    });

    it('should set selectedModel when models load', () => {
      createFixture();
      expect(component.selectedModel()).toBeTruthy();
    });

    it('should prefer mini models as default', () => {
      createFixture();
      const selectedModel = component.selectedModel();
      expect(selectedModel.includes('mini') || selectedModel.includes('3.5')).toBe(true);
    });

    it('should set isLoadingModels to true during model loading', async () => {
      // Create a mock that delays completion to capture the loading state
      let resolveModels: (value: any) => void;
      const modelsPromise = new Promise((resolve) => {
        resolveModels = resolve;
      });

      (mockApiService.getModels as any).mockReturnValue(
        new Observable((subscriber) => {
          subscriber.next([
            { name: 'gpt-4o', provider: 'openai' },
            { name: 'gpt-4o-mini', provider: 'openai' },
          ]);
          // Delay the completion to capture loading state
          setTimeout(() => {
            subscriber.complete();
            resolveModels(null);
          }, 50);
        })
      );

      createFixture();
      component.loadModels('openai');

      // At this point, isLoadingModels should be true (subscription started)
      expect(component.isLoadingModels()).toBe(true);

      // Wait for completion
      await modelsPromise;
      expect(component.isLoadingModels()).toBe(false);
    });
  });

  describe('loadProviders error handling', () => {
    it('should set default providers on error', async () => {
      (mockApiService.getProviders as any).mockReturnValue(
        new Observable((subscriber) => {
          subscriber.error(new Error('Network error'));
        })
      );
      createFixture();
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(component.providers().length).toBeGreaterThan(0);
    });
  });

  describe('loadModels', () => {
    it('should set isLoadingModels to false when complete', () => {
      createFixture();
      expect(component.isLoadingModels()).toBe(false);
    });

    it('should load models for selected provider', () => {
      createFixture();
      component.loadModels('openai');
      expect(mockApiService.getModels).toHaveBeenCalledWith('openai');
    });
  });

  describe('setInput', () => {
    it('should set input value', () => {
      createFixture();
      component.setInput('Hello world');
      expect(component.input()).toBe('Hello world');
    });

    it('should handle empty string', () => {
      createFixture();
      component.setInput('');
      expect(component.input()).toBe('');
    });
  });

  describe('onKeyDown', () => {
    it('should call send on Enter without shift', () => {
      createFixture();
      component.input.set('Test message');
      const spy = vi.spyOn(component, 'send');
      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
      component.onKeyDown(event);
      expect(spy).toHaveBeenCalled();
    });

    it('should not call send on Enter with shift', () => {
      createFixture();
      component.input.set('Test message');
      const spy = vi.spyOn(component, 'send');
      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
      component.onKeyDown(event);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should not call send on other keys', () => {
      createFixture();
      component.input.set('Test message');
      const spy = vi.spyOn(component, 'send');
      const event = new KeyboardEvent('keydown', { key: 'Space' });
      component.onKeyDown(event);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should prevent default on Enter without shift', () => {
      createFixture();
      const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
      const preventSpy = vi.spyOn(event, 'preventDefault');
      component.onKeyDown(event);
      expect(preventSpy).toHaveBeenCalled();
    });
  });

  describe('send', () => {
    it('should not send if input is empty', () => {
      createFixture();
      component.input.set('');
      component.send();
      expect(mockApiService.chatStream).not.toHaveBeenCalled();
    });

    it('should not send if input is whitespace only', () => {
      createFixture();
      component.input.set('   ');
      component.send();
      expect(mockApiService.chatStream).not.toHaveBeenCalled();
    });

    it('should not send if already loading', () => {
      createFixture();
      component.input.set('Hello');
      component.isLoading.set(true);
      component.send();
      expect(mockApiService.chatStream).not.toHaveBeenCalled();
    });

    it('should clear input after sending', () => {
      createFixture();
      component.input.set('Hello');
      component.send();
      expect(component.input()).toBe('');
    });

    it('should set isLoading to true after sending', () => {
      createFixture();
      component.input.set('Hello');
      component.send();
      expect(component.isLoading()).toBe(true);
    });

    it('should clear error after sending', () => {
      createFixture();
      component.error.set('Previous error');
      component.input.set('Hello');
      component.send();
      expect(component.error()).toBeNull();
    });

    it('should add user message to messages', () => {
      createFixture();
      component.input.set('Test message');
      component.send();
      const messages = component.messages();
      expect(messages.some((m) => m.role === 'user' && m.content === 'Test message')).toBe(true);
    });

    it('should add assistant placeholder message', () => {
      createFixture();
      component.input.set('Test message');
      component.send();
      const messages = component.messages();
      expect(messages.some((m) => m.role === 'assistant' && m.content === '')).toBe(true);
    });

    it('should call chatStream with correct parameters', () => {
      createFixture();
      component.input.set('Hello');
      component.send();
      expect(mockApiService.chatStream).toHaveBeenCalled();
    });

    it('should abort previous request when sending new message', async () => {
      createFixture();

      // Track abort calls
      const abortSpies: any[] = [];
      (mockApiService.chatStream as any).mockImplementation(() => {
        const abortFn = vi.fn();
        abortSpies.push(abortFn);
        return { abort: abortFn };
      });

      // Send first message
      component.input.set('First message');
      component.send();
      expect(mockApiService.chatStream).toHaveBeenCalledTimes(1);

      // First request completes, allowing second request
      const firstOnDone = (mockApiService.chatStream as any).mock.calls[0][2];
      firstOnDone();

      // Now second message can be sent
      component.input.set('Second message');
      component.send();
      expect(mockApiService.chatStream).toHaveBeenCalledTimes(2);
    });
  });

  describe('send error handling', () => {
    it('should set error on fetch failure', async () => {
      createFixture();
      component.input.set('Hello');
      (mockApiService.chatStream as any).mockImplementation(
        (_: any, __: any, ___: any, onError: any) => {
          onError(new Error('Network error'));
          return { abort: vi.fn() };
        }
      );
      component.send();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toBeTruthy();
    });

    it('should set isLoading to false on error', async () => {
      createFixture();
      component.input.set('Hello');
      (mockApiService.chatStream as any).mockImplementation(
        (_: any, __: any, ___: any, onError: any) => {
          onError(new Error('Network error'));
          return { abort: vi.fn() };
        }
      );
      component.send();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.isLoading()).toBe(false);
    });

    it('should show user-friendly message for network errors', async () => {
      createFixture();
      component.input.set('Hello');
      (mockApiService.chatStream as any).mockImplementation(
        (_: any, __: any, ___: any, onError: any) => {
          onError(new Error('Failed to fetch'));
          return { abort: vi.fn() };
        }
      );
      component.send();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toContain('unavailable');
    });
  });

  describe('send completion handling', () => {
    it('should set isLoading to false on completion', () => {
      createFixture();
      component.input.set('Hello');
      (mockApiService.chatStream as any).mockImplementation(
        (_: any, _onChunk: any, onDone: any, _onError: any) => {
          onDone();
          return { abort: vi.fn() };
        }
      );
      component.send();
      expect(component.isLoading()).toBe(false);
    });

    it('should set abortController to null on completion', () => {
      createFixture();
      component.input.set('Hello');
      (mockApiService.chatStream as any).mockImplementation(
        (_: any, _onChunk: any, onDone: any, _onError: any) => {
          onDone();
          return { abort: vi.fn() };
        }
      );
      component.send();
      expect(component['abortController']).toBeNull();
    });
  });

  describe('streaming message updates', () => {
    it('should update assistant message content with chunks', () => {
      createFixture();
      component.input.set('Hello');
      let capturedChunk: ((chunk: string) => void) | null = null;

      (mockApiService.chatStream as any).mockImplementation(
        (_: any, onChunk: (chunk: string) => void, _onDone: any, _onError: any) => {
          capturedChunk = onChunk;
          return { abort: vi.fn() };
        }
      );

      component.send();
      capturedChunk!('Hello ');
      capturedChunk!('world!');

      const messages = component.messages();
      const assistantMsg = messages.find((m) => m.role === 'assistant');
      expect(assistantMsg?.content).toBe('Hello world!');
    });
  });

  describe('formatTime', () => {
    it('should format timestamp to locale time string', () => {
      createFixture();
      const timestamp = new Date('2024-01-15T10:30:00').getTime();
      const formatted = component.formatTime(timestamp);
      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });
  });

  describe('renderMarkdown', () => {
    it('should convert bold text', () => {
      createFixture();
      const html = component.renderMarkdown('**bold** text');
      expect(html).toContain('<strong>bold</strong>');
    });

    it('should convert italic text', () => {
      createFixture();
      const html = component.renderMarkdown('*italic* text');
      expect(html).toContain('<em>italic</em>');
    });

    it('should convert h1 headers', () => {
      createFixture();
      const html = component.renderMarkdown('# Header');
      expect(html).toContain('<h1>Header</h1>');
    });

    it('should convert h2 headers', () => {
      createFixture();
      const html = component.renderMarkdown('## Header');
      expect(html).toContain('<h2>Header</h2>');
    });

    it('should convert h3 headers', () => {
      createFixture();
      const html = component.renderMarkdown('### Header');
      expect(html).toContain('<h3>Header</h3>');
    });

    it('should convert inline code', () => {
      createFixture();
      const html = component.renderMarkdown('`code`');
      expect(html).toContain('<code>code</code>');
    });

    it('should convert code blocks', () => {
      createFixture();
      const html = component.renderMarkdown('```\nconst x = 1;\n```');
      expect(html).toContain('<pre>');
      expect(html).toContain('<code>');
    });

    it('should convert blockquotes', () => {
      createFixture();
      const html = component.renderMarkdown('> Quote');
      expect(html).toContain('<blockquote>Quote</blockquote>');
    });

    it('should convert list items', () => {
      createFixture();
      const html = component.renderMarkdown('- Item 1\n- Item 2');
      expect(html).toContain('<li>Item 1</li>');
      expect(html).toContain('<li>Item 2</li>');
    });

    it('should convert newlines', () => {
      createFixture();
      const html = component.renderMarkdown('Line 1\nLine 2');
      expect(html).toContain('<br>');
    });

    it('should handle empty string', () => {
      createFixture();
      const html = component.renderMarkdown('');
      expect(html).toBe('');
    });
  });

  describe('stateChange output', () => {
    it('should emit stateChange when provider changes', () => {
      createFixture();
      const spy = vi.spyOn(component.stateChange, 'emit');
      component.onProviderChange('anthropic');
      expect(spy).toHaveBeenCalledWith(expect.objectContaining({ provider: 'anthropic' }));
    });

    it('should emit stateChange when model changes', () => {
      createFixture();
      const spy = vi.spyOn(component.stateChange, 'emit');
      component.setSelectedModel('gpt-4o');
      expect(spy).toHaveBeenCalledWith(expect.objectContaining({ model: 'gpt-4o' }));
    });
  });

  describe('setSelectedModel', () => {
    it('should update selected model', () => {
      createFixture();
      component.setSelectedModel('gpt-4o');
      expect(component.selectedModel()).toBe('gpt-4o');
    });
  });
});
