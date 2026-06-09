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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatMessageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatMessageComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    component.message.set(createMessage());
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display user message content', () => {
    component.message.set(createMessage({
      role: 'user',
      content: 'Hello, this is a user message',
    }));
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.message-content');
    expect(content.textContent?.trim()).toContain('Hello, this is a user message');
    expect(content).toHaveClass('message-content--user');
  });

  it('should display assistant message with rendered content', () => {
    component.message.set(createMessage({
      role: 'assistant',
      content: 'This is an assistant response',
    }));
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.message-content');
    expect(content.textContent?.trim()).toContain('This is an assistant response');
  });

  it('should format timestamp correctly', () => {
    const testTimestamp = new Date('2024-01-15T10:30:00').getTime();
    component.message.set(createMessage({ timestamp: testTimestamp }));
    fixture.detectChanges();

    const timeElement = fixture.nativeElement.querySelector('.message-time');
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
    component.message.set(createMessage({ toolCalls }));
    fixture.detectChanges();

    const toolResults = fixture.nativeElement.querySelectorAll('app-tool-result');
    expect(toolResults.length).toBe(1);
  });

  it('should not render tool calls section when no tool calls', () => {
    component.message.set(createMessage({ toolCalls: undefined }));
    fixture.detectChanges();

    const toolCallsSection = fixture.nativeElement.querySelector('.tool-calls');
    expect(toolCallsSection).toBeFalsy();
  });

  it('should handle empty content gracefully', () => {
    component.message.set(createMessage({ content: '' }));
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.message-content');
    expect(content).toBeTruthy();
  });

  it('should escape HTML in user content', () => {
    component.message.set(createMessage({
      role: 'user',
      content: '<script>alert("xss")</script>',
    }));
    fixture.detectChanges();

    const content = fixture.nativeElement.querySelector('.message-content');
    expect(content.innerHTML).not.toContain('<script>');
    expect(content.textContent).toContain('<script>alert("xss")</script>');
  });

  describe('isUser computed', () => {
    it('should return true for user messages', () => {
      component.message.set(createMessage({ role: 'user' }));
      expect(component.isUser()).toBe(true);
    });

    it('should return false for assistant messages', () => {
      component.message.set(createMessage({ role: 'assistant' }));
      expect(component.isUser()).toBe(false);
    });

    it('should return false for system messages', () => {
      component.message.set(createMessage({ role: 'system' }));
      expect(component.isUser()).toBe(false);
    });
  });

  describe('formattedTime computed', () => {
    it('should format time using locale format', () => {
      const timestamp = new Date('2024-01-15T14:30:45').getTime();
      component.message.set(createMessage({ timestamp }));
      
      const formatted = component.formattedTime();
      expect(formatted).toBeTruthy();
      expect(typeof formatted).toBe('string');
    });
  });

  describe('renderedContent computed', () => {
    it('should return empty string for empty content', () => {
      component.message.set(createMessage({ content: '' }));
      expect(component.renderedContent()).toBe('');
    });

    it('should render markdown headers', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: '# Hello\n## World',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<h1>');
      expect(content.innerHTML).toContain('<h2>');
    });

    it('should render bold text', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: 'This is **bold** text',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<strong>');
      expect(content.innerHTML).toContain('bold');
    });

    it('should render italic text', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: 'This is *italic* text',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<em>');
      expect(content.innerHTML).toContain('italic');
    });

    it('should render inline code', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: 'Use `console.log()` for debugging',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<code>console.log()</code>');
    });

    it('should render code blocks', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: '```javascript\nconst x = 1;\n```',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<pre');
      expect(content.innerHTML).toContain('<code>');
      expect(content.innerHTML).toContain('const x = 1');
    });

    it('should render links', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: 'Check [this link](https://example.com)',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('<a href="https://example.com"');
      expect(content.innerHTML).toContain('this link');
    });

    it('should handle JSON content with syntax highlighting', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: '{"name": "John", "age": 30}',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.innerHTML).toContain('json-key');
      expect(content.innerHTML).toContain('json-string');
      expect(content.innerHTML).toContain('json-number');
    });

    it('should not parse JSON when inside code blocks', () => {
      component.message.set(createMessage({
        role: 'assistant',
        content: '```\n{"key": "value"}\n```',
      }));
      fixture.detectChanges();
      
      const content = fixture.nativeElement.querySelector('.message-content');
      expect(content.textContent).toContain('{"key": "value"}');
    });
  });

  describe('message bubble styling', () => {
    it('should have user class for user messages', () => {
      component.message.set(createMessage({ role: 'user' }));
      fixture.detectChanges();
      
      const bubble = fixture.nativeElement.querySelector('.message-bubble');
      expect(bubble).toHaveClass('message-bubble--user');
    });

    it('should not have user class for assistant messages', () => {
      component.message.set(createMessage({ role: 'assistant' }));
      fixture.detectChanges();
      
      const bubble = fixture.nativeElement.querySelector('.message-bubble');
      expect(bubble).not.toHaveClass('message-bubble--user');
    });
  });

  describe('accessibility', () => {
    it('should have message-meta for timestamp', () => {
      component.message.set(createMessage());
      fixture.detectChanges();
      
      const meta = fixture.nativeElement.querySelector('.message-meta');
      expect(meta).toBeTruthy();
    });

    it('should have time element with proper formatting', () => {
      component.message.set(createMessage());
      fixture.detectChanges();
      
      const time = fixture.nativeElement.querySelector('.message-time');
      expect(time).toBeTruthy();
    });
  });
});
