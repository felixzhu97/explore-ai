import { expect, test } from '@playwright/test';
import {
  gotoAppPage,
  prepareVisualPage,
  sendRagMessage,
  setupCommonMocks,
  setupRagMocks,
  setupRagStreamMock,
  waitForChatReady,
} from './fixtures/mock-api';

test.describe('RAG UI visual regression', () => {
  test.beforeEach(async ({ page }) => {
    await setupCommonMocks(page);
    await setupRagMocks(page);
    await setupRagStreamMock(page);
  });

  test('welcome empty state with nx-welcome and nx-prompts', async ({ page }) => {
    await gotoAppPage(page, '/rag');
    await waitForChatReady(page);
    await prepareVisualPage(page);

    const main = page.locator('main');
    await expect(main.locator('h2').filter({ hasText: 'Document Q&A' })).toBeVisible();
    await expect(main.locator('nx-welcome')).toBeVisible();
    await expect(main.locator('nx-prompts')).toBeVisible();

    await expect(main).toHaveScreenshot('rag-welcome.png');
  });

  test('conversation with sources footer in nx-bubble-list', async ({ page }) => {
    await gotoAppPage(page, '/rag');
    await waitForChatReady(page);
    await sendRagMessage(page, 'What is this document about?');
    await expect(page.locator('nx-bubble-list')).toBeVisible();
    await expect(page.getByText('What is this document about?')).toBeVisible();
    await expect(page.getByText(/Based on your documents/)).toBeVisible();
    await expect(page.getByText(/Based on 2 source/)).toBeVisible();

    await prepareVisualPage(page);
    await expect(page.locator('main')).toHaveScreenshot('rag-conversation.png');
  });

  test('expanded RAG sources panel', async ({ page }) => {
    await gotoAppPage(page, '/rag');
    await waitForChatReady(page);
    await sendRagMessage(page, 'Summarize the key points');
    await expect(page.getByText(/Based on 2 source/)).toBeVisible();
    await page.getByText(/Based on 2 source/).click();
    await expect(page.getByText('bubble component supports')).toBeVisible();

    await prepareVisualPage(page);
    await expect(page.locator('main')).toHaveScreenshot('rag-sources-expanded.png');
  });
});
