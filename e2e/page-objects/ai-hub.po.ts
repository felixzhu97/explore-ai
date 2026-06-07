import { Page, Locator, expect } from '@playwright/test';

export class AIHubPageObject {
  readonly page: Page;
  readonly baseUrl: string;

  readonly container: Locator;
  readonly tabHeader: Locator;
  readonly segmentButtons: Locator;
  readonly chatTab: Locator;
  readonly imageTab: Locator;
  readonly ttsTab: Locator;
  readonly chatContainer: Locator;
  readonly chatInput: Locator;
  readonly sendButton: Locator;
  readonly emptyState: Locator;
  readonly quickActions: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page, baseUrl = 'http://localhost:4200') {
    this.page = page;
    this.baseUrl = baseUrl;

    this.container = page.locator('app-ai-hub, .ai-hub');
    this.tabHeader = page.locator('.tab-header');
    this.segmentButtons = page.locator('.segment-button');
    this.chatTab = page.locator('.segment-button').filter({ hasText: /chat/i }).first();
    this.imageTab = page.locator('.segment-button').filter({ hasText: /image/i }).first();
    this.ttsTab = page.locator('.segment-button').filter({ hasText: /tts/i }).first();
    this.chatContainer = page.locator('.chat-container');
    this.chatInput = page.locator('.chat-input, textarea');
    this.sendButton = page.locator('.send-button');
    this.emptyState = page.locator('.empty-state, .empty-chat-state');
    this.quickActions = page.locator('.quick-actions');
    this.errorMessage = page.locator('.error-message');
  }

  async navigate(): Promise<void> {
    await this.page.goto(this.baseUrl);
    await this.page.waitForLoadState('networkidle');
  }

  async selectTab(tab: 'chat' | 'image' | 'tts'): Promise<void> {
    const tabMap = {
      chat: this.chatTab,
      image: this.imageTab,
      tts: this.ttsTab,
    };
    await tabMap[tab].click();
    await this.page.waitForTimeout(300);
  }

  async sendMessage(message: string): Promise<void> {
    await this.chatInput.fill(message);
    await this.sendButton.click();
    await this.page.waitForTimeout(500);
  }

  async getActiveTab(): Promise<string> {
    const activeTab = this.segmentButtons.filter({ hasClass: /active/ });
    return await activeTab.textContent() ?? '';
  }

  async getMessageCount(): Promise<number> {
    return await this.chatContainer.locator('.message-bubble').count();
  }

  async getTabCount(): Promise<number> {
    return await this.segmentButtons.count();
  }

  async isTabActive(index: number): Promise<boolean> {
    const tab = this.segmentButtons.nth(index);
    const className = await tab.getAttribute('class');
    return className?.includes('active') ?? false;
  }

  async clickQuickAction(index = 0): Promise<void> {
    await this.quickActions.locator('.quick-action').nth(index).click();
  }

  async expectTabCount(expected: number): Promise<void> {
    await expect(this.segmentButtons).toHaveCount(expected);
  }

  async expectActiveTab(expectedIndex: number): Promise<void> {
    for (let i = 0; i < await this.segmentButtons.count(); i++) {
      const isActive = await this.isTabActive(i);
      if (i === expectedIndex) {
        await expect(this.segmentButtons.nth(i)).toHaveClass(/active/);
      } else {
        await expect(this.segmentButtons.nth(i)).not.toHaveClass(/active/);
      }
    }
  }

  async getComputedStyles(selector: string) {
    return await this.page.evaluate(
      (s) => {
        const el = document.querySelector(s);
        if (!el) return null;
        const style = getComputedStyle(el);
        return {
          backgroundColor: style.backgroundColor,
          color: style.color,
          borderRadius: style.borderRadius,
          padding: style.padding,
          margin: style.margin,
          fontSize: style.fontSize,
          fontWeight: style.fontWeight,
          boxShadow: style.boxShadow,
        };
      },
      selector
    );
  }

  async getSendButtonOpacity(): Promise<string> {
    return await this.sendButton.evaluate((el) => getComputedStyle(el).opacity);
  }

  async isSendButtonDisabled(): Promise<boolean> {
    return await this.sendButton.isDisabled();
  }
}
