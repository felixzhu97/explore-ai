import type { ModelInfoResponse, ProviderInfoResponse } from './chat.service';

export const DEFAULT_PROVIDERS: ProviderInfoResponse[] = [
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

export const DEFAULT_MODELS: Record<string, ModelInfoResponse[]> = {
  openai: [
    { name: 'deepseek-v4-flash', provider: 'openai', description: 'DeepSeek V4 Flash' },
    { name: 'deepseek-v4-pro', provider: 'openai', description: 'DeepSeek V4 Pro' },
  ],
  anthropic: [
    { name: 'claude-fable-5', provider: 'anthropic', description: 'Claude Fable 5' },
    { name: 'claude-opus-4-8', provider: 'anthropic', description: 'Claude Opus 4.8' },
    { name: 'claude-sonnet-5', provider: 'anthropic', description: 'Claude Sonnet 5' },
    { name: 'claude-haiku-4-5-20251001', provider: 'anthropic', description: 'Claude Haiku 4.5' },
    { name: 'claude-opus-4-7', provider: 'anthropic', description: 'Claude Opus 4.7' },
    { name: 'claude-opus-4-6', provider: 'anthropic', description: 'Claude Opus 4.6' },
    { name: 'claude-sonnet-4-6', provider: 'anthropic', description: 'Claude Sonnet 4.6' },
    { name: 'claude-sonnet-4-5-20250929', provider: 'anthropic', description: 'Claude Sonnet 4.5' },
    { name: 'claude-opus-4-5-20251101', provider: 'anthropic', description: 'Claude Opus 4.5' },
  ],
  ollama: [
    { name: 'qwen3.5:35b', provider: 'ollama', description: 'Qwen 3.5 35B' },
    { name: 'qwen3:8b', provider: 'ollama', description: 'Qwen 3 8B' },
    { name: 'qwen3:14b', provider: 'ollama', description: 'Qwen 3 14B' },
    { name: 'llama3.2', provider: 'ollama', description: 'Llama 3.2' },
    { name: 'llama3.1:8b', provider: 'ollama', description: 'Llama 3.1 8B' },
    { name: 'gemma3:12b', provider: 'ollama', description: 'Gemma 3 12B' },
    { name: 'mistral', provider: 'ollama', description: 'Mistral' },
    { name: 'deepseek-r1:14b', provider: 'ollama', description: 'DeepSeek R1 14B' },
  ],
};
