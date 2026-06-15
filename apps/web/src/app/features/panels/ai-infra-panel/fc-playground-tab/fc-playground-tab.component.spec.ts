import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FcPlaygroundTabComponent } from './fc-playground-tab.component';
import { FunctionCallService, FunctionCallEvent } from '@core/services/function-call.service';
import { NotificationService } from '@core/services/notification.service';

describe('FcPlaygroundTabComponent', () => {
  let component: FcPlaygroundTabComponent;
  let fixture: ComponentFixture<FcPlaygroundTabComponent>;
  let mockFcService: any;
  let mockNotification: any;

  beforeEach(async () => {
    mockFcService = {
      chatStream: vi.fn(),
    };

    mockNotification = {
      showError: vi.fn(),
      showSuccess: vi.fn(),
      showWarning: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [FcPlaygroundTabComponent],
      providers: [
        { provide: FunctionCallService, useValue: mockFcService },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FcPlaygroundTabComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with empty messages', () => {
    expect(component.messages().length).toBe(0);
  });

  it('should not send message when input is empty', () => {
    component.inputValue = '';
    component.sendMessage();
    expect(mockFcService.chatStream).not.toHaveBeenCalled();
  });

  it('should add user message to messages array', () => {
    component.inputValue = 'Hello';
    mockFcService.chatStream.mockReturnValue({
      subscribe: vi.fn(),
    });

    component.sendMessage();

    expect(component.messages().length).toBe(2); // user + assistant
    expect(component.messages()[0].role).toBe('user');
    expect(component.messages()[0].content).toBe('Hello');
  });

  it('should clear input after sending', () => {
    component.inputValue = 'Hello';
    mockFcService.chatStream.mockReturnValue({
      subscribe: vi.fn(),
    });

    component.sendMessage();

    expect(component.inputValue).toBe('');
  });

  it('should toggle tool call expansion', () => {
    const messageId = 'msg1';
    const toolCallId = 'tool1';

    component.messages.set([
      {
        id: messageId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        toolCalls: [{ id: toolCallId, name: 'test', args: {}, expanded: false }],
      },
    ]);

    component.toggleToolCall(messageId, toolCallId);

    expect(component.messages()[0].toolCalls![0].expanded).toBe(true);
  });
});
