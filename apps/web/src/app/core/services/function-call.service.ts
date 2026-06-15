import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export type FunctionCallEvent =
  | { type: 'token'; delta: string }
  | { type: 'tool_call'; name: string; args: Record<string, unknown>; composite?: boolean }
  | { type: 'tool_result'; name: string; content: string; isError: boolean }
  | { type: 'done' }
  | { type: 'truncated'; reason: string };

@Injectable({ providedIn: 'root' })
export class FunctionCallService {
  chatStream(message: string, sessionId?: string): Observable<FunctionCallEvent> {
    const subject = new Subject<FunctionCallEvent>();
    const controller = new AbortController();

    fetch('/api/mcp/function-call/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, sessionId }),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          subject.next({ type: 'truncated', reason: response.statusText });
          subject.complete();
          return;
        }

        if (!response.body) {
          subject.next({ type: 'truncated', reason: 'No response body' });
          subject.complete();
          return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let currentEvent = '';

        const processLine = () => {
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) {
              currentEvent = '';
              continue;
            }
            if (trimmed.startsWith('event:')) {
              currentEvent = trimmed.slice(6).trim();
              continue;
            }

            if (trimmed.startsWith('data:')) {
              const data = trimmed.slice(5).trim();

              if (data === '[DONE]') {
                subject.next({ type: 'done' });
                subject.complete();
                return true;
              }

              try {
                const parsed = JSON.parse(data);
                if (parsed.type === 'token') {
                  subject.next({ type: 'token', delta: parsed.delta });
                } else if (parsed.type === 'tool_call') {
                  subject.next({
                    type: 'tool_call',
                    name: parsed.name,
                    args: parsed.args ?? {},
                    composite: parsed.composite,
                  });
                } else if (parsed.type === 'tool_result') {
                  subject.next({
                    type: 'tool_result',
                    name: parsed.name,
                    content: parsed.content,
                    isError: parsed.isError ?? false,
                  });
                }
              } catch {
                // ignore parse errors
              }
            }
          }
          return false;
        };

        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            if (processLine()) break;
          }
        } catch (err) {
          if ((err as Error).name !== 'AbortError') {
            subject.next({ type: 'truncated', reason: (err as Error).message });
          }
        } finally {
          subject.complete();
        }
      })
      .catch((err) => {
        if ((err as Error).name !== 'AbortError') {
          subject.next({ type: 'truncated', reason: err.message });
        }
        subject.complete();
      });

    return new Observable((observer) => {
      const subscription = subject.subscribe(observer);
      return () => {
        controller.abort();
        subscription.unsubscribe();
      };
    });
  }
}
