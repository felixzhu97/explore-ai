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

  parseChunk(
    chunk: string,
    state: StreamParserState,
  ): { state: StreamParserState; events: StreamChunk[] } {
    const newState = { ...state, buffer: state.buffer + chunk };
    const events: StreamChunk[] = [];
    const lines = newState.buffer.split('\n');
    newState.buffer = lines.pop() ?? '';

    let currentEvent = newState.currentEvent;
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
        events.push({
          event: currentEvent,
          data,
          raw: trimmed,
        });
      }
    }

    return { state: { ...newState, currentEvent }, events };
  }

  parseJSON<T>(data: string): T | null {
    try {
      return JSON.parse(data) as T;
    } catch {
      return null;
    }
  }
}
