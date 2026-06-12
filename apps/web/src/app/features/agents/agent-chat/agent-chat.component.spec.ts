import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { AgentChatComponent, AgentInfo } from './agent-chat.component';
import { ChatMessageComponent } from '../chat-message/chat-message.component';

describe('AgentChatComponent', () => {
  let fixture: ComponentFixture<AgentChatComponent>;
  let component: AgentChatComponent;

  const mockAgentInfo: AgentInfo = {
    name: 'Test Agent',
    description: 'A test agent for unit testing',
    status: 'online',
  };

  const mockApiEndpoint = '/api/test/chat';
  const mockQuickPrompts = ['Hello', 'Help me', 'Tell me a story'];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentChatComponent, ChatMessageComponent, HttpClientTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(AgentChatComponent);
    component = fixture.componentInstance;

    (component as any).agentInfo = signal(mockAgentInfo);
    (component as any).apiEndpoint = signal(mockApiEndpoint);
    (component as any).quickPrompts = signal(mockQuickPrompts);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('component creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with empty messages', () => {
      expect(component.messages()).toEqual([]);
    });

    it('should initialize with isLoading as false', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should initialize with empty inputValue', () => {
      expect(component.inputValue).toBe('');
    });

    it('should have null abort controller initially', () => {
      expect(component['abortController']).toBeNull();
    });
  });

  describe('computed signals', () => {
    it('should return start conversation text', () => {
      expect(component.startConversationText()).toBe('Start a conversation');
    });

    it('should return thinking text', () => {
      expect(component.thinkingText()).toBe('Thinking...');
    });

    it('should return input placeholder text', () => {
      expect(component.inputPlaceholderText()).toBe('Type your message...');
    });
  });

  describe('setInput', () => {
    it('should set input value', () => {
      component.setInput('Hello world');
      expect(component.inputValue).toBe('Hello world');
    });

    it('should handle empty string', () => {
      component.setInput('');
      expect(component.inputValue).toBe('');
    });

    it('should overwrite previous value', () => {
      component.setInput('First');
      component.setInput('Second');
      expect(component.inputValue).toBe('Second');
    });
  });

  describe('onKeyDown', () => {
    it('should call sendMessage on Enter key without shift', () => {
      vi.spyOn(component, 'sendMessage');
      const event = new KeyboardEvent('keydown', {
        key: 'Enter',
        shiftKey: false,
      });
      component.onKeyDown(event);
      expect(component.sendMessage).toHaveBeenCalled();
    });

    it('should not call sendMessage on Enter with shift', () => {
      vi.spyOn(component, 'sendMessage');
      const event = new KeyboardEvent('keydown', {
        key: 'Enter',
        shiftKey: true,
      });
      component.onKeyDown(event);
      expect(component.sendMessage).not.toHaveBeenCalled();
    });

    it('should not call sendMessage on other keys', () => {
      vi.spyOn(component, 'sendMessage');
      const event = new KeyboardEvent('keydown', { key: 'Space' });
      component.onKeyDown(event);
      expect(component.sendMessage).not.toHaveBeenCalled();
    });

    it('should prevent default on Enter without shift', () => {
      const event = new KeyboardEvent('keydown', {
        key: 'Enter',
        shiftKey: false,
      });
      const preventDefaultSpy = vi.spyOn(event, 'preventDefault');
      component.onKeyDown(event);
      expect(preventDefaultSpy).toHaveBeenCalled();
    });
  });

  describe('sendMessage', () => {
    it('should not send if input is empty', async () => {
      component.inputValue = '';
      const fetchSpy = vi.spyOn(global, 'fetch');
      await component.sendMessage();
      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('should not send if input is whitespace only', async () => {
      component.inputValue = '   ';
      const fetchSpy = vi.spyOn(global, 'fetch');
      await component.sendMessage();
      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('should not send if already loading', async () => {
      component.inputValue = 'Hello';
      component.isLoading.set(true);
      const fetchSpy = vi.spyOn(global, 'fetch');
      await component.sendMessage();
      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('should clear input after sending', async () => {
      component.inputValue = 'Hello';

      const mockReader = {
        read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as any);

      await component.sendMessage();
      expect(component.inputValue).toBe('');
    });

    it('should add user message to messages array', async () => {
      component.inputValue = 'Test message';

      const mockReader = {
        read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as any);

      await component.sendMessage();

      const messages = component.messages();
      expect(messages.length).toBeGreaterThan(0);
      // First message should be user
      const userMessage = messages.find((m) => m.role === 'user');
      expect(userMessage).toBeDefined();
      expect(userMessage?.content).toBe('Test message');
    });

    it('should add assistant placeholder message', async () => {
      component.inputValue = 'Test';

      const mockReader = {
        read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as any);

      await component.sendMessage();

      const messages = component.messages();
      const assistantMessages = messages.filter((m) => m.role === 'assistant');
      expect(assistantMessages.length).toBeGreaterThan(0);
    });

    it('should handle abort error gracefully', async () => {
      component.inputValue = 'Test';

      const abortError = new DOMException('Aborted', 'AbortError');
      vi.spyOn(global, 'fetch').mockRejectedValue(abortError);

      await component.sendMessage();

      expect(component.isLoading()).toBe(false);
    });

    it('should reset abortController after completion', async () => {
      component.inputValue = 'Test';

      const mockReader = {
        read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        body: { getReader: () => mockReader },
      } as any);

      await component.sendMessage();
      expect(component['abortController']).toBeNull();
    });
  });

  describe('updateMessageContent', () => {
    it('should update message content by id', () => {
      component.messages.set([
        { id: 'msg1', role: 'user', content: 'Hello', timestamp: Date.now() },
        { id: 'msg2', role: 'assistant', content: '', timestamp: Date.now() },
      ]);

      component['updateMessageContent']('msg2', 'Updated content');

      const messages = component.messages();
      const updatedMsg = messages.find((m) => m.id === 'msg2');
      expect(updatedMsg?.content).toBe('Updated content');
    });

    it('should not update other messages', () => {
      component.messages.set([
        { id: 'msg1', role: 'user', content: 'Hello', timestamp: Date.now() },
        { id: 'msg2', role: 'assistant', content: '', timestamp: Date.now() },
      ]);

      component['updateMessageContent']('msg2', 'Updated');

      const msg1 = component.messages().find((m) => m.id === 'msg1');
      expect(msg1?.content).toBe('Hello');
    });
  });

  describe('updateToolCall', () => {
    it('should add new tool call to message', () => {
      component.messages.set([
        {
          id: 'msg1',
          role: 'assistant',
          content: 'Test',
          timestamp: Date.now(),
          toolCalls: [],
        },
      ]);

      component['updateToolCall']('msg1', 'tool1', 'testTool', { arg: 'value' }, 'running');

      const msg = component.messages().find((m) => m.id === 'msg1');
      expect(msg?.toolCalls).toHaveLength(1);
      expect(msg?.toolCalls?.[0].id).toBe('tool1');
      expect(msg?.toolCalls?.[0].name).toBe('testTool');
      expect(msg?.toolCalls?.[0].status).toBe('running');
    });

    it('should update existing tool call', () => {
      component.messages.set([
        {
          id: 'msg1',
          role: 'assistant',
          content: 'Test',
          timestamp: Date.now(),
          toolCalls: [
            {
              id: 'tool1',
              name: 'testTool',
              input: {},
              status: 'running' as const,
            },
          ],
        },
      ]);

      component['updateToolCall']('msg1', 'tool1', 'testTool', {}, 'success', 'Tool output');

      const msg = component.messages().find((m) => m.id === 'msg1');
      expect(msg?.toolCalls).toHaveLength(1);
      expect(msg?.toolCalls?.[0].status).toBe('success');
      expect(msg?.toolCalls?.[0].output).toBe('Tool output');
    });

    it('should handle message without toolCalls array', () => {
      component.messages.set([
        {
          id: 'msg1',
          role: 'assistant',
          content: 'Test',
          timestamp: Date.now(),
        },
      ]);

      component['updateToolCall']('msg1', 'tool1', 'testTool', {}, 'running');

      const msg = component.messages().find((m) => m.id === 'msg1');
      expect(msg?.toolCalls).toHaveLength(1);
    });
  });

  describe('AgentInfo interface', () => {
    it('should accept valid AgentInfo', () => {
      const agentInfo: AgentInfo = {
        name: 'test-agent',
        description: 'A test agent',
        status: 'online',
      };
      expect(agentInfo.name).toBe('test-agent');
    });

    it('should allow offline status', () => {
      const agentInfo: AgentInfo = {
        name: 'test-agent',
        description: 'Test',
        status: 'offline',
      };
      expect(agentInfo.status).toBe('offline');
    });

    it('should allow busy status', () => {
      const agentInfo: AgentInfo = {
        name: 'test-agent',
        description: 'Test',
        status: 'busy',
      };
      expect(agentInfo.status).toBe('busy');
    });

    it('should allow optional status', () => {
      const agentInfo: AgentInfo = {
        name: 'test-agent',
        description: 'Test',
      };
      expect((agentInfo as any).status).toBeUndefined();
    });
  });
});
