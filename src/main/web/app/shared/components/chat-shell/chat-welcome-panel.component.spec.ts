import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatWelcomePanelComponent } from './chat-welcome-panel.component';
import { NxPrompt } from 'ng-zorro-x/prompts';

describe('ChatWelcomePanelComponent', () => {
  let fixture: ComponentFixture<ChatWelcomePanelComponent>;

  const prompts: NxPrompt[] = [
    { key: 'a', label: 'Prompt A', description: 'Description A' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatWelcomePanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatWelcomePanelComponent);
    fixture.componentRef.setInput('title', 'Welcome');
    fixture.componentRef.setInput('description', 'Start chatting');
    fixture.componentRef.setInput('prompts', prompts);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should emit prompt label on item click', () => {
    const spy = vi.fn();
    fixture.componentInstance.promptSelect.subscribe(spy);

    fixture.componentInstance.onPromptClick(prompts[0]);

    expect(spy).toHaveBeenCalledWith('Prompt A');
  });
});
