import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright Configuration for AI Agents E2E Tests
 * 
 * This configuration is designed for testing the AI Agents chat functionality
 * with proper browser settings, timeouts, and test options.
 */

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.spec.ts',
  
  /* Run tests in files in parallel */
  fullyParallel: true,
  
  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: !!process.env.CI,
  
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  
  /* Opt out of parallel tests on CI */
  workers: process.env.CI ? 1 : undefined,
  
  /* Reporter to use */
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list'],
  ],

  /* Shared settings for all projects */
  use: {
    /* Base URL for navigation */
    baseURL: 'http://localhost:5173',

    /* Collect trace when retrying the failed test */
    trace: 'on-first-retry',
    
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',

    /* Video recording */
    video: 'retain-on-failure',

    /* Ignore HTTPS errors in development */
    ignoreHTTPSErrors: true,

    /* Default timeout for each action */
    actionTimeout: 10000,

    /* Default timeout for navigation */
    navigationTimeout: 30000,
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // Firefox and Webkit require additional installation
    // Uncomment after running: pnpm exec playwright install firefox webkit
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],

  /* Run local dev server before starting the tests */
  webServer: {
    command: 'pnpm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
    stdout: 'pipe',
    stderr: 'pipe',
  },

  /* Test timeout - how long a single test can run */
  timeout: 180000, // 3 minutes for slow AI services
  
  /* Expect timeout - how long expect().toBeVisible() waits */
  expect: {
    timeout: 10000,
  },
});
