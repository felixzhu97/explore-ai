// Environment configuration for production (cloud-minimal)
export const environment = {
  production: true,
  apiBaseUrl: 'https://explore-ai-production.up.railway.app/api',
  wsUrl: 'wss://explore-ai-production.up.railway.app',
  launchDarklyClientSideId: '',
  featureFlagFallback: {
    'module-vision': false,
    'module-audio-asr': false,
    'module-mcp': false,
    'module-eval': false,
  },
};
