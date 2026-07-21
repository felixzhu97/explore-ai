import type { Voice } from './tts.model';

export const DEFAULT_VOICES: Voice[] = [
  {
    id: 'alloy',
    name: 'Alloy',
    language: 'en',
    provider: 'openai',
    isDefault: true,
  },
  {
    id: 'nova',
    name: 'Nova',
    language: 'en',
    provider: 'openai',
    isDefault: false,
  },
];
