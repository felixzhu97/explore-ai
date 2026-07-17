// Environment configuration for production (cloud-minimal)
export const environment = {
  production: true,
  apiBaseUrl: 'https://explore-ai-production.up.railway.app/api',
  wsUrl: 'wss://explore-ai-production.up.railway.app',
  launchDarklyClientSideId: '6a53b2bf3d90280be0afbf03',
  datadog: {
    applicationId: '',
    clientToken: '',
    site: 'us5.datadoghq.com',
    service: 'explore-ai-web',
    env: 'production',
    version: '0.0.1',
  },
  featureFlagFallback: {
    'module-vision': false,
    'module-audio-asr': false,
    'module-mcp': false,
    'module-eval': false,
    'module-agents': true,
  },
};
