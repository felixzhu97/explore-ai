import { defineConfig, devices } from '@playwright/test';

const NO_PROXY = '127.0.0.1,localhost';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
    locale: 'en-US',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: './gradlew bootRun',
      url: 'http://localhost:9000/actuator/health',
      reuseExistingServer: true,
      timeout: 300_000,
      env: {
        NO_PROXY,
        H2_URL: 'jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1',
        LAUNCHDARKLY_ENABLED: 'false',
        APP_AUTOMATION_SCAN_ENABLED: 'false',
        APP_OAUTH_GOOGLE_ENABLED: 'false',
        APP_OAUTH_GITHUB_ENABLED: 'false',
        APP_OAUTH_EXPLORE_IAM_ENABLED: 'false',
        APP_OAUTH_EXPLORE_IAM_RESOURCE_SERVER: 'false',
      },
    },
    {
      command: 'pnpm ng serve --port 4200 --host localhost',
      url: 'http://localhost:4200',
      reuseExistingServer: true,
      timeout: 180_000,
      env: { NO_PROXY },
    },
  ],
});
