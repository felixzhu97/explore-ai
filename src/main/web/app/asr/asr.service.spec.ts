import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AsrService } from './asr.service';

class FakeWebSocket {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 3;
  static instances: FakeWebSocket[] = [];

  readonly url: string;
  readyState = FakeWebSocket.CONNECTING;
  sentMessages: string[] = [];
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;

  constructor(url: string | URL) {
    this.url = String(url);
    FakeWebSocket.instances.push(this);
  }

  open(): void {
    this.readyState = FakeWebSocket.OPEN;
    this.onopen?.(new Event('open'));
  }

  receive(data: string): void {
    this.onmessage?.({ data } as MessageEvent);
  }

  fail(): void {
    this.onerror?.(new Event('error'));
  }

  send(data: string): void {
    this.sentMessages.push(data);
  }

  close(): void {
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.({} as CloseEvent);
  }
}

describe('AsrService', () => {
  beforeEach(() => {
    FakeWebSocket.instances = [];
    vi.stubGlobal('WebSocket', FakeWebSocket);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should_connect_to_asr_websocket_when_connect_called', () => {
    const service = new AsrService();

    service.connect();

    const socket = latestSocket();
    expect(socket.url).toBe('ws://localhost:9000/ws/audio/transcribe');
    expect(service.connectionState()).toBe('connecting');

    socket.open();

    expect(service.connectionState()).toBe('connected');
    expect(service.error()).toBeNull();
    expect(service.transcript()).toBe('');
    expect(service.lastMessage()).toBeNull();
  });

  it('should_append_transcript_and_store_last_message_when_json_text_received', () => {
    const service = connectService();
    const socket = latestSocket();

    socket.receive('{"text":"hello "}');
    socket.receive('{"text":"world"}');

    expect(service.transcript()).toBe('hello world');
    expect(service.lastMessage()).toBe('{"text":"world"}');
    expect(service.error()).toBeNull();
  });

  it('should_append_plain_text_when_message_is_not_json', () => {
    const service = connectService();

    latestSocket().receive('partial transcript');

    expect(service.transcript()).toBe('partial transcript');
    expect(service.lastMessage()).toBe('partial transcript');
  });

  it('should_set_error_when_server_message_reports_failure', () => {
    const service = connectService();

    latestSocket().receive('{"message":"transcription model unavailable"}');

    expect(service.error()).toBe('transcription model unavailable');
    expect(service.connectionState()).toBe('connected');
  });

  it('should_send_commands_when_socket_is_open', () => {
    const service = connectService();
    const socket = latestSocket();

    service.sendStop();
    service.sendTestAudioPayload();

    expect(socket.sentMessages).toEqual([
      '{"type":"stop"}',
      '{"type":"audio","data":""}',
    ]);
    expect(service.error()).toBeNull();
  });

  it('should_set_error_when_sending_without_connected_socket', () => {
    const service = new AsrService();

    service.sendStop();

    expect(service.error()).toBe('WebSocket is not connected');
  });

  it('should_keep_error_state_when_socket_closes_after_transport_error', () => {
    const service = connectService();
    const socket = latestSocket();

    socket.fail();
    socket.close();

    expect(service.connectionState()).toBe('error');
    expect(service.error()).toBe('WebSocket connection failed');
  });
});

function connectService(): AsrService {
  const service = new AsrService();
  service.connect();
  latestSocket().open();
  return service;
}

function latestSocket(): FakeWebSocket {
  const socket = FakeWebSocket.instances.at(-1);
  if (!socket) {
    throw new Error('Expected a fake WebSocket instance');
  }
  return socket;
}
