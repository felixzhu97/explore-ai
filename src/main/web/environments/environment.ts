// Environment configuration for development
export const environment = {
  production: false,
  apiBaseUrl: '/api',
  wsUrl: 'ws://localhost:9000',
  launchDarklyClientSideId: '',
  featureFlagFallback: {
    'module-vision': true,
    'module-audio-asr': true,
    'module-mcp': true,
    'module-eval': true,
  },
};
