import { beforeEach, describe, expect, it, vi } from 'vitest';
import { type ComponentFixture, TestBed } from '@angular/core/testing';

vi.mock('./markdown-with-a2ui.component', async () => {
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
import { type ChatMessageView } from './chat-bubble-list.component';

describe('ChatMessagePaneComponent', () => {
  let fixture: ComponentFixture<ChatMessagePaneComponent>;

  beforeEach(async () => {
    Element.prototype.scrollTo = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ChatMessagePaneComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatMessagePaneComponent);
  });

  it('should show welcome panel when messages empty and no empty text', () => {
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('welcomeTitle', 'Welcome');
    fixture.componentRef.setInput('welcomeDescription', 'Start');
    fixture.detectChanges();

    const welcome = fixture.nativeElement.querySelector('app-chat-welcome-panel');
    expect(welcome).toBeTruthy();
  });

  it('should show empty text when messages empty and empty text set', () => {
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('emptyText', 'No results yet');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No results yet');
    expect(
      fixture.nativeElement.querySelector('app-chat-welcome-panel'),
    ).toBeNull();
  });

  it('should show bubble list when messages present', () => {
    const messages: ChatMessageView[] = [
      { id: '1', role: 'user', content: 'Hello' },
    ];
    fixture.componentRef.setInput('messages', messages);
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('app-chat-bubble-list'),
    ).toBeTruthy();
  });

  it('should emit prompt selected when welcome prompt chosen', () => {
    const spy = vi.fn();
    fixture.componentInstance.promptSelected.subscribe(spy);
    fixture.componentRef.setInput('messages', []);
    fixture.componentRef.setInput('welcomeTitle', 'Welcome');
    fixture.componentRef.setInput('welcomeDescription', 'Start');
    fixture.detectChanges();

    fixture.componentInstance.promptSelected.emit('Ask something');

    expect(spy).toHaveBeenCalledWith('Ask something');
  });
});
