import { useState, useRef, useEffect, useCallback } from 'react';
import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { colors, radius, shadows, spacing, typography, transitions } from '../../theme';
import { ChatMessage, ChatMessageData, ToolCall } from './ChatMessage';
import { useI18n } from '../../i18n';

const API_BASE = import.meta.env.VITE_AI_AGENTS_URL || 'http://localhost:8003';

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

const spin = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.lg};
  animation: ${fadeIn} 0.3s ease;
`;

const ChatContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
  max-height: 400px;
  min-height: 200px;
  overflow-y: auto;
  padding: ${spacing.md};
  background: ${colors.surface};
  border-radius: ${radius.lg};
  border: 1px solid ${colors.border};
`;

const EmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: ${spacing.xxl};
  color: ${colors.textSecondary};
  text-align: center;
  gap: ${spacing.sm};
`;

const EmptyIcon = styled.div`
  font-size: 48px;
  opacity: 0.5;
`;

const InputArea = styled.div`
  display: flex;
  gap: ${spacing.sm};
  align-items: flex-end;
`;

const TextArea = styled.textarea`
  flex: 1;
  padding: ${spacing.md};
  font-size: ${typography.fontSize.base};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.lg};
  background: ${colors.surface};
  color: ${colors.text};
  resize: none;
  min-height: 48px;
  max-height: 120px;
  transition: border-color ${transitions.fast}, box-shadow ${transitions.fast};

  &:focus {
    outline: none;
    border-color: ${colors.primary};
    box-shadow: ${shadows.input};
  }

  &::placeholder {
    color: ${colors.textTertiary};
  }
`;

const SendButton = styled.button<{ disabled?: boolean }>`
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${colors.primary};
  color: white;
  border: none;
  border-radius: ${radius.lg};
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  opacity: ${({ disabled }) => (disabled ? 0.5 : 1)};
  transition: all ${transitions.default};
  font-size: 18px;

  &:hover:not(:disabled) {
    background: ${colors.primaryHover};
  }

  &:active:not(:disabled) {
    background: ${colors.primaryActive};
    transform: scale(0.95);
  }
`;

const Spinner = styled.span`
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: ${spin} 0.6s linear infinite;
`;

const LoadingIndicator = styled.div`
  display: flex;
  align-items: center;
  gap: ${spacing.sm};
  color: ${colors.textSecondary};
  font-size: ${typography.fontSize.sm};
  padding: ${spacing.sm};
