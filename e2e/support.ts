import { expect, test as base } from '@playwright/test';

export const test = base.extend<{ pageErrors: string[] }>({
  pageErrors: [
    async ({ page }, use) => {
      const errors: string[] = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.addInitScript(() => {
        localStorage.setItem('explore-ai.i18n.language', 'en');
        localStorage.setItem(
          'explore-ai.privacy.consent',
          JSON.stringify({ decided: true, analytics: false, contactEmail: '' }),
        );
      });
      await use(errors);
      expect(errors).toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };
