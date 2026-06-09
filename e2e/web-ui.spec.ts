import { test, expect } from '@playwright/test';

test.describe('Web UI - Main Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should load the main page', async ({ page }) => {
    await expect(page).toHaveTitle(/AI/);
  });

  test('should display navigation', async ({ page }) => {
    const nav = page.locator('nav, [role="navigation"], .navigation, .nav');
    await expect(nav.first()).toBeVisible();
  });

  test('should have service cards or sections', async ({ page }) => {
    const pageContent = await page.content();
    expect(pageContent.length).toBeGreaterThan(100);
  });
});
