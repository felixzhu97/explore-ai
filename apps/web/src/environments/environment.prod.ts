// Environment configuration for production
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  wsUrl: 'ws://localhost:9000',
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
