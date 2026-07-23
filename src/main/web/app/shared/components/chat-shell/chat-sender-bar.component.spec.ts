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

  it('should_stackSelectedChips_when_multipleToolsChosen', () => {
    const weather = {
      id: 'tool:getWeather',
      kind: 'tool' as const,
      label: 'getWeather',
    };
    const search = {
      id: 'tool:searchWeb',
      kind: 'tool' as const,
      label: 'searchWeb',
    };
    fixture.componentInstance.value.set('my question');

    fixture.componentInstance.onActionSelect(weather);
    fixture.componentInstance.onActionSelect(search);
    fixture.componentInstance.onActionSelect(weather);
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedActions().map(item => item.id)).toEqual([
      'tool:getWeather',
      'tool:searchWeb',
    ]);
    expect(fixture.componentInstance.value()).toBe('my question');
    expect(fixture.nativeElement.querySelectorAll('[data-selected-action]')).toHaveLength(2);
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

    expect(fixture.componentInstance.selectedActions()[0]?.label).toBe('Weather Agent');
    expect(fixture.componentInstance.value()).toBe('ask about rain');
  });

  it('should_removeOneChip_when_removeClicked', () => {
    fixture.componentInstance.selectedActions.set([
      { id: 'tool:getWeather', kind: 'tool', label: 'getWeather' },
      { id: 'tool:searchWeb', kind: 'tool', label: 'searchWeb' },
    ]);
    fixture.detectChanges();

    const remove = fixture.nativeElement.querySelector(
      '[data-selected-action] button',
    ) as HTMLButtonElement;
    remove.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedActions().map(item => item.id)).toEqual([
      'tool:searchWeb',
    ]);
  });
});
