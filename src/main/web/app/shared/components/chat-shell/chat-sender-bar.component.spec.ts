import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ChatSenderBarComponent } from './chat-sender-bar.component';

describe('ChatSenderBarComponent', () => {
  let fixture: ComponentFixture<ChatSenderBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatSenderBarComponent],
      providers: [provideNzIconsPatch([ArrowUpOutline])],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatSenderBarComponent);
    fixture.componentRef.setInput('placeholder', 'Type a message');
    fixture.detectChanges();
  });

  it('should_renderSender_when_created', () => {
    expect(fixture.nativeElement.querySelector('nx-sender')).toBeTruthy();
  });

  it('should_emitSubmitSend_when_submitTriggered', () => {
    const spy = vi.fn();
    fixture.componentInstance.submitSend.subscribe(spy);

    fixture.componentInstance.submitSend.emit();

    expect(spy).toHaveBeenCalledOnce();
  });

  it('should_bindValue_when_modelUpdated', () => {
    fixture.componentInstance.value.set('hello');
    fixture.detectChanges();

    expect(fixture.componentInstance.value()).toBe('hello');
  });

  it('should_openSuggestion_when_slashTyped', () => {
    fixture.componentRef.setInput('actionGroups', [
      {
        id: 'tools',
        label: 'Tools',
        items: [
          {
            id: 'tool:getWeather',
            kind: 'tool',
            label: 'getWeather',
          },
        ],
      },
    ]);
    fixture.detectChanges();

    fixture.componentInstance.onValueChange('hello/');
    fixture.detectChanges();

    expect(fixture.componentInstance.suggestionOpen()).toBe(true);
    expect(fixture.componentInstance.value()).toBe('hello');
    expect(fixture.nativeElement.querySelector('app-sender-suggestion')).toBeTruthy();
  });

  it('should_setSelectedToolChip_when_toolChosen_withoutChangingInput', () => {
    const spy = vi.fn();
    fixture.componentInstance.actionSelect.subscribe(spy);
    fixture.componentInstance.value.set('my question');
    const item = {
      id: 'tool:getWeather',
      kind: 'tool' as const,
      label: 'getWeather',
    };
    fixture.componentRef.setInput('actionGroups', [
      { id: 'tools', label: 'Tools', items: [item] },
    ]);
    fixture.detectChanges();

    fixture.componentInstance.onActionSelect(item);
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledWith(item);
    expect(fixture.componentInstance.selectedTool()?.label).toBe('getWeather');
    expect(fixture.componentInstance.value()).toBe('my question');
    const chip = fixture.nativeElement.querySelector('[data-selected-tool] span');
    expect(chip?.className).toContain('bg-foreground');
    expect(chip?.className).toContain('text-white');
    expect(fixture.componentInstance.suggestionOpen()).toBe(false);
  });

  it('should_setSelectedChip_when_agentChosen_withoutChangingInput', () => {
    const item = {
      id: 'agent:weather',
      kind: 'agent' as const,
      label: 'Weather Agent',
      agentType: 'weather',
    };
    fixture.componentInstance.value.set('ask about rain');
    fixture.componentInstance.onActionSelect(item);
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedTool()?.label).toBe('Weather Agent');
    expect(fixture.componentInstance.value()).toBe('ask about rain');
  });

  it('should_clearSelectedTool_when_removeClicked', () => {
    fixture.componentInstance.selectedTool.set({
      id: 'tool:getWeather',
      kind: 'tool',
      label: 'getWeather',
    });
    fixture.detectChanges();

    const remove = fixture.nativeElement.querySelector(
      '[data-selected-tool] button',
    ) as HTMLButtonElement;
    remove.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedTool()).toBeNull();
  });
});
