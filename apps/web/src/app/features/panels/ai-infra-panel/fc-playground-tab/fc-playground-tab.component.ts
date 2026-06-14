import { Component, ChangeDetectionStrategy, signal, inject, ElementRef, viewChild, effect } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { FunctionCallService, FunctionCallEvent } from "@core/services/function-call.service";
import { NotificationService } from "@core/services/notification.service";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: number;
  toolCalls?: ToolCallCard[];
}

interface ToolCallCard {
  id: string;
  name: string;
  args: Record<string, unknown>;
  result?: string;
  isError?: boolean;
  expanded: boolean;
}

@Component({
  selector: "app-fc-playground-tab",
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="container">
      <div class="header">
        <h2>Function Call Playground</h2>
        <p class="subtitle">Chat with auto Function Calling - watch tool calls execute live</p>
      </div>

      <div class="chat-container" #chatContainer>
        @if (messages().length === 0) {
          <div class="empty-state">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
                <circle cx="8.5" cy="14.5" r="1.5"/>
                <circle cx="15.5" cy="14.5" r="1.5"/>
              </svg>
            </div>
            <p>Start a conversation to see function calls in action</p>
          </div>
        } @else {
          <div class="messages">
            @for (msg of messages(); track msg.id) {
              <div class="message" [class.user]="msg.role === 'user'" [class.assistant]="msg.role === 'assistant'">
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-role">{{ msg.role === 'user' ? 'You' : 'Assistant' }}</span>
                  </div>
                  <div class="message-body">
                    {{ msg.content || "Thinking..." }}
                  </div>
                  @if (msg.toolCalls && msg.toolCalls.length > 0) {
                    <div class="tool-calls">
                      @for (tc of msg.toolCalls; track tc.id) {
                        <div class="tool-call-card" [class.error]="tc.isError" [class.expanded]="tc.expanded">
                          <div class="tool-call-header" (click)="toggleToolCall(msg.id, tc.id)">
                            <div class="tool-call-info">
                              <span class="tool-icon">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                  <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
                                </svg>
                              </span>
                              <span class="tool-name">{{ tc.name }}</span>
                              @if (tc.result) {
                                <span class="tool-status" [class.error]="tc.isError">
                                  {{ tc.isError ? "Error" : "Done" }}
                                </span>
                              }
                            </div>
                            <span class="expand-icon">{{ tc.expanded ? "-" : "+" }}</span>
                          </div>
                          @if (tc.expanded) {
                            <div class="tool-call-body">
                              <div class="tool-section">
                                <h5>Arguments</h5>
                                <pre>{{ tc.args | json }}</pre>
                              </div>
                              @if (tc.result) {
                                <div class="tool-section">
                                  <h5>Result</h5>
                                  <pre>{{ tc.result }}</pre>
                                </div>
                              }
                            </div>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>

      <div class="input-area">
        <textarea
          class="input-textarea"
          [(ngModel)]="inputValue"
          placeholder="Type your message..."
          [disabled]="isLoading()"
          (keydown)="onKeyDown($event)"
          rows="1"
        ></textarea>
        <button
          class="send-button"
          [disabled]="isLoading() || !inputValue.trim()"
          (click)="sendMessage()"
        >
          @if (isLoading()) {
            <span class="spinner"></span>
          } @else {
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          }
        </button>
      </div>
    </div>
  `,
  styles: [`
    .container {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-lg);
      padding: var(--spacing-lg);
      height: 100%;
    }

    .header {
      flex-shrink: 0;
    }

    .header h2 {
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-semibold);
      margin: 0 0 var(--spacing-xs) 0;
    }

    .subtitle {
      color: var(--color-text-secondary);
      margin: 0;
    }

    .chat-container {
      flex: 1;
      overflow-y: auto;
      padding: var(--spacing-md);
      background: var(--color-surface);
      border-radius: var(--radius-lg);
      border: 1px solid var(--color-border);
      min-height: 300px;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: var(--color-text-secondary);
      text-align: center;
      gap: var(--spacing-md);
    }

    .empty-icon {
      opacity: 0.5;
    }

    .messages {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-md);
    }

    .message {
      display: flex;
      flex-direction: column;
    }

    .message.user {
      align-items: flex-end;
    }

    .message.assistant {
      align-items: flex-start;
    }

    .message-content {
      max-width: 85%;
      padding: var(--spacing-md);
      border-radius: var(--radius-lg);
    }

    .message.user .message-content {
      background: var(--color-primary);
      color: white;
    }

    .message.assistant .message-content {
      background: var(--color-background);
      border: 1px solid var(--color-border);
    }

    .message-header {
      margin-bottom: var(--spacing-xs);
    }

    .message-role {
      font-size: var(--font-size-xs);
      opacity: 0.7;
      font-weight: var(--font-weight-medium);
    }

    .message-body {
      font-size: var(--font-size-base);
      line-height: 1.5;
      white-space: pre-wrap;
    }

    .tool-calls {
      margin-top: var(--spacing-md);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-sm);
    }

    .tool-call-card {
      background: rgba(0, 0, 0, 0.05);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      overflow: hidden;
    }

    .message.user .tool-call-card {
      background: rgba(255, 255, 255, 0.1);
    }

    .tool-call-card.error {
      border-color: var(--color-error);
    }

    .tool-call-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--spacing-sm) var(--spacing-md);
      cursor: pointer;
      transition: background var(--transition-fast);
    }

    .tool-call-header:hover {
      background: rgba(0, 0, 0, 0.05);
    }

    .message.user .tool-call-header:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .tool-call-info {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
    }

    .tool-icon {
      color: var(--color-primary);
    }

    .message.user .tool-icon {
      color: rgba(255, 255, 255, 0.8);
    }

    .tool-name {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
    }

    .tool-status {
      font-size: var(--font-size-xs);
      padding: 2px 6px;
      border-radius: var(--radius-full);
      background: var(--color-success-light);
      color: var(--color-success);
    }

    .tool-status.error {
      background: var(--color-error-light);
      color: var(--color-error);
    }

    .expand-icon {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-secondary);
    }

    .tool-call-body {
      padding: var(--spacing-md);
      border-top: 1px solid var(--color-border);
      background: rgba(255, 255, 255, 0.5);
    }

    .message.user .tool-call-body {
      background: rgba(255, 255, 255, 0.1);
    }

    .tool-section {
      margin-bottom: var(--spacing-md);
    }

    .tool-section:last-child {
      margin-bottom: 0;
    }

    .tool-section h5 {
      margin: 0 0 var(--spacing-xs) 0;
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .tool-section pre {
      margin: 0;
      padding: var(--spacing-sm);
      background: var(--color-background);
      border-radius: var(--radius-sm);
      font-size: var(--font-size-xs);
      overflow-x: auto;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .input-area {
      flex-shrink: 0;
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
      transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
    }

    .input-textarea:focus {
      outline: none;
      border-color: var(--color-primary);
      box-shadow: var(--shadow-input);
    }

    .input-textarea::placeholder {
      color: var(--color-text-tertiary);
    }

    .input-textarea:disabled {
      opacity: 0.5;
      cursor: not-allowed;
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
    }

    .send-button:hover:not(:disabled) {
      background: var(--color-primary-hover);
    }

    .send-button:active:not(:disabled) {
      background: var(--color-primary-active);
      transform: scale(0.95);
    }

    .send-button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
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
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FcPlaygroundTabComponent {
  private fcService = inject(FunctionCallService);
  private notification = inject(NotificationService);

  messages = signal<ChatMessage[]>([]);
  isLoading = signal(false);
  inputValue = "";

  chatContainer = viewChild<ElementRef>("chatContainer");

  constructor() {
    effect(() => {
      const container = this.chatContainer();
      if (container) {
        container.nativeElement.scrollTop = container.nativeElement.scrollHeight;
      }
    });
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendMessage() {
    const input = this.inputValue.trim();
    if (!input || this.isLoading()) return;

    const userMessage: ChatMessage = {
      id: `user_${Date.now()}`,
      role: "user",
      content: input,
      timestamp: Date.now(),
    };

    this.messages.update((msgs) => [...msgs, userMessage]);
    this.inputValue = "";
    this.isLoading.set(true);

    const assistantMessageId = `assistant_${Date.now()}`;
    const assistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: "assistant",
      content: "",
      timestamp: Date.now(),
      toolCalls: [],
    };
    this.messages.update((msgs) => [...msgs, assistantMessage]);

    let currentToolCallId = "";

    this.fcService.chatStream(input).subscribe({
      next: (event) => {
        switch (event.type) {
          case "token":
            this.appendToAssistantMessage(assistantMessageId, event.delta);
            break;
          case "tool_call":
            currentToolCallId = `tool_${Date.now()}`;
            this.addToolCall(assistantMessageId, {
              id: currentToolCallId,
              name: event.name,
              args: event.args || {},
              expanded: false,
            });
            break;
          case "tool_result":
            this.updateToolCallResult(assistantMessageId, currentToolCallId, event.content, event.isError);
            break;
          case "done":
            this.isLoading.set(false);
            break;
          case "truncated":
            this.isLoading.set(false);
            this.notification.showWarning(`Stream truncated: ${event.reason}`);
            break;
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.appendToAssistantMessage(assistantMessageId, "An error occurred.");
        this.notification.showError("Failed to send message");
      },
    });
  }

  private appendToAssistantMessage(messageId: string, delta: string) {
    this.messages.update((msgs) =>
      msgs.map((msg) =>
        msg.id === messageId ? { ...msg, content: msg.content + delta } : msg
      )
    );
  }

  private addToolCall(messageId: string, toolCall: ToolCallCard) {
    this.messages.update((msgs) =>
      msgs.map((msg) => {
        if (msg.id !== messageId) return msg;
        return {
          ...msg,
          toolCalls: [...(msg.toolCalls || []), toolCall],
        };
      })
    );
  }

  private updateToolCallResult(messageId: string, toolCallId: string, content: string, isError?: boolean) {
    this.messages.update((msgs) =>
      msgs.map((msg) => {
        if (msg.id !== messageId) return msg;
        return {
          ...msg,
          toolCalls: (msg.toolCalls || []).map((tc) =>
            tc.id === toolCallId ? { ...tc, result: content, isError, expanded: true } : tc
          ),
        };
      })
    );
  }

  toggleToolCall(messageId: string, toolCallId: string) {
    this.messages.update((msgs) =>
      msgs.map((msg) => {
        if (msg.id !== messageId) return msg;
        return {
          ...msg,
          toolCalls: (msg.toolCalls || []).map((tc) =>
            tc.id === toolCallId ? { ...tc, expanded: !tc.expanded } : tc
          ),
        };
      })
    );
  }
}