`;

export interface AgentInfo {
  name: string;
  description: string;
  status?: 'online' | 'offline' | 'busy';
}

interface AgentChatProps {
  agentInfo: AgentInfo;
  apiEndpoint: string;
  quickPrompts?: string[];
}

export function AgentChat({ agentInfo, apiEndpoint, quickPrompts = [] }: AgentChatProps) {
  const { t } = useI18n();
  const [messages, setMessages] = useState<ChatMessageData[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Cleanup SSE connection on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, []);

  const handleSend = useCallback(async () => {
    if (!input.trim() || isLoading) return;

    // Cancel any existing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    const userMessage: ChatMessageData = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: input.trim(),
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    const assistantMessageId = `assistant_${Date.now()}`;
    const pendingToolCalls: ToolCall[] = [];
    
    setMessages((prev) => [
      ...prev,
      {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        toolCalls: pendingToolCalls,
      },
    ]);

    try {
      const response = await fetch(`${API_BASE}${apiEndpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: [{ role: 'user', content: userMessage.content }],
        }),
        signal: abortControllerRef.current.signal,
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

      const createOrUpdateToolCall = (
        id: string,
        name: string,
        input: Record<string, unknown>,
        status: ToolCall['status'],
        output?: string
      ) => {
        setMessages((prev) =>
          prev.map((msg) => {
            if (msg.id !== assistantMessageId) return msg;
            const existingToolCalls = msg.toolCalls || [];
            const existingIndex = existingToolCalls.findIndex((tc) => tc.id === id);
            
            if (existingIndex >= 0) {
              const updated = [...existingToolCalls];
              updated[existingIndex] = { ...updated[existingIndex], status, output };
              return { ...msg, toolCalls: updated };
            } else {
              return {
                ...msg,
                toolCalls: [
                  ...existingToolCalls,
                  { id, name, input, status, output },
                ],
              };
            }
          })
        );
      };

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
              // Reset accumulated output for new tool call
              accumulatedToolOutput = '';
              try {
                const toolData = JSON.parse(data);
                currentToolCallId = toolData.id;
                createOrUpdateToolCall(
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
                const outputStr = typeof outputData === 'string' ? outputData : JSON.stringify(outputData);
                // Accumulate multi-line outputs
                accumulatedToolOutput += (accumulatedToolOutput ? '\n' : '') + outputStr;
                // Also append to main content for visibility
                fullContent += (fullContent ? '\n\n' : '') + outputStr;
                createOrUpdateToolCall(
                  currentToolCallId,
                  '',
                  {},
                  'success',
                  accumulatedToolOutput
                );
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMessageId
                      ? { ...msg, content: fullContent }
                      : msg
                  )
                );
              } catch {
                accumulatedToolOutput += (accumulatedToolOutput ? '\n' : '') + data;
                fullContent += (fullContent ? '\n\n' : '') + data;
                createOrUpdateToolCall(currentToolCallId, '', {}, 'success', accumulatedToolOutput);
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMessageId
                      ? { ...msg, content: fullContent }
                      : msg
                  )
                );
              }
            } else if (currentEvent === 'tool_error') {
              createOrUpdateToolCall(currentToolCallId, '', {}, 'error', data);
            } else if (data === '[DONE]') {
              break;
            } else if (!currentEvent || currentEvent === 'message') {
              fullContent += data;
              setMessages((prev) =>
                prev.map((msg) =>
                  msg.id === assistantMessageId
                    ? { ...msg, content: fullContent }
                    : msg
                )
              );
            }
          } else if (line.trim() === '') {
            currentEvent = '';
          }
        }
      }
    } catch (error) {
      // Ignore abort errors (user cancelled or component unmounted)
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      console.error('Error sending message:', error);
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessageId
            ? { ...msg, content: t.agents?.errorMessage || 'An error occurred.' }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
      if (abortControllerRef.current) {
        abortControllerRef.current = null;
      }
    }
  }, [input, isLoading, apiEndpoint, t]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Container>
      <ChatContainer>
        {messages.length === 0 ? (
          <EmptyState>
            <EmptyIcon>
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
              <circle cx="8.5" cy="14.5" r="1.5"/>
              <circle cx="15.5" cy="14.5" r="1.5"/>
            </svg>
          </EmptyIcon>
            <p>{t.agents?.startConversation || 'Start a conversation'}</p>
            {quickPrompts.length > 0 && (
              <div style={{ display: 'flex', gap: spacing.sm, flexWrap: 'wrap', justifyContent: 'center', marginTop: spacing.sm }}>
                {quickPrompts.slice(0, 3).map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => setInput(prompt)}
                    style={{
                      padding: '6px 12px',
                      fontSize: typography.fontSize.sm,
                      background: colors.surface,
                      border: `1px solid ${colors.border}`,
                      borderRadius: radius.full,
                      color: colors.primary,
                      cursor: 'pointer',
                    }}
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            )}
          </EmptyState>
        ) : (
          <>
            {messages.map((msg) => (
              <ChatMessage key={msg.id} message={msg} />
            ))}
            {isLoading && (
              <LoadingIndicator>
                <Spinner />
                {t.agents?.thinking || 'Thinking...'}
              </LoadingIndicator>
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </ChatContainer>

      <InputArea>
        <TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t.agents?.inputPlaceholder || 'Type your message...'}
          rows={1}
          disabled={isLoading}
        />
        <SendButton onClick={handleSend} disabled={isLoading || !input.trim()}>
          {isLoading ? <Spinner /> : '→'}
        </SendButton>
      </InputArea>
    </Container>
  );
}
