// TTS Feature Models — aligned with TtsRequest / VoiceResponse

export interface Voice {
  id: string;
  name: string;
  language: string;
  languageName?: string;
  gender?: string;
  provider?: string;
  isDefault?: boolean;
}

/** POST /api/audio/speak */
export interface TtsRequest {
  text: string;
  voice?: string;
  speed?: number;
  outputFormat?: string;
}
