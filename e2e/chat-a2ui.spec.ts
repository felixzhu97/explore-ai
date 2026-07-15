import { expect, test } from '@playwright/test';
import {
  gotoAppPage,
  sendChatMessage,
  setupChatStreamMock,
  setupCommonMocks,
  waitForBubbleConversation,
  waitForChatReady,
} from './fixtures/mock-api';

const A2UI_BAR_CHART_RESPONSE = [
  'Here is a **bar chart** of quarterly sales:',
  '',
  '```a2ui',
  '{"version":"v0.9","createSurface":{"surfaceId":"chart-1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}',
  '{"version":"v0.9","updateComponents":{"surfaceId":"chart-1","components":[{"id":"root","component":"Column","children":["c1"]},{"id":"c1","component":"Chart","type":"bar","title":"Sales","chartData":[{"label":"Q1","value":10},{"label":"Q2","value":20},{"label":"Q3","value":15}]}]}}',
  '```',
  '',
  'The chart above compares Q1–Q3.',
].join('\n');

const A2UI_PENDING_RESPONSE = [
  'Generating visualization…',
  '',
  '```a2ui',
  '{"version":"v0.9","createSurface":{"surfaceId":"pending-1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}',
].join('\n');

test.describe('Chat A2UI Chart functional', () => {
  test.beforeEach(async ({ page }) => {
    await setupCommonMocks(page);
  });

  test('should_render_a2ui_bar_chart_surface_with_echarts', async ({ page }) => {
    await setupChatStreamMock(page, A2UI_BAR_CHART_RESPONSE);
    await gotoAppPage(page, '/chat');
    await waitForChatReady(page);

    await sendChatMessage(page, 'Show a bar chart of sales');
    await waitForBubbleConversation(page, 'Show a bar chart of sales');

    await expect(page.locator('main').getByText('bar chart', { exact: true })).toBeVisible();
    await expect(page.locator('app-markdown-with-a2ui')).toBeVisible();
    await expect(page.locator('a2ui-v09-surface')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('app-a2ui-chart')).toBeVisible({ timeout: 15_000 });

    const chartHost = page.locator('app-a2ui-chart');
    await expect(chartHost.locator('canvas').first()).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByText('界面生成中…')).toHaveCount(0);
  });

  test('should_show_pending_hint_for_unclosed_a2ui_fence', async ({ page }) => {
    await setupChatStreamMock(page, A2UI_PENDING_RESPONSE);
    await gotoAppPage(page, '/chat');
    await waitForChatReady(page);

    await sendChatMessage(page, 'Start generating a chart');
    await page.locator('nx-bubble-list').waitFor({ state: 'visible' });
    await page.locator('main').getByText('Start generating a chart').waitFor({ state: 'visible' });

    await expect(page.getByText('界面生成中…')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('app-a2ui-chart')).toHaveCount(0);
  });
});
