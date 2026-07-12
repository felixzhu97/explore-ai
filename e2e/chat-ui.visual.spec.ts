import { expect, test } from '@playwright/test';
import {
  gotoAppPage,
  prepareVisualPage,
  sendChatMessage,
  setupChatStreamMock,
  setupCommonMocks,
  waitForBubbleConversation,
  waitForChatReady,
} from './fixtures/mock-api';

test.describe('Chat UI visual regression', () => {
  test.beforeEach(async ({ page }) => {
    await setupCommonMocks(page);
    await setupChatStreamMock(page);
  });

  test('welcome empty state with nx-welcome and nx-prompts', async ({ page }) => {
    await gotoAppPage(page, '/chat');
    await waitForChatReady(page);
    await prepareVisualPage(page);

    const main = page.locator('main');
    await expect(main.getByText('How can I help you today?')).toBeVisible();
    await expect(main.getByText('Suggested prompts')).toBeVisible();
    await expect(main.locator('nx-welcome')).toBeVisible();
    await expect(main.locator('nx-prompts')).toBeVisible();

    await expect(main).toHaveScreenshot('chat-welcome.png');
  });

  test('conversation with nx-bubble-list user and assistant bubbles', async ({ page }) => {
    await gotoAppPage(page, '/chat');
    await waitForChatReady(page);
    await sendChatMessage(page, 'Explain ng-zorro-x bubble components');
    await waitForBubbleConversation(page, 'Explain ng-zorro-x bubble components');
    await expect(page.locator('main').getByText('markdown')).toBeVisible();

    await prepareVisualPage(page);
    await expect(page.locator('main')).toHaveScreenshot('chat-conversation.png');
  });
});
