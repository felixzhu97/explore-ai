/** One web search hit: Java `WebSourcesEvent.Source`, shaped like `WebSourceResponse`. */
export interface WebSource {
  title: string;
  url: string;
  snippet: string;
  publishedAt: string | null;
}

export interface StreamTokenEvent {
  type: 'message';
  token: string;
}

export interface ToolCallEvent {
  type: 'tool_call';
  name: string;
  input: string;
}

export interface ToolResultEvent {
  type: 'tool_result';
  name: string;
  ok: boolean;
  output: string;
}

export interface WebSourcesEvent {
  type: 'sources';
  query: string;
  items: WebSource[];
}

export type ChatStreamEvent =
  | StreamTokenEvent
  | ToolCallEvent
  | ToolResultEvent
  | WebSourcesEvent;

/** SSE data: JSON objects/strings vs plain token text (e.g. numeric chunks). */
export function parseSseToken(data: string): string | null {
  const event = parseChatStreamEvent(data);
  if (event === null) {
    return null;
  }
  return event.type === 'message' ? event.token : null;
}

/** The JSON field when it is a string; any other value reads as empty. */
export function readStringField(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

/** The JSON value as an object, or null for arrays, primitives and null. */
export function readObjectOrNull(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

function parseWebSource(value: unknown): WebSource | null {
  const row = readObjectOrNull(value);
  if (row === null) {
    return null;
  }
  const publishedAt = row['publishedAt'];
  return {
    title: readStringField(row['title']),
    url: readStringField(row['url']),
    snippet: readStringField(row['snippet']),
    publishedAt: typeof publishedAt === 'string' && publishedAt.trim() !== ''
      ? publishedAt.trim()
      : null,
  };
}

/** Parse chat SSE data payloads (message tokens, tool events, web sources). */
export function parseChatStreamEvent(data: string): ChatStreamEvent | null {
  if (data === '') {
    return { type: 'message', token: '\n' };
  }

  const first = data.trimStart()[0];
  if (first === '{' || first === '[') {
    let json: unknown;
    try {
      json = JSON.parse(data);
    } catch {
      return { type: 'message', token: data };
    }
    const parsed = readObjectOrNull(json);
    if (parsed === null) {
      return null;
    }

    switch (parsed['type']) {
      case 'tool_call':
        return {
          type: 'tool_call',
          name: readStringField(parsed['name']),
          input: readStringField(parsed['input']),
        };
      case 'tool_result':
        return {
          type: 'tool_result',
          name: readStringField(parsed['name']),
          ok: parsed['ok'] === true,
          output: readStringField(parsed['output']),
        };
      case 'sources': {
        const rawItems: unknown = parsed['items'];
        const items = Array.isArray(rawItems)
          ? rawItems.map(parseWebSource).filter(item => item !== null)
          : [];
        return { type: 'sources', query: readStringField(parsed['query']), items };
      }
      case 'message': {
        const token = parsed['token'];
        return typeof token === 'string' ? { type: 'message', token } : null;
      }
      default:
        return null;
    }
  }

  if (first === '"') {
    try {
      const parsed: unknown = JSON.parse(data);
      return typeof parsed === 'string' ? { type: 'message', token: parsed } : { type: 'message', token: data };
    } catch {
      return { type: 'message', token: data };
    }
  }

  return { type: 'message', token: data };
}

export interface SseEventPayload {
  eventType: string;
  data: string;
}

/** Accumulates SSE data lines until a blank line, joining with `\n` per SSE spec. */
export class SseEventAssembler {
  #eventType = '';
  #dataLines: string[] = [];

  pushLine(line: string): SseEventPayload | null {
    if (line === '') {
      return this.flush();
    }

    if (line.startsWith('event:')) {
      this.#eventType = line.slice(6).trim();
      return null;
    }

    if (line.startsWith('data:')) {
      let data = line.slice(5);
      if (data.startsWith(' ')) {
        data = data.slice(1);
      }
      this.#dataLines.push(data);
    }

    return null;
  }

  flush(): SseEventPayload | null {
    if (this.#dataLines.length === 0) {
      this.#eventType = '';
      return null;
    }

    const payload: SseEventPayload = {
      eventType: this.#eventType,
      data: this.#dataLines.join('\n'),
    };
    this.#dataLines = [];
    this.#eventType = '';
    return payload;
  }
}

export interface SseStreamHandlers {
  onEvent: (event: SseEventPayload) => boolean;
  onDone: () => void;
  onError: (error: Error) => void;
}

export function streamSsePost(
  url: string,
  body: unknown,
  handlers: SseStreamHandlers,
): { abort: () => void } {
  const controller = new AbortController();
  const sseAssembler = new SseEventAssembler();
  let finished = false;

  const finish = () => {
    if (!finished) {
      finished = true;
      handlers.onDone();
    }
  };

  const readerPromise = fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
    },
    body: JSON.stringify(body),
    credentials: 'include',
    signal: controller.signal,
  }).then(async (response) => {
    if (!response.ok) {
      handlers.onError(new Error(`HTTP ${String(response.status)}: ${response.statusText}`));
      return;
    }

    if (response.body === null) {
      handlers.onError(new Error('No response body'));
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    const processBuffer = (flushPending = false) => {
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const rawLine of lines) {
        const line = rawLine.replace(/\r$/, '');
        const event = sseAssembler.pushLine(line);
        if (event !== null && handlers.onEvent(event)) {
          finished = true;
          return true;
        }
      }

      if (flushPending) {
        const event = sseAssembler.flush();
        if (event !== null && handlers.onEvent(event)) {
          finished = true;
          return true;
        }
      }

      return false;
    };

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (value !== undefined) {
          buffer += decoder.decode(value, { stream: !done });
        }
        if (processBuffer()) {
          break;
        }
        if (done) {
          buffer += decoder.decode(undefined, { stream: false });
          processBuffer(true);
          break;
        }
      }
      finish();
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        handlers.onError(error as Error);
      }
    }
  });

  readerPromise.catch((error: unknown) => {
    const failure = error instanceof Error ? error : new Error(String(error));
    if (failure.name !== 'AbortError') {
      handlers.onError(failure);
    }
  });

  return {
    abort: () => controller.abort(),
  };
}
