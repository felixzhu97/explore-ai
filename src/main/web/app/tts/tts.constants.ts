import type { VoiceResponse } from './tts.service';

export const DEFAULT_VOICES: VoiceResponse[] = [
  {
    id: 'alloy',
    name: 'Alloy',
    language: 'en',
    gender: 'neutral',
  },
  {
    id: 'nova',
    name: 'Nova',
    language: 'en',
    gender: 'female',
  },
];
