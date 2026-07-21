import { Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import type { AsrConnectionState, AsrServerMessage } from './asr.model';

@Injectable({ providedIn: 'root' })
export class AsrService {
  private socket: WebSocket | null = null;

  readonly connectionState = signal<AsrConnectionState>('disconnected');
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
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.connectionState.set('connected');
    };

    this.socket.onmessage = (event) => {
      const payload = String(event.data);
      this.lastMessage.set(payload);
      try {
        const message = JSON.parse(payload) as AsrServerMessage;
        if (message.text) {
          this.transcript.update(current => current + message.text);
        }
        if (message.error || message.message) {
          this.error.set(message.error ?? message.message ?? 'ASR error');
        }
      } catch {
        this.transcript.update(current => current + payload);
      }
    };

    this.socket.onerror = () => {
      this.connectionState.set('error');
      this.error.set('WebSocket connection failed');
    };

    this.socket.onclose = () => {
      if (this.connectionState() !== 'error') {
        this.connectionState.set('disconnected');
      }
    };
  }

  sendStop(): void {
    this.sendJson({ type: 'stop' });
  }

  sendTestAudioPayload(): void {
    this.sendJson({ type: 'audio', data: '' });
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connectionState.set('disconnected');
  }

  private sendJson(payload: Record<string, string>): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      this.error.set('WebSocket is not connected');
      return;
    }
    this.socket.send(JSON.stringify(payload));
  }
}
