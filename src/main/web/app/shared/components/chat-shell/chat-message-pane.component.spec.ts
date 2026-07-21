import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

vi.mock('../markdown-with-a2ui.component', async () => {
  const { Component, input } = await import('@angular/core');

  @Component({
    selector: 'app-markdown-with-a2ui',
    template: '',
  })
  class MarkdownWithA2uiComponent {
    readonly content = input.required<string>();
    readonly streaming = input(false);
  }

  return { MarkdownWithA2uiComponent };
});

import { ChatMessagePaneComponent } from './chat-message-pane.component';
import { ChatBubbleMessage } from './chat-bubble.model';

describe('ChatMessagePaneComponent', () => {
  let fixture: ComponentFixture<ChatMessagePaneComponent>;

  beforeEach(async () => {
    Element.prototype.scrollTo = vi.fn() as unknown as Element['scrollTo'];

    await TestBed.configureTestingModule({
      imports: [ChatMessagePaneComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatMessagePaneComponent);
  });

  it('should_showWelcomePanel_when_messagesEmptyAndNoEmptyText', () => {
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('welcomeTitle', 'Welcome');
    fixture.componentRef.setInput('welcomeDescription', 'Start');
    fixture.detectChanges();

    const welcome = fixture.nativeElement.querySelector('app-chat-welcome-panel');
    expect(welcome).toBeTruthy();
  });

  it('should_showEmptyText_when_messagesEmptyAndEmptyTextSet', () => {
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('emptyText', 'No results yet');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No results yet');
    expect(
      fixture.nativeElement.querySelector('app-chat-welcome-panel'),
    ).toBeNull();
  });

  it('should_showBubbleList_when_messagesPresent', () => {
    const messages: ChatBubbleMessage[] = [
      { id: '1', role: 'user', content: 'Hello' },
    ];
    fixture.componentRef.setInput('messages', messages);
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('app-chat-bubble-list'),
    ).toBeTruthy();
  });

  it('should_emitPromptSelect_when_welcomePromptChosen', () => {
    const spy = vi.fn();
    fixture.componentInstance.promptSelect.subscribe(spy);
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('welcomeTitle', 'Welcome');
    fixture.componentRef.setInput('welcomeDescription', 'Start');
    fixture.detectChanges();

    fixture.componentInstance.promptSelect.emit('Ask something');

    expect(spy).toHaveBeenCalledWith('Ask something');
  });
});
