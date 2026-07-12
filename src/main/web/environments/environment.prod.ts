// Environment configuration for production (cloud-minimal)
export const environment = {
  production: true,
  apiBaseUrl: 'https://explore-ai-production.up.railway.app/api',
  wsUrl: 'wss://explore-ai-production.up.railway.app',
  modules: {
    vision: false,
    audioAsr: false,
    mcp: false,
    eval: false,
  },
};
