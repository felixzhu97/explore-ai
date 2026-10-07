import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SpeechToTextService, parseTranscriptionResponse } from './speech-to-text.service';

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

describe('SpeechToTextService', () => {
  beforeEach(() => {
    FakeWebSocket.instances = [];
    vi.stubGlobal('WebSocket', FakeWebSocket);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should connect to transcription websocket when connect called', () => {
    const service = new SpeechToTextService();

    service.connectStream();

    const socket = latestSocket();
    expect(socket.url).toBe('ws://localhost:9000/ws/audio/transcribe');
    expect(service.connectionState()).toBe('connecting');

    socket.open();

    expect(service.connectionState()).toBe('connected');
    expect(service.error()).toBeNull();
    expect(service.transcript()).toBe('');
    expect(service.lastMessage()).toBeNull();
  });

  it('should replace transcript with latest frame text when transcription received', () => {
    const service = connectService();
    const socket = latestSocket();

    socket.receive('{"type":"partial","text":"hello"}');
    socket.receive('{"type":"final","text":"hello world"}');

    expect(service.transcript()).toBe('hello world');
    expect(service.lastMessage()).toBe('{"type":"final","text":"hello world"}');
    expect(service.error()).toBeNull();
  });

  it('should ignore frames that are not transcription responses', () => {
    const service = connectService();

    latestSocket().receive('partial transcript');
    latestSocket().receive('{"text":"untyped"}');

    expect(service.transcript()).toBe('');
    expect(service.lastMessage()).toBe('{"text":"untyped"}');
  });

  it('should set error when server sends error frame', () => {
    const service = connectService();

    latestSocket().receive('{"type":"error","text":"transcription model unavailable"}');

    expect(service.error()).toBe('transcription model unavailable');
    expect(service.transcript()).toBe('');
    expect(service.connectionState()).toBe('connected');
  });

  it('should send commands when socket is open', () => {
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

  it('should set error when sending without connected socket', () => {
    const service = new SpeechToTextService();

    service.sendStop();

    expect(service.error()).toBe('notConnected');
  });

  it('should keep error state when socket closes after transport error', () => {
    const service = connectService();
    const socket = latestSocket();

    socket.fail();
    socket.close();

    expect(service.connectionState()).toBe('error');
    expect(service.error()).toBe('connectionFailed');
  });
});

function connectService(): SpeechToTextService {
  const service = new SpeechToTextService();
  service.connectStream();
  latestSocket().open();
  return service;
}

function latestSocket(): FakeWebSocket {
  const socket = FakeWebSocket.instances.at(-1);
  if (socket === undefined) {
    throw new Error('Expected a fake WebSocket instance');
  }
  return socket;
}

describe('parseTranscriptionResponse', () => {
  it('should parse typed frames', () => {
    expect(parseTranscriptionResponse('{"type":"partial","text":"hi"}')).toEqual({ type: 'partial', text: 'hi' });
  });

  it('should default missing text to empty string', () => {
    expect(parseTranscriptionResponse('{"type":"final"}')).toEqual({ type: 'final', text: '' });
  });

  it('should return null when type is unknown or payload is not json', () => {
    expect(parseTranscriptionResponse('{"type":"delta","text":"x"}')).toBeNull();
    expect(parseTranscriptionResponse('plain')).toBeNull();
  });
});
