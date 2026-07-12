// Environment configuration for development
export const environment = {
  production: false,
  apiBaseUrl: '/api',
  wsUrl: 'ws://localhost:9000',
  modules: {
    vision: true,
    audioAsr: true,
    mcp: true,
    eval: true,
  },
};
