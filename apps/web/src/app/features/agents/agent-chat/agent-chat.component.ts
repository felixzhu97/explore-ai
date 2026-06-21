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
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ChatMessageComponent } from '../chat-message/chat-message.component';
import type { ChatMessageData } from '../chat-message/chat-message.component';
import { ToolCall } from '../tool-result/tool-result.component';
import type { AgentInfo } from '../models/agent.model';
export type { AgentInfo };

@Component({
  selector: 'app-agent-chat',
  standalone: true,
  imports: [ChatMessageComponent, FormsModule],
  template: `
    <div class="container">
      <div class="chat-container" #chatContainer>
        @if (messages().length === 0) {
          <div class="empty-state">
            <div class="empty-icon">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <path
                  d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"
                />
                <circle cx="8.5" cy="14.5" r="1.5" />
                <circle cx="15.5" cy="14.5" r="1.5" />
              </svg>
            </div>
            <p>{{ startConversationText() }}</p>
            @if (quickPrompts().length > 0) {
              <div class="quick-prompts">
                @for (prompt of quickPrompts().slice(0, 3); track $index) {
                  <button class="quick-prompt-btn" (click)="setInput(prompt)">
                    {{ prompt }}
                  </button>
                }
              </div>
            }
          </div>
        } @else {
          <div class="messages">
            @for (msg of messages(); track msg.id) {
              <app-chat-message [message]="msg" />
            }
            @if (isLoading()) {
              <div class="loading-indicator">
                <span class="spinner"></span>
                <span>{{ thinkingText() }}</span>
              </div>
            }
            <div #messagesEnd></div>
          </div>
        }
      </div>

      <div class="input-area">
        <textarea
          class="input-textarea"
          [ngModel]="inputValue()"
          (ngModelChange)="inputValue.set($event)"
          [placeholder]="inputPlaceholderText()"
          [disabled]="isLoading()"
          (keydown)="onKeyDown($event)"
          rows="1"
        ></textarea>
        <button
          class="send-button"
          [disabled]="isLoading() || !inputValue().trim()"
          (click)="sendMessage()"
        >
          @if (isLoading()) {
            <span class="spinner"></span>
          } @else {
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          }
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .container {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-lg);
        animation: fadeIn 0.3s ease;
      }

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

      .chat-container {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
        max-height: 400px;
        min-height: 200px;
        overflow-y: auto;
        padding: var(--spacing-md);
        background: var(--color-surface);
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border);
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: var(--spacing-xxl);
        color: var(--color-text-secondary);
        text-align: center;
        gap: var(--spacing-sm);
      }

      .empty-icon {
        font-size: 48px;
        opacity: 0.5;
      }

      .quick-prompts {
        display: flex;
        gap: var(--spacing-sm);
        flex-wrap: wrap;
        justify-content: center;
        margin-top: var(--spacing-sm);
      }

      .quick-prompt-btn {
        padding: 6px 12px;
        font-size: var(--font-size-sm);
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-full);
        color: var(--color-primary);
        cursor: pointer;
        transition: all var(--transition-fast);

        &:hover {
          background: var(--color-primary-light);
        }
      }

      .messages {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .loading-indicator {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);
        color: var(--color-text-secondary);
        font-size: var(--font-size-sm);
        padding: var(--spacing-sm);
      }

      .spinner {
        display: inline-block;
        width: 18px;
        height: 18px;
        border: 2px solid currentColor;
        border-right-color: transparent;
        border-radius: 50%;
        animation: spin 0.6s linear infinite;
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .input-area {
        display: flex;
        gap: var(--spacing-sm);
        align-items: flex-end;
      }

      .input-textarea {
        flex: 1;
        padding: var(--spacing-md);
        font-size: var(--font-size-base);
        font-family: var(--font-family-body);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-lg);
        background: var(--color-surface);
        color: var(--color-text);
        resize: none;
        min-height: 48px;
        max-height: 120px;
        transition:
          border-color var(--transition-fast),
          box-shadow var(--transition-fast);

        &:focus {
          outline: none;
          border-color: var(--color-primary);
          box-shadow: var(--shadow-input);
        }

        &::placeholder {
          color: var(--color-text-tertiary);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }

      .send-button {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-primary);
        color: white;
        border: none;
        border-radius: var(--radius-lg);
        cursor: pointer;
        transition: all var(--transition-default);
        font-size: 18px;

        &:hover:not(:disabled) {
          background: var(--color-primary-hover);
        }

        &:active:not(:disabled) {
          background: var(--color-primary-active);
          transform: scale(0.95);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentChatComponent {
  private http = inject(HttpClient);

  agentInfo = input.required<AgentInfo>();
  apiEndpoint = input.required<string>();
  quickPrompts = input<string[]>([]);

  messages = signal<ChatMessageData[]>([]);
  isLoading = signal(false);
  inputValue = model<string>('');

  chatContainer = viewChild<ElementRef>('chatContainer');
  messagesEnd = viewChild<ElementRef>('messagesEnd');

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
  startConversationText = computed(() => 'Start a conversation');
  thinkingText = computed(() => 'Thinking...');
  inputPlaceholderText = computed(() => 'Type your message...');

  setInput(value: string) {
    this.inputValue.set(value);
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
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

    this.messages.update((msgs) => [...msgs, userMessage]);
    this.inputValue.set('');
    this.isLoading.set(true);

    const assistantMessageId = `assistant_${Date.now()}`;

    this.messages.update((msgs) => [
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
                  'running'
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
                  accumulatedToolOutput
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
                  accumulatedToolOutput
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
      console.error('Error sending message:', error);
      this.updateMessageContent(assistantMessageId, 'An error occurred.');
    } finally {
      this.isLoading.set(false);
      this.abortController = null;
    }
  }

  private updateMessageContent(messageId: string, content: string) {
    this.messages.update((msgs) =>
      msgs.map((msg) => (msg.id === messageId ? { ...msg, content } : msg))
    );
  }

  private updateToolCall(
    messageId: string,
    toolCallId: string,
    name: string,
    input: Record<string, unknown>,
    status: ToolCall['status'],
    output?: string
  ) {
    this.messages.update((msgs) =>
      msgs.map((msg) => {
        if (msg.id !== messageId) return msg;
        const existingToolCalls = msg.toolCalls || [];
        const existingIndex = existingToolCalls.findIndex((tc) => tc.id === toolCallId);

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
            toolCalls: [...existingToolCalls, { id: toolCallId, name, input, status, output }],
          };
        }
      })
    );
  }
}
