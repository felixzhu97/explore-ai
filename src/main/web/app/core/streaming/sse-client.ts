export function parseSseToken(data: string): string | null {
  if (data === '') {
    return '\n';
  }

  const first = data.trimStart()[0];
  if (first === '{' || first === '[') {
    try {
      const parsed = JSON.parse(data) as { token?: string | number };
      if (parsed && typeof parsed === 'object' && 'token' in parsed) {
        const token = parsed.token;
        if (token !== null && token !== undefined) {
          return String(token);
        }
      }
    } catch {
      return data;
    }
    return null;
  }

  if (first === '"') {
    try {
      const parsed = JSON.parse(data);
      return typeof parsed === 'string' ? parsed : data;
    } catch {
      return data;
    }
  }

  return data;
}

interface SseEventPayload {
  eventType: string;
  data: string;
}

class SseEventAssembler {
  private eventType = '';
  private dataLines: string[] = [];

  pushLine(line: string): SseEventPayload | null {
    if (!line) {
      return this.flush();
    }

    if (line.startsWith('event:')) {
      this.eventType = line.slice(6).trim();
      return null;
    }

    if (line.startsWith('data:')) {
      let data = line.slice(5);
      if (data.startsWith(' ')) {
        data = data.slice(1);
      }
      this.dataLines.push(data);
    }

    return null;
  }

  flush(): SseEventPayload | null {
    if (this.dataLines.length === 0) {
      this.eventType = '';
      return null;
    }

    const payload: SseEventPayload = {
      eventType: this.eventType,
      data: this.dataLines.join('\n'),
    };
    this.dataLines = [];
    this.eventType = '';
    return payload;
  }
}

export interface StreamSsePostOptions {
  onEvent: (event: SseEventPayload) => boolean | void;
  onDone: () => void;
  onError: (err: Error) => void;
}

export function streamSsePost(
  url: string,
  body: unknown,
  options: StreamSsePostOptions,
): { abort: () => void } {
  const controller = new AbortController();
  const sseAssembler = new SseEventAssembler();

  const readerPromise = fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: controller.signal,
  }).then(async (response) => {
    if (!response.ok) {
      options.onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
      return;
    }

    if (!response.body) {
      options.onError(new Error('No response body'));
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let finished = false;

    const finish = () => {
      if (!finished) {
        finished = true;
        options.onDone();
      }
    };

    const handleSseEvent = (event: SseEventPayload): boolean => {
      const shouldStop = options.onEvent(event);
      return shouldStop === true;
    };

    const processBuffer = (flushPending = false) => {
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const rawLine of lines) {
        const line = rawLine.replace(/\r$/, '');
        const event = sseAssembler.pushLine(line);
        if (event !== null && handleSseEvent(event)) {
          return true;
        }
      }

      if (flushPending) {
        const event = sseAssembler.flush();
        if (event !== null && handleSseEvent(event)) {
          return true;
        }
      }

      return false;
    };

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (value) {
          buffer += decoder.decode(value, { stream: !done });
        }
        if (processBuffer()) {
          finished = true;
          break;
        }
        if (done) {
          buffer += decoder.decode(undefined, { stream: false });
          processBuffer(true);
          break;
        }
      }
      finish();
    } catch (err) {
      if ((err as Error).name !== 'AbortError') {
        options.onError(err as Error);
      }
    }
  });

  readerPromise.catch((err) => {
    if ((err as Error).name !== 'AbortError') {
      options.onError(err as Error);
    }
  });

  return {
    abort: () => controller.abort(),
  };
}
