import type { ModelInfo, ProviderInfo, Voice } from '@shared/models';
import { environment } from '@env/environment';

export const API_BASE_URL = environment.apiBaseUrl;

export const DEFAULT_PROVIDERS: ProviderInfo[] = [
  {
    name: 'openai',
    displayName: 'DeepSeek',
    models: ['deepseek-v4-flash', 'deepseek-v4-pro'],
    status: 'available',
  },
  {
    name: 'anthropic',
    displayName: 'Anthropic Claude',
    models: [
      'claude-fable-5',
      'claude-opus-4-8',
      'claude-sonnet-5',
      'claude-haiku-4-5-20251001',
      'claude-opus-4-7',
      'claude-opus-4-6',
      'claude-sonnet-4-6',
      'claude-sonnet-4-5-20250929',
      'claude-opus-4-5-20251101',
    ],
    status: 'unavailable',
  },
  {
    name: 'ollama',
    displayName: 'Ollama (Local)',
    models: [
      'qwen3.5:35b',
      'qwen3:8b',
      'qwen3:14b',
      'llama3.2',
      'llama3.1:8b',
      'gemma3:12b',
      'mistral',
      'deepseek-r1:14b',
    ],
    status: 'unavailable',
  },
];

export const DEFAULT_MODELS: Record<string, ModelInfo[]> = {
  openai: [
    { name: 'deepseek-v4-flash', provider: 'openai' },
    { name: 'deepseek-v4-pro', provider: 'openai' },
  ],
  anthropic: [
    { name: 'claude-fable-5', provider: 'anthropic' },
    { name: 'claude-opus-4-8', provider: 'anthropic' },
    { name: 'claude-sonnet-5', provider: 'anthropic' },
    { name: 'claude-haiku-4-5-20251001', provider: 'anthropic' },
    { name: 'claude-opus-4-7', provider: 'anthropic' },
    { name: 'claude-opus-4-6', provider: 'anthropic' },
    { name: 'claude-sonnet-4-6', provider: 'anthropic' },
    { name: 'claude-sonnet-4-5-20250929', provider: 'anthropic' },
    { name: 'claude-opus-4-5-20251101', provider: 'anthropic' },
  ],
  ollama: [
    { name: 'qwen3.5:35b', provider: 'ollama' },
    { name: 'qwen3:8b', provider: 'ollama' },
    { name: 'qwen3:14b', provider: 'ollama' },
    { name: 'llama3.2', provider: 'ollama' },
    { name: 'llama3.1:8b', provider: 'ollama' },
    { name: 'gemma3:12b', provider: 'ollama' },
    { name: 'mistral', provider: 'ollama' },
    { name: 'deepseek-r1:14b', provider: 'ollama' },
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
