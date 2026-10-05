import { expect, test } from './support';

test('should create and delete a skill without description', async ({ page }) => {
  const name = `E2E skill ${Date.now()}`;
  await page.goto('/skills');

  await page.getByRole('button', { name: 'New Skill' }).click();
  await page.getByLabel('Name').fill(name);
  await page.getByLabel('Instructions').fill('Answer in one sentence.');
  await page.getByRole('button', { name: 'Save' }).click();

  const row = page.getByRole('listitem').filter({ hasText: name });
  await expect(row).toBeVisible();

  page.once('dialog', dialog => dialog.accept());
  await row.getByRole('button', { name: 'Delete' }).click();
  await expect(row).toHaveCount(0);
});
