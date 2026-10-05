import { expect, test } from './support';

const ROUTES = [
  '/chat',
  '/rag',
  '/vision',
  '/mcp',
  '/eval',
  '/speech-to-text',
  '/pipelines',
  '/automations',
  '/agents',
  '/skills',
  '/metrics',
  '/privacy',
  '/policies',
  '/generate',
];

for (const route of ROUTES) {
  test(`should render ${route} without page errors`, async ({ page }) => {
    await page.goto(route);
    await expect(page.locator('main')).toBeVisible();
  });
}
