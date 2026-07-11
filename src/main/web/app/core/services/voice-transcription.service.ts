import { Injectable, signal } from '@angular/core';

export type TranscriptionMessageType = 'partial' | 'final' | 'error';

export interface TranscriptionMessage {
  type: TranscriptionMessageType;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class VoiceTranscriptionService {
  readonly partialText = signal('');
  readonly finalText = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly isConnected = signal(false);

  private socket: WebSocket | null = null;
  private finalResolver: ((text: string) => void) | null = null;
  private finalRejecter: ((reason: Error) => void) | null = null;

  connect(): void {
    this.disconnect();
    this.partialText.set('');
    this.finalText.set(null);
    this.error.set(null);

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocol}//${window.location.host}/ws/audio/transcribe`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => this.isConnected.set(true);
    this.socket.onclose = () => this.isConnected.set(false);
    this.socket.onerror = () => {
      this.error.set('WebSocket connection failed');
      this.rejectFinal(new Error('WebSocket connection failed'));
    };
    this.socket.onmessage = event => this.handleMessage(event.data);
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.isConnected.set(false);
  }

  sendAudioChunk(base64Wav: string): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    this.socket.send(JSON.stringify({ type: 'audio', data: base64Wav }));
  }

  stopAndWaitForFinal(): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        reject(new Error('Transcription socket is not connected'));
        return;
      }
      this.finalResolver = resolve;
      this.finalRejecter = reject;
      this.socket.send(JSON.stringify({ type: 'stop' }));
    });
  }

  private handleMessage(raw: string): void {
    let message: TranscriptionMessage;
    try {
      message = JSON.parse(raw) as TranscriptionMessage;
    } catch {
      this.error.set('Invalid transcription response');
      this.rejectFinal(new Error('Invalid transcription response'));
      return;
    }

    if (message.type === 'partial') {
      this.partialText.set(message.text);
      return;
    }

    if (message.type === 'final') {
      this.finalText.set(message.text);
      this.partialText.set(message.text);
      this.resolveFinal(message.text);
      this.disconnect();
      return;
    }

    if (message.type === 'error') {
      this.error.set(message.text);
      this.rejectFinal(new Error(message.text));
      this.disconnect();
    }
  }

  private resolveFinal(text: string): void {
    this.finalResolver?.(text);
    this.finalResolver = null;
    this.finalRejecter = null;
  }

  private rejectFinal(reason: Error): void {
    this.finalRejecter?.(reason);
    this.finalResolver = null;
    this.finalRejecter = null;
  }
}
