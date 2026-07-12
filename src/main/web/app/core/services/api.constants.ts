import type { ModelInfo, ProviderInfo, Voice } from '@shared/models';
import { environment } from '@env/environment';

export const API_BASE_URL = environment.apiBaseUrl;

export const DEFAULT_PROVIDERS: ProviderInfo[] = [
  {
    name: 'openai',
    displayName: 'OpenAI',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
    status: 'available',
  },
  {
    name: 'anthropic',
    displayName: 'Anthropic Claude',
    models: ['claude-3-5-sonnet', 'claude-3-opus', 'claude-3-haiku'],
    status: 'available',
  },
  {
    name: 'ollama',
    displayName: 'Ollama (Local)',
    models: ['llama3', 'mistral', 'codellama'],
    status: 'unavailable',
  },
];

export const DEFAULT_MODELS: Record<string, ModelInfo[]> = {
  anthropic: [
    { name: 'claude-3-5-sonnet', provider: 'anthropic' },
    { name: 'claude-3-opus', provider: 'anthropic' },
    { name: 'claude-3-haiku', provider: 'anthropic' },
  ],
  openai: [
    { name: 'gpt-4o', provider: 'openai' },
    { name: 'gpt-4o-mini', provider: 'openai' },
    { name: 'gpt-4-turbo', provider: 'openai' },
  ],
};

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
