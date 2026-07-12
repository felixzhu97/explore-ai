import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatBubbleListComponent } from './chat-bubble-list.component';
import { ChatBubbleMessage } from './chat-bubble.model';

describe('ChatBubbleListComponent', () => {
  let fixture: ComponentFixture<ChatBubbleListComponent>;
  let component: ChatBubbleListComponent;

  const messages: ChatBubbleMessage[] = [
    {
      id: 'user-1',
      role: 'user',
      content: 'Hello',
      timestamp: Date.now(),
    },
    {
      id: 'assistant-1',
      role: 'assistant',
      content: 'Hi there',
      timestamp: Date.now(),
      sources: [{ text: 'Source text', score: 0.9 }],
      sourcesExpanded: false,
    },
  ];

  beforeEach(async () => {
    Element.prototype.scrollTo = vi.fn() as unknown as Element['scrollTo'];

    await TestBed.configureTestingModule({
      imports: [ChatBubbleListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatBubbleListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('messages', messages);
    fixture.componentRef.setInput('streamingMessageId', 'assistant-1');
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should return empty bubble items when template refs are unavailable', () => {
    vi.spyOn(component, 'userMessageTpl').mockReturnValue(undefined);

    expect(component.bubbleItems()).toEqual([]);
  });

  it('should map messages to bubble items with roles', () => {
    fixture.detectChanges();

    const items = component.bubbleItems();

    expect(items).toHaveLength(2);
    expect(items[0].role).toBe('user');
    expect(items[0].placement).toBeUndefined();
    expect(items[1].role).toBe('assistant');
    expect(items[1].footerRender).toBeTruthy();
  });

  it('should detect streaming state by message id', () => {
    fixture.detectChanges();

    expect(component.isStreaming('assistant-1')).toBe(true);
    expect(component.isStreaming('user-1')).toBe(false);
  });

  it('should treat missing message key info as empty id', () => {
    expect(component.messageKey(undefined)).toBe('');
  });

  it('should not throw when streaming message ids are unavailable', () => {
    fixture.componentRef.setInput('streamingMessageIds', null as unknown as ReadonlySet<string>);
    fixture.detectChanges();

    expect(component.isStreaming('assistant-1')).toBe(true);
    expect(component.isStreaming('user-1')).toBe(false);
  });

  it('should set loading when assistant is streaming with empty content', () => {
    fixture.detectChanges();

    fixture.componentRef.setInput('messages', [
      {
        id: 'assistant-2',
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      },
    ]);
    fixture.componentRef.setInput('streamingMessageId', 'assistant-2');
    fixture.detectChanges();

    const items = component.bubbleItems();
    expect(items[0].loading).toBe(true);
  });
});
