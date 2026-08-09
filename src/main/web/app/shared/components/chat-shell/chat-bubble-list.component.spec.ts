import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
      sources: [
        {
          text: 'Snippet about AI',
          score: 0.9,
          url: 'https://www.example.com/ai',
          title: 'Example Source',
        },
      ],
    },
  ];

  beforeEach(async () => {
    vi.useFakeTimers();
    Element.prototype.scrollTo = vi.fn() as unknown as Element['scrollTo'];

    await TestBed.configureTestingModule({
      imports: [ChatBubbleListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatBubbleListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('messages', messages);
    fixture.componentRef.setInput('streamingMessageId', 'assistant-1');
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function stubChipRect(chip: HTMLButtonElement): void {
    Object.defineProperty(chip, 'getBoundingClientRect', {
      configurable: true,
      value: () => ({
        left: 40,
        top: 200,
        right: 120,
        bottom: 220,
        width: 80,
        height: 20,
        x: 40,
        y: 200,
        toJSON: () => ({}),
      }) as DOMRect,
    });
  }

  function openChipPopover(chip: HTMLButtonElement): void {
    stubChipRect(chip);
    chip.dispatchEvent(new Event('pointerenter', { bubbles: true }));
    vi.advanceTimersByTime(200);
    fixture.detectChanges();
  }

  it('should create component', () => {
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

  it('should detect streaming state when message id matches', () => {
    fixture.detectChanges();

    expect(component.isStreaming('assistant-1')).toBe(true);
    expect(component.isStreaming('user-1')).toBe(false);
  });

  it('should return empty id when message key info is missing', () => {
    expect(component.messageKey(undefined)).toBe('');
  });

  it('should fallback to single streaming id when streaming ids are unavailable', () => {
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

  it('should collapse long user messages when enabled', () => {
    const longContent = 'x'.repeat(200);
    fixture.componentRef.setInput('messages', [
      {
        id: 'user-long',
        role: 'user',
        content: longContent,
        timestamp: Date.now(),
      },
    ]);
    fixture.componentRef.setInput('collapseLongUserMessages', true);
    fixture.detectChanges();

    const message = component.messageById('user-long')!;
    expect(component.isLongUserMessage(message)).toBe(true);
    expect(component.userMessageText(message).endsWith('…')).toBe(true);

    component.toggleUserExpanded('user-long');
    expect(component.userMessageText(message)).toBe(longContent);
  });

  it('should render hostname pill when source has url', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement | null;
    expect(chip).toBeTruthy();
    expect(chip?.textContent?.replace(/\s+/g, ' ').trim()).toContain('example.com');
    expect(chip?.textContent).not.toContain('[1]');

    const icon = chip?.querySelector('img') as HTMLImageElement | null;
    expect(icon?.src).toContain('favicons');
    expect(icon?.src).toContain('example.com');
  });

  it('should open popover below chip without body when space is available', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    // Plenty of room below (chip near top of a tall viewport).
    vi.stubGlobal('innerHeight', 900);
    vi.stubGlobal('innerWidth', 1200);
    chip.getBoundingClientRect = () => ({
      left: 40,
      top: 80,
      right: 120,
      bottom: 100,
      width: 80,
      height: 20,
      x: 40,
      y: 80,
      toJSON: () => ({}),
    }) as DOMRect;
    chip.dispatchEvent(new Event('pointerenter', { bubbles: true }));
    fixture.detectChanges();
    // Chip highlight is immediate; panel waits for open delay.
    expect(chip.className).toContain('bg-foreground');
    expect(chip.className).toContain('text-background');
    expect(component.openRef()).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-source-popover]')).toBeNull();

    vi.advanceTimersByTime(200);
    fixture.detectChanges();

    const popover = fixture.nativeElement.querySelector('[data-source-popover]') as HTMLElement | null;
    expect(popover).toBeTruthy();
    expect(popover?.textContent).toContain('example.com');
    expect(popover?.textContent).toContain('Example Source');
    expect(popover?.textContent).not.toContain('Snippet about AI');

    const ref = component.openRef();
    expect(ref).not.toBeNull();
    expect(ref!.x).toBe(40);
    expect(ref!.y).toBe(102); // bottom 100 + gap 2
    expect(popover?.textContent).not.toContain('Open');
    expect(popover?.querySelector('a[aria-label]')).toBeNull();
  });

  it('should show published date in popover when source has publishedAt', () => {
    fixture.componentRef.setInput('messages', [
      {
        id: 'assistant-dated',
        role: 'assistant',
        content: 'Answer',
        timestamp: Date.now(),
        sources: [
          {
            text: 'Snippet',
            score: 0.9,
            url: 'https://www.example.com/ai',
            title: 'Example Source',
            publishedAt: 'Jul 19, 2026',
          },
        ],
      },
    ]);
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    vi.stubGlobal('innerHeight', 900);
    vi.stubGlobal('innerWidth', 1200);
    openChipPopover(chip);

    const popover = fixture.nativeElement.querySelector('[data-source-popover]') as HTMLElement;
    expect(popover.textContent).toContain('Jul 19, 2026');
    expect(popover.textContent).not.toMatch(/打开|Open/);
  });

  it('should omit published date in popover when source has no publishedAt', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    vi.stubGlobal('innerHeight', 900);
    vi.stubGlobal('innerWidth', 1200);
    openChipPopover(chip);

    const popover = fixture.nativeElement.querySelector('[data-source-popover]') as HTMLElement;
    expect(popover.textContent).toContain('Example Source');
    expect(popover.textContent).not.toContain('Jul 19');
  });

  it('should open popover above chip when near viewport bottom', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    vi.stubGlobal('innerHeight', 400);
    vi.stubGlobal('innerWidth', 1200);
    chip.getBoundingClientRect = () => ({
      left: 60,
      top: 320,
      right: 140,
      bottom: 340,
      width: 80,
      height: 20,
      x: 60,
      y: 320,
      toJSON: () => ({}),
    }) as DOMRect;
    chip.dispatchEvent(new Event('pointerenter', { bubbles: true }));
    vi.advanceTimersByTime(200);
    fixture.detectChanges();

    const ref = component.openRef();
    expect(ref).not.toBeNull();
    expect(ref!.x).toBe(60);
    // Prefer above (estimate): top - estHeight - gap = 320 - 96 - 2 = 222
    expect(ref!.y).toBe(222);
  });

  it('should re-anchor popover flush to chip when measured height differs', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    vi.stubGlobal('innerHeight', 400);
    vi.stubGlobal('innerWidth', 1200);
    chip.getBoundingClientRect = () => ({
      left: 60,
      top: 320,
      right: 140,
      bottom: 340,
      width: 80,
      height: 20,
      x: 60,
      y: 320,
      toJSON: () => ({}),
    }) as DOMRect;
    chip.dispatchEvent(new Event('pointerenter', { bubbles: true }));
    vi.advanceTimersByTime(200);
    fixture.detectChanges();

    const popover = fixture.nativeElement.querySelector('[data-source-popover]') as HTMLElement;
    popover.getBoundingClientRect = () => ({
      left: 60,
      top: 0,
      right: 380,
      bottom: 72,
      width: 320,
      height: 72,
      x: 60,
      y: 0,
      toJSON: () => ({}),
    }) as DOMRect;

    // Measured height 72 → top = 320 - 72 - 2 = 246 (flush above chip)
    interface WithRefine { refinePopoverPosition: () => void }
    (component as unknown as WithRefine).refinePopoverPosition();
    expect(component.openRef()?.y).toBe(246);
  });

  it('should align popover to hovered chip when pointer enters', () => {
    fixture.componentRef.setInput('messages', [
      {
        id: 'assistant-multi',
        role: 'assistant',
        content: 'Answer',
        timestamp: Date.now(),
        sources: [
          {
            text: 'One',
            score: 0.9,
            url: 'https://www.example.com/a',
            title: 'A',
          },
          {
            text: 'Two',
            score: 0.8,
            url: 'https://www.example.com/b',
            title: 'B',
          },
        ],
      },
    ]);
    fixture.detectChanges();
    vi.stubGlobal('innerHeight', 900);
    vi.stubGlobal('innerWidth', 1200);

    const buttons = fixture.nativeElement.querySelectorAll(
      '[data-source-chips] button',
    ) as NodeListOf<HTMLButtonElement>;
    buttons[0].getBoundingClientRect = () => ({
      left: 40,
      top: 80,
      right: 100,
      bottom: 100,
      width: 60,
      height: 20,
      x: 40,
      y: 80,
      toJSON: () => ({}),
    }) as DOMRect;
    buttons[1].getBoundingClientRect = () => ({
      left: 160,
      top: 80,
      right: 240,
      bottom: 100,
      width: 80,
      height: 20,
      x: 160,
      y: 80,
      toJSON: () => ({}),
    }) as DOMRect;

    buttons[0].dispatchEvent(new Event('pointerenter', { bubbles: true }));
    vi.advanceTimersByTime(200);
    fixture.detectChanges();
    expect(component.openRef()?.x).toBe(40);

    // Already open: switching chips updates immediately (no second open delay).
    buttons[1].dispatchEvent(new Event('pointerenter', { bubbles: true }));
    fixture.detectChanges();
    expect(component.openRef()?.x).toBe(160);
    expect(component.openRef()?.index).toBe(1);
  });

  it('should open url and close popover when chip with url is clicked', () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    openChipPopover(chip);
    expect(component.openRef()).not.toBeNull();

    chip.click();
    fixture.detectChanges();

    expect(openSpy).toHaveBeenCalledWith(
      'https://www.example.com/ai',
      '_blank',
      'noopener,noreferrer',
    );
    expect(component.openRef()).toBeNull();
    openSpy.mockRestore();
  });

  it('should close popover when title link is clicked', () => {
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    openChipPopover(chip);
    expect(component.openRef()).not.toBeNull();

    const jump = fixture.nativeElement.querySelector(
      '[data-source-popover] a[href="https://www.example.com/ai"]',
    ) as HTMLAnchorElement;
    expect(jump).toBeTruthy();
    jump.addEventListener('click', event => event.preventDefault());
    jump.click();
    fixture.detectChanges();

    expect(component.openRef()).toBeNull();
  });

  it('should not open window when source has no url on chip click', () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    fixture.componentRef.setInput('messages', [
      {
        id: 'assistant-rag',
        role: 'assistant',
        content: 'Answer',
        timestamp: Date.now(),
        sources: [{ text: 'Document chunk about RAG', score: 0.82 }],
      },
    ]);
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-source-chips] button') as HTMLButtonElement;
    openChipPopover(chip);
    chip.click();
    fixture.detectChanges();

    expect(openSpy).not.toHaveBeenCalled();
    openSpy.mockRestore();
  });
});
