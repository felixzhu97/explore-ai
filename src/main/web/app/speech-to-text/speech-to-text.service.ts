import { Service, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { objectOrNull, stringField } from '../http/sse-client';
import { hasText, textOr } from '../shared/presence';

export type SpeechToTextConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

export type TranscriptionType = 'partial' | 'final' | 'error';

/** Matches the backend TranscriptionResponse WebSocket frame. */
export interface TranscriptionResponse {
  type: TranscriptionType;
  /** Full transcript so far, or the error reason when `type` is `error`. */
  text: string;
}

const TRANSCRIPTION_TYPES: readonly string[] =
  ['partial', 'final', 'error'] satisfies TranscriptionType[];

function isTranscriptionType(value: unknown): value is TranscriptionType {
  return typeof value === 'string' && TRANSCRIPTION_TYPES.includes(value);
}

/** Parse a transcription frame; null when the payload is not a TranscriptionResponse. */
export function parseTranscriptionResponse(
  payload: string,
): TranscriptionResponse | null {
  let json: unknown;
  try {
    json = JSON.parse(payload);
  } catch {
    return null;
  }
  const frame = objectOrNull(json);
  if (frame === null || !isTranscriptionType(frame['type'])) {
    return null;
  }
  return { type: frame['type'], text: stringField(frame['text']) };
}

@Service()
export class SpeechToTextService {
  #socket: WebSocket | null = null;

  readonly connectionState = signal<SpeechToTextConnectionState>('disconnected');
  readonly transcript = signal('');
  readonly lastMessage = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  connect(): void {
    this.disconnect();
    this.connectionState.set('connecting');
    this.error.set(null);
    this.transcript.set('');
    this.lastMessage.set(null);

    const url = `${environment.wsUrl}/ws/audio/transcribe`;
    this.#socket = new WebSocket(url);

    this.#socket.onopen = () => {
      this.connectionState.set('connected');
    };

    this.#socket.onmessage = (event) => {
      const payload = String(event.data);
      this.lastMessage.set(payload);
      const frame = parseTranscriptionResponse(payload);
      if (frame === null) {
        return;
      }
      if (frame.type === 'error') {
        this.error.set(textOr(frame.text, 'generic'));
      } else if (hasText(frame.text)) {
        this.transcript.set(frame.text);
      }
    };

    this.#socket.onerror = () => {
      this.connectionState.set('error');
      this.error.set('connectionFailed');
    };

    this.#socket.onclose = () => {
      if (this.connectionState() !== 'error') {
        this.connectionState.set('disconnected');
      }
    };
  }

  sendStop(): void {
    this.#sendJson({ type: 'stop' });
  }

  sendTestAudioPayload(): void {
    this.#sendJson({ type: 'audio', data: '' });
  }

  disconnect(): void {
    if (this.#socket !== null) {
      this.#socket.close();
      this.#socket = null;
    }
    this.connectionState.set('disconnected');
  }

  #sendJson(payload: Record<string, string>): void {
    if (this.#socket === null || this.#socket.readyState !== WebSocket.OPEN) {
      this.error.set('notConnected');
      return;
    }
    this.#socket.send(JSON.stringify(payload));
  }
}
