// Environment configuration for production
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  get wsUrl() {
    const isBrowser = typeof window !== 'undefined';
    const protocol = isBrowser && window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = isBrowser ? window.location.host : 'localhost:8080';
    return `${protocol}//${host}`;
  },
  agents: {
    supervisor: '/api/agents/supervisor/invoke/sse',
    kubernetes: '/api/agents/kubernetes/invoke/sse',
    monitoring: '/api/agents/monitoring/invoke/sse',
    model: '/api/agents/model/invoke/sse',
    llmops: '/api/agents/llmops/invoke/sse',
    aiops: '/api/agents/aiops/invoke/sse',
    vector: '/api/agents/vector/invoke/sse',
  },
  services: {
    text: '/api/text',
    vision: '/api/vision',
    rag: '/api/rag',
    tts: '/api/tts',
    image: '/api/image',
  },
};
