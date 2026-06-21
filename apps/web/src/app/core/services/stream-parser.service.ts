import { Injectable } from '@angular/core';

export interface StreamParserState {
  buffer: string;
  currentEvent: string;
}

export interface StreamChunk {
  event: string;
  data: string;
  raw: string;
}

@Injectable({ providedIn: 'root' })
export class StreamParserService {
  createInitialState(): StreamParserState {
    return {
      buffer: '',
      currentEvent: '',
    };
  }

  parseChunk(chunk: string, state: StreamParserState): { state: StreamParserState; events: StreamChunk[] } {
    state.buffer += chunk;
    const events: StreamChunk[] = [];
    const lines = state.buffer.split('\n');
    state.buffer = lines.pop() ?? '';

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) {
        state.currentEvent = '';
        continue;
      }

      if (trimmed.startsWith('event:')) {
        state.currentEvent = trimmed.slice(6).trim();
        continue;
      }

      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim();
        events.push({
          event: state.currentEvent,
          data,
          raw: trimmed,
        });
      }
    }

    return { state, events };
  }

  parseJSON<T>(data: string): T | null {
    try {
      return JSON.parse(data) as T;
    } catch {
      return null;
    }
  }
}
