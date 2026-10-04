export type SpeechToTextConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface TranscriptionMessage {
  type?: string;
  text?: string;
  error?: string;
  message?: string;
}
