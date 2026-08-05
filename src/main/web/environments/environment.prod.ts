// Environment configuration for production (cloud-minimal)
// Browser calls same-origin /api (Vercel rewrite → Render) so HttpOnly
// client identity cookies stay first-party (SameSite=Lax works).
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  wsUrl: 'wss://explore-ai-3krr.onrender.com',
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
    'module-pipelines': true,
    'module-skills': true,
  },
};
