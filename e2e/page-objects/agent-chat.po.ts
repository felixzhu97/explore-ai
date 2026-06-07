import { Page, Locator, expect } from '@playwright/test';

export interface StatusBadgeTestCase {
  status: 'online' | 'offline' | 'busy' | 'error' | 'pending';
  expectedBgRgb: string;
  label: string;
}

export const STATUS_BADGE_TEST_CASES: StatusBadgeTestCase[] = [
  {
    status: 'online',
    expectedBgRgb: 'rgb(52, 199, 89)',
    label: 'Online',
  },
  {
    status: 'offline',
    expectedBgRgb: 'rgba(0, 0, 0, 0.06)',
    label: 'Offline',
  },
  {
    status: 'busy',
    expectedBgRgb: 'rgb(255, 204, 0)',
    label: 'Busy',
  },
  {
    status: 'error',
    expectedBgRgb: 'rgb(255, 59, 48)',
    label: 'Error',
  },
  {
    status: 'pending',
    expectedBgRgb: 'rgb(0, 122, 255)',
    label: 'Pending',
  },
];

export class AgentChatPageObject {
  readonly page: Page;
  readonly baseUrl: string;

  readonly container: Locator;
  readonly chatContainer: Locator;
  readonly messageBubbles: Locator;
  readonly chatInput: Locator;
  readonly sendButton: Locator;
  readonly emptyState: Locator;
  readonly quickActions: Locator;
  readonly statusBadges: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page, baseUrl = 'http://localhost:4200') {
    this.page = page;
    this.baseUrl = baseUrl;

    this.container = page.locator('app-agent-chat, .agent-chat');
    this.chatContainer = page.locator('.chat-container');
    this.messageBubbles = page.locator('.message-bubble');
    this.chatInput = page.locator('.chat-input');
    this.sendButton = page.locator('.send-button');
    this.emptyState = page.locator('.empty-state');
    this.quickActions = page.locator('.quick-actions');
    this.statusBadges = page.locator('app-status-badge .badge, .badge');
    this.errorMessage = page.locator('.error-message');
  }

  async navigate(path = '/agent-chat'): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path}`);
    await this.page.waitForLoadState('networkidle');
  }

  async sendMessage(message: string): Promise<void> {
    await this.chatInput.fill(message);
    await this.sendButton.click();
    await this.page.waitForTimeout(500);
  }

  async clickQuickAction(index = 0): Promise<void> {
    await this.quickActions.locator('.quick-action').nth(index).click();
  }

  async getMessageCount(): Promise<number> {
    return await this.messageBubbles.count();
  }

  async getUserMessageCount(): Promise<number> {
    return await this.messageBubbles.filter({ hasClass: /user/ }).count();
  }

  async getAssistantMessageCount(): Promise<number> {
    return await this.messageBubbles.filter({ hasClass: /user/ }).count();
  }

  async isSendButtonDisabled(): Promise<boolean> {
    return await this.sendButton.isDisabled();
  }

  async getSendButtonOpacity(): Promise<string> {
    return await this.sendButton.evaluate((el) => getComputedStyle(el).opacity);
  }

  async isEmptyStateVisible(): Promise<boolean> {
    return await this.emptyState.isVisible();
  }

  async getStatusBadgeCount(): Promise<number> {
    return await this.statusBadges.count();
  }

  async getStatusBadgeStatus(index: number): Promise<string> {
    const badge = this.statusBadges.nth(index);
    const className = await badge.getAttribute('class');
    for (const status of ['online', 'offline', 'busy', 'error', 'pending']) {
      if (className?.includes(status)) {
        return status;
      }
    }
    return '';
  }

  async getStatusBadgeBackgroundColor(index: number): Promise<string> {
    const badge = this.statusBadges.nth(index);
    return await badge.evaluate((el) => getComputedStyle(el).backgroundColor);
  }

  async getStatusBadgeLabel(index: number): Promise<string> {
    const badge = this.statusBadges.nth(index);
    return await badge.textContent() ?? '';
  }

  async isStatusBadgeDotVisible(index: number): Promise<boolean> {
    const badge = this.statusBadges.nth(index);
    const dot = badge.locator('.badge__dot');
    return await dot.isVisible();
  }

  async getStatusBadgeDotSize(index: number): Promise<{ width: number; height: number }> {
    const badge = this.statusBadges.nth(index);
    const dot = badge.locator('.badge__dot');
    return await dot.evaluate((el) => ({
      width: el.offsetWidth,
      height: el.offsetHeight,
    }));
  }

  async getStatusBadgeDotBorderRadius(index: number): Promise<string> {
    const badge = this.statusBadges.nth(index);
    const dot = badge.locator('.badge__dot');
    return await dot.evaluate((el) => getComputedStyle(el).borderRadius);
  }

  async getMessageBubbleAnimation(index: number): Promise<string> {
    const bubble = this.messageBubbles.nth(index);
    return await bubble.evaluate((el) => getComputedStyle(el).animation);
  }

  async getMessageContentStyles(index: number): Promise<{
    backgroundColor: string;
    color: string;
    borderRadius: string;
    padding: string;
  } | null> {
    const bubble = this.messageBubbles.nth(index);
    const content = bubble.locator('.message-content, .message-content--user');
    return await content.evaluate((el) => {
      const style = getComputedStyle(el);
      return {
        backgroundColor: style.backgroundColor,
        color: style.color,
        borderRadius: style.borderRadius,
        padding: style.padding,
      };
    });
  }

  async getUserMessageBorderRadius(index: number): Promise<string> {
    const bubble = this.messageBubbles.filter({ hasClass: /user/ }).nth(index);
    return await bubble.evaluate((el) => getComputedStyle(el).borderRadius);
  }

  async expectEmptyStateVisible(): Promise<void> {
    await expect(this.emptyState).toBeVisible();
  }

  async expectSendButtonDisabled(): Promise<void> {
    await expect(this.sendButton).toBeDisabled();
  }

  async expectSendButtonEnabled(): Promise<void> {
    await expect(this.sendButton).toBeEnabled();
  }

  async expectMessageCount(expected: number): Promise<void> {
    await expect(this.messageBubbles).toHaveCount(expected);
  }

  async expectUserMessageAtIndex(index: number): Promise<void> {
    await expect(this.messageBubbles.nth(index)).toHaveClass(/user/);
  }

  async expectAssistantMessageAtIndex(index: number): Promise<void> {
    await expect(this.messageBubbles.nth(index)).not.toHaveClass(/user/);
  }

  async expectStatusBadgeDotCircular(index: number): Promise<void> {
    const borderRadius = await this.getStatusBadgeDotBorderRadius(index);
    expect(borderRadius).toBe('50%');
  }

  async expectStatusBadgeDotSize(index: number, expectedSize = 6): Promise<void> {
    const size = await this.getStatusBadgeDotSize(index);
    expect(size.width).toBe(expectedSize);
    expect(size.height).toBe(expectedSize);
  }

  async expectStatusBadgeColor(
    index: number,
    expectedColor: string
  ): Promise<void> {
    const bgColor = await this.getStatusBadgeBackgroundColor(index);
    expect(bgColor).toBe(expectedColor);
  }
}
