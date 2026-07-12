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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should map messages to bubble items with roles', () => {
    const items = component.bubbleItems();

    expect(items).toHaveLength(2);
    expect(items[0].role).toBe('user');
    expect(items[0].placement).toBeUndefined();
    expect(items[1].role).toBe('assistant');
    expect(items[1].footerRender).toBeTruthy();
  });

  it('should detect streaming state by message id', () => {
    expect(component.isStreaming('assistant-1')).toBe(true);
    expect(component.isStreaming('user-1')).toBe(false);
  });

  it('should set loading when assistant is streaming with empty content', () => {
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
