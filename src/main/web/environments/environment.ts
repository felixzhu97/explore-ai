// Environment configuration for development
export const environment = {
  production: false,
  apiBaseUrl: '/api',
  wsUrl: 'ws://localhost:9000',
  launchDarklyClientSideId: '',
  datadog: {
    applicationId: '',
    clientToken: '',
    site: 'us5.datadoghq.com',
    service: 'explore-ai-web',
    env: 'development',
    version: '0.0.1',
  },
  featureFlagFallback: {
    'module-vision': true,
    'module-audio-asr': true,
    'module-mcp': true,
    'module-eval': true,
  },
};
