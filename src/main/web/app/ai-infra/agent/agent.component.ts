import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  computed,
  effect,
  ElementRef,
  viewChild,
  inject,
  model,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatMessageComponent } from './chat-message/chat-message.component';
import type { ChatMessageData } from './chat-message/chat-message.component';
import { ToolCall } from './chat-message/tool-result/tool-result.component';
import type { AgentInfo } from './agent.model';
import { NotificationService } from '@core/services/notification.service';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
export type { AgentInfo };

@Component({
  selector: 'app-agent-component',
  imports: [ChatMessageComponent, FormsModule, NxSenderComponent, NzIconModule],
  standalone: true,
  templateUrl: './agent.component.html',
  styles: [`
    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes spin {
      from {
        transform: rotate(0deg);
      }
      to {
        transform: rotate(360deg);
      }
    }
  `],
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentComponent {
  private notifications = inject(NotificationService);

  readonly agentInfo = input.required<AgentInfo>();
  readonly apiEndpoint = input.required<string>();
  readonly quickPrompts = input<string[]>([]);

  readonly messages = signal<ChatMessageData[]>([]);
  readonly isLoading = signal(false);
  readonly inputValue = model<string>('');

  readonly chatContainer = viewChild<ElementRef>('chatContainer');
  readonly messagesEnd = viewChild<ElementRef>('messagesEnd');

  private abortController: AbortController | null = null;

  constructor() {
    // Scroll to bottom when messages change
    effect(() => {
      this.messages(); // Trigger on messages change
      const endEl = this.messagesEnd();
      if (endEl) {
        endEl.nativeElement.scrollIntoView({ behavior: 'smooth' });
      }
    });
  }

  // i18n placeholder texts
  readonly startConversationText = computed(() => 'Start a conversation');
  readonly thinkingText = computed(() => 'Thinking...');
  readonly inputPlaceholderText = computed(() => 'Type your message...');

  setInput(value: string) {
    this.inputValue.set(value);
  }

  async sendMessage() {
    const input = this.inputValue().trim();
    if (!input || this.isLoading()) return;

    // Cancel any existing request
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();

    const userMessage: ChatMessageData = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: input,
      timestamp: Date.now(),
    };

    this.messages.update(msgs => [...msgs, userMessage]);
    this.inputValue.set('');
    this.isLoading.set(true);

    const assistantMessageId = `assistant_${Date.now()}`;

    this.messages.update(msgs => [
      ...msgs,
      {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        toolCalls: [],
      },
    ]);

    try {
      const response = await fetch(this.apiEndpoint(), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: [{ role: 'user', content: userMessage.content }],
        }),
        signal: this.abortController.signal,
      });

      if (!response.ok) {
        throw new Error('Request failed');
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error('No response body');

      const decoder = new TextDecoder();
      let fullContent = '';
      let currentEvent = '';
      let currentToolCallId = '';
      let accumulatedToolOutput = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.slice(7).trim();
          } else if (line.startsWith('data: ')) {
            const data = line.slice(6);

            if (currentEvent === 'tool_call') {
              accumulatedToolOutput = '';
              try {
                const toolData = JSON.parse(data);
                currentToolCallId = toolData.id;
                this.updateToolCall(
                  assistantMessageId,
                  toolData.id,
                  toolData.name,
                  toolData.input || {},
                  'running',
                );
              } catch {
                // Ignore parse errors
              }
            } else if (currentEvent === 'tool_output') {
              try {
                const outputData = JSON.parse(data);
                const outputStr =
                  typeof outputData === 'string' ? outputData : JSON.stringify(outputData);
                accumulatedToolOutput += (accumulatedToolOutput ? '\n' : '') + outputStr;
                fullContent += (fullContent ? '\n\n' : '') + outputStr;
                this.updateToolCall(
                  assistantMessageId,
                  currentToolCallId,
                  '',
                  {},
                  'success',
                  accumulatedToolOutput,
                );
                this.updateMessageContent(assistantMessageId, fullContent);
              } catch {
                accumulatedToolOutput += (accumulatedToolOutput ? '\n' : '') + data;
                fullContent += (fullContent ? '\n\n' : '') + data;
                this.updateToolCall(
                  assistantMessageId,
                  currentToolCallId,
                  '',
                  {},
                  'success',
                  accumulatedToolOutput,
                );
                this.updateMessageContent(assistantMessageId, fullContent);
              }
            } else if (currentEvent === 'tool_error') {
              this.updateToolCall(assistantMessageId, currentToolCallId, '', {}, 'error', data);
            } else if (data === '[DONE]') {
              break;
            } else if (!currentEvent || currentEvent === 'message') {
              fullContent += data;
              this.updateMessageContent(assistantMessageId, fullContent);
            }
          } else if (line.trim() === '') {
            currentEvent = '';
          }
        }
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      this.notifications.showError('An error occurred while sending your message.');
      this.updateMessageContent(assistantMessageId, 'An error occurred.');
    } finally {
      this.isLoading.set(false);
      this.abortController = null;
    }
  }

  private updateMessageContent(messageId: string, content: string) {
    this.messages.update((msgs) => {
      return msgs.map((msg) => {
        return msg.id === messageId ? { ...msg, content } : msg;
      });
    });
  }

  private updateToolCall(
    messageId: string,
    toolCallId: string,
    name: string,
    input: Record<string, unknown>,
    status: ToolCall['status'],
    output?: string,
  ) {
    this.messages.update(msgs => msgs.map((msg) => {
      if (msg.id !== messageId) return msg;
      const existingToolCalls = msg.toolCalls || [];
      const existingIndex = existingToolCalls.findIndex(tc => tc.id === toolCallId);

      if (existingIndex >= 0) {
        const updated = [...existingToolCalls];
        updated[existingIndex] = {
          ...updated[existingIndex],
          status,
          output,
        };
        return { ...msg, toolCalls: updated };
      } else {
        return {
          ...msg,
          toolCalls: [
            ...existingToolCalls,
            { id: toolCallId, name, input, status, output },
          ],
        };
      }
    }),
    );
  }
}
