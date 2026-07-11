import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { VoiceTranscriptionService } from './voice-transcription.service';

class MockWebSocket {
  static instances: MockWebSocket[] = [];
  static OPEN = 1;

  readyState = MockWebSocket.OPEN;
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  sent: string[] = [];

  constructor(public url: string) {
    MockWebSocket.instances.push(this);
    queueMicrotask(() => this.onopen?.());
  }

  send(data: string): void {
    this.sent.push(data);
  }

  close(): void {
    this.readyState = 3;
    this.onclose?.();
  }

  emit(message: object): void {
    this.onmessage?.({ data: JSON.stringify(message) });
  }
}

describe('VoiceTranscriptionService', () => {
  let service: VoiceTranscriptionService;

  beforeEach(() => {
    MockWebSocket.instances = [];
    vi.stubGlobal('WebSocket', MockWebSocket);
    TestBed.configureTestingModule({});
    service = TestBed.inject(VoiceTranscriptionService);
  });

  afterEach(() => {
    service.disconnect();
    vi.unstubAllGlobals();
  });

  it('should_update_partial_text_when_partial_message_received', () => {
    service.connect();
    const socket = MockWebSocket.instances[0];
    socket.emit({ type: 'partial', text: 'hello' });

    expect(service.partialText()).toBe('hello');
  });

  it('should_resolve_final_text_when_stop_sent', async () => {
    service.connect();
    const socket = MockWebSocket.instances[0];
    const finalPromise = service.stopAndWaitForFinal();

    expect(socket.sent.at(-1)).toBe(JSON.stringify({ type: 'stop' }));
    socket.emit({ type: 'final', text: 'hello world' });

    await expect(finalPromise).resolves.toBe('hello world');
    expect(service.finalText()).toBe('hello world');
  });

  it('should_set_error_when_error_message_received', async () => {
    service.connect();
    const socket = MockWebSocket.instances[0];
    const finalPromise = service.stopAndWaitForFinal();
    socket.emit({ type: 'error', text: 'ASR failed' });

    await expect(finalPromise).rejects.toThrow('ASR failed');
    expect(service.error()).toBe('ASR failed');
  });

  it('should_send_audio_chunks_as_json', () => {
    service.connect();
    const socket = MockWebSocket.instances[0];
    service.sendAudioChunk('dGVzdA==');

    expect(socket.sent).toContain(JSON.stringify({ type: 'audio', data: 'dGVzdA==' }));
  });
});
