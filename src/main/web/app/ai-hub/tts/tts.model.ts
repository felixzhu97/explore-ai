// TTS Feature Models

export interface Voice {
  id: string;
  name: string;
  language: string;
  language_name?: string;
  gender?: string;
  provider: string;
  is_default: boolean;
}

export interface TTSRequest {
  text: string;
  voice?: string;
  speed?: number;
  output_format?: string;
}

export interface TTSResult {
  audioUrl: string;
  duration?: number;
}

export interface SynthesizeRequest {
  text: string;
  voice?: string;
  language?: string;
  speed?: number;
  pitch?: number;
  output_format?: 'mp3' | 'wav' | 'ogg' | 'flac';
}
