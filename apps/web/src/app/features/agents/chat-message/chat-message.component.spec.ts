import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatMessageComponent, ChatMessageData } from './chat-message.component';
import { ToolCall } from '../tool-result/tool-result.component';

describe('ChatMessageComponent', () => {
  let fixture: ComponentFixture<ChatMessageComponent>;
  let component: ChatMessageComponent;

  const createMessage = (overrides: Partial<ChatMessageData> = {}): ChatMessageData => ({
    id: 'msg_1',
    role: 'assistant',
    content: 'Hello, how can I help you?',
    timestamp: Date.now(),
    ...overrides,
  });

  const createFixture = (messageData: ChatMessageData) => {
    fixture = TestBed.createComponent(ChatMessageComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('message', messageData);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatMessageComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture(createMessage());
    expect(component).toBeTruthy();
  });

  it('should display user message content', () => {
    createFixture(
      createMessage({
        role: 'user',
        content: 'Hello, this is a user message',
      })
    );

    const content = fixture.nativeElement.querySelector('div > div');
    expect(content.textContent?.trim()).toContain('Hello, this is a user message');
  });

  it('should display assistant message with rendered content', () => {
    createFixture(
      createMessage({
        role: 'assistant',
        content: 'This is an assistant response',
      })
    );

    const content = fixture.nativeElement.querySelector('div > div');
    expect(content.textContent?.trim()).toContain('This is an assistant response');
  });

  it('should format timestamp correctly', () => {
    const testTimestamp = new Date('2024-01-15T10:30:00').getTime();
    createFixture(createMessage({ timestamp: testTimestamp }));

    const timeElement = fixture.nativeElement.querySelector('.text-\\[11px\\]');
    expect(timeElement.textContent).toBeTruthy();
  });

  it('should render tool calls when provided', () => {
    const toolCalls: ToolCall[] = [
      {
        id: 'tool_1',
        name: 'get_weather',
        input: { location: 'San Francisco' },
        output: '{"temperature": 72}',
        status: 'success',
      },
    ];
    createFixture(createMessage({ toolCalls }));

    const toolResults = fixture.nativeElement.querySelectorAll('app-tool-result');
    expect(toolResults.length).toBe(1);
  });

  it('should not render tool calls section when no tool calls', () => {
    createFixture(createMessage({ toolCalls: undefined }));

    const toolCallsSection = fixture.nativeElement.querySelector('.mt-2');
    expect(toolCallsSection).toBeFalsy();
  });

  it('should handle empty content gracefully', () => {
    createFixture(createMessage({ content: '' }));

    const content = fixture.nativeElement.querySelector('div > div');
    expect(content).toBeTruthy();
  });

  it('should escape HTML in user content', () => {
    createFixture(
      createMessage({
        role: 'user',
        content: '<script>alert("xss")</script>',
      })
    );

    const content = fixture.nativeElement.querySelector('div > div');
    expect(content.innerHTML).not.toContain('<script>');
    expect(content.textContent).toContain('<script>alert("xss")</script>');
  });

  describe('isUser computed', () => {
    it('should return true for user messages', () => {
      createFixture(createMessage({ role: 'user' }));
      expect(component.isUser()).toBe(true);
    });

    it('should return false for assistant messages', () => {
      createFixture(createMessage({ role: 'assistant' }));
      expect(component.isUser()).toBe(false);
    });

    it('should return false for system messages', () => {
      createFixture(createMessage({ role: 'system' }));
      expect(component.isUser()).toBe(false);
    });
  });

  describe('formattedTime computed', () => {
    it('should format time using locale format', () => {
      const timestamp = new Date('2024-01-15T14:30:45').getTime();
      createFixture(createMessage({ timestamp }));

      const formatted = component.formattedTime();
      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });
  });

  describe('renderedContent computed', () => {
    it('should return empty string for empty content', () => {
      createFixture(createMessage({ content: '' }));
      expect(component.renderedContent()).toBe('');
    });

    it('should render markdown headers', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: '# Hello\n## World',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<h1>');
      expect(content.innerHTML).toContain('<h2>');
    });

    it('should render bold text', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: 'This is **bold** text',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<strong>');
      expect(content.innerHTML).toContain('bold');
    });

    it('should render italic text', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: 'This is *italic* text',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<em>');
      expect(content.innerHTML).toContain('italic');
    });

    it('should render inline code', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: 'Use `console.log()` for debugging',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<code>console.log()</code>');
    });

    it('should render code blocks', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: '```javascript\nconst x = 1;\n```',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<pre');
      expect(content.innerHTML).toContain('<code>');
      expect(content.innerHTML).toContain('const x = 1');
    });

    it('should render links', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: 'Check [this link](https://example.com)',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('<a href="https://example.com"');
      expect(content.innerHTML).toContain('this link');
    });

    it('should handle JSON content with syntax highlighting', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: '{"name": "John", "age": 30}',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.innerHTML).toContain('json-key');
      expect(content.innerHTML).toContain('json-string');
      expect(content.innerHTML).toContain('json-number');
    });

    it('should not parse JSON when inside code blocks', () => {
      createFixture(
        createMessage({
          role: 'assistant',
          content: '```\n{"key": "value"}\n```',
        })
      );

      const content = fixture.nativeElement.querySelector('div > div');
      expect(content.textContent).toContain('{"key": "value"}');
    });
  });
});
