export type AsrConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface AsrServerMessage {
  type?: string;
  text?: string;
  error?: string;
  message?: string;
}
