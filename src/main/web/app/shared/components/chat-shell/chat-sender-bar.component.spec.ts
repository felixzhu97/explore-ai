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

  it('should render sender when created', () => {
    expect(fixture.nativeElement.querySelector('nx-sender')).toBeTruthy();
  });

  it('should emit submit send when submit triggered', () => {
    const spy = vi.fn();
    fixture.componentInstance.submitSend.subscribe(spy);

    fixture.componentInstance.submitSend.emit();

    expect(spy).toHaveBeenCalledOnce();
  });

  it('should bind value when model updated', () => {
    fixture.componentInstance.value.set('hello');
    fixture.detectChanges();

    expect(fixture.componentInstance.value()).toBe('hello');
  });
});
