import { Page, Locator, expect } from '@playwright/test';

export class RAGChatPageObject {
  readonly page: Page;
  readonly baseUrl: string;

  readonly container: Locator;
  readonly toastContainer: Locator;
  readonly header: Locator;
  readonly title: Locator;
  readonly modelBadge: Locator;
  readonly documentsSection: Locator;
  readonly documentCards: Locator;
  readonly skeletonLoaders: Locator;
  readonly emptyDocsMessage: Locator;
  readonly fileUploadArea: Locator;
  readonly fileUploadLabel: Locator;
  readonly fileInput: Locator;
  readonly uploadedFiles: Locator;
  readonly uploadButton: Locator;
  readonly chatContainer: Locator;
  readonly messageBubbles: Locator;
  readonly emptyState: Locator;
  readonly quickActions: Locator;
  readonly chatInput: Locator;
  readonly sendButton: Locator;
  readonly sourcesPanel: Locator;

  constructor(page: Page, baseUrl = 'http://localhost:4200') {
    this.page = page;
    this.baseUrl = baseUrl;

    this.container = page.locator('app-rag-chat, .rag-chat');
    this.toastContainer = page.locator('.toast-container');
    this.header = page.locator('.header');
    this.title = page.locator('.title');
    this.modelBadge = page.locator('.model-badge');
    this.documentsSection = page.locator('.documents-section');
    this.documentCards = page.locator('.document-card');
    this.skeletonLoaders = page.locator('.skeleton');
    this.emptyDocsMessage = page.locator('.empty-docs');
    this.fileUploadArea = page.locator('.file-upload-area');
    this.fileUploadLabel = page.locator('.file-upload-label');
    this.fileInput = page.locator('#file-upload');
    this.uploadedFiles = page.locator('.uploaded-files');
    this.uploadButton = page.locator('.upload-button');
    this.chatContainer = page.locator('.chat-container');
    this.messageBubbles = page.locator('.message-bubble');
    this.emptyState = page.locator('.empty-state');
    this.quickActions = page.locator('.quick-actions');
    this.chatInput = page.locator('.chat-input');
    this.sendButton = page.locator('.send-button');
    this.sourcesPanel = page.locator('.sources-panel');
  }

  async navigate(path = '/rag-chat'): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path}`);
    await this.page.waitForLoadState('networkidle');
  }

  async selectDocument(index: number): Promise<void> {
    await this.documentCards.nth(index).click();
    await this.page.waitForTimeout(200);
  }

  async deleteDocument(index: number): Promise<void> {
    const card = this.documentCards.nth(index);
    await card.locator('.delete-button').click();
    await this.page.waitForTimeout(500);
  }

  async selectAllDocuments(): Promise<void> {
    await this.documentsSection.locator('.select-button').filter({ hasText: /select all/i }).click();
    await this.page.waitForTimeout(200);
  }

  async clearDocumentSelection(): Promise<void> {
    const clearBtn = this.documentsSection.locator('.select-button').filter({ hasText: /clear/i });
    if (await clearBtn.isVisible()) {
      await clearBtn.click();
      await this.page.waitForTimeout(200);
    }
  }

  async uploadFiles(filePaths: string[]): Promise<void> {
    await this.fileInput.setInputFiles(filePaths);
    await this.page.waitForTimeout(300);
  }

  async clickUploadButton(): Promise<void> {
    await this.uploadButton.click();
    await this.page.waitForTimeout(500);
  }

  async sendMessage(message: string): Promise<void> {
    await this.chatInput.fill(message);
    await this.sendButton.click();
    await this.page.waitForTimeout(500);
  }

  async clickQuickAction(index = 0): Promise<void> {
    await this.quickActions.locator('.quick-action').nth(index).click();
  }

  async expandSources(messageIndex = 0): Promise<void> {
    const sourceBadge = this.messageBubbles.nth(messageIndex).locator('.source-badge');
    if (await sourceBadge.isVisible()) {
      await sourceBadge.click();
      await this.page.waitForTimeout(300);
    }
  }

  async getToastCount(): Promise<number> {
    return await this.toastContainer.locator('.toast-item').count();
  }

  async getDocumentCount(): Promise<number> {
    return await this.documentCards.count();
  }

  async getSelectedDocumentCount(): Promise<number> {
    return await this.documentCards.filter({ hasClass: /selected/ }).count();
  }

  async getMessageCount(): Promise<number> {
    return await this.messageBubbles.count();
  }

  async isDocumentSelected(index: number): Promise<boolean> {
    const card = this.documentCards.nth(index);
    const className = await card.getAttribute('class');
    return className?.includes('selected') ?? false;
  }

  async isSendButtonDisabled(): Promise<boolean> {
    return await this.sendButton.isDisabled();
  }

  async getSendButtonOpacity(): Promise<string> {
    return await this.sendButton.evaluate((el) => getComputedStyle(el).opacity);
  }

  async getChatInputBorderRadius(): Promise<string> {
    return await this.chatInput.evaluate((el) => getComputedStyle(el).borderRadius);
  }

  async getChatInputFocusBoxShadow(): Promise<string> {
    await this.chatInput.focus();
    return await this.chatInput.evaluate((el) => getComputedStyle(el).boxShadow);
  }

  async getUploadButtonOpacity(): Promise<string> {
    return await this.uploadButton.evaluate((el) => getComputedStyle(el).opacity);
  }

  async getUploadButtonCursor(): Promise<string> {
    return await this.uploadButton.evaluate((el) => getComputedStyle(el).cursor);
  }

  async getSelectedDocumentStyles(index: number): Promise<{
    backgroundColor: string;
    color: string;
    borderColor: string;
    boxShadow: string;
  } | null> {
    return await this.documentCards.nth(index).evaluate((el) => {
      const style = getComputedStyle(el);
      return {
        backgroundColor: style.backgroundColor,
        color: style.color,
        borderColor: style.borderColor,
        boxShadow: style.boxShadow,
      };
    });
  }

  async getToastSlideAnimation(): Promise<string> {
    const toast = this.toastContainer.locator('.toast-item').first();
    return await toast.evaluate((el) => getComputedStyle(el).animation);
  }

  async getToastTransform(): Promise<string> {
    const toast = this.toastContainer.locator('.toast-item').first();
    return await toast.evaluate((el) => getComputedStyle(el).transform);
  }

  async waitForSkeletonToDisappear(): Promise<void> {
    await this.page.waitForSelector('.skeleton', { state: 'hidden', timeout: 5000 });
  }

  async expectEmptyStateVisible(): Promise<void> {
    await expect(this.emptyState).toBeVisible();
  }

  async expectSendButtonDisabled(): Promise<void> {
    await expect(this.sendButton).toBeDisabled();
  }

  async expectUploadButtonDisabled(): Promise<void> {
    await expect(this.uploadButton).toBeDisabled();
  }

  async expectDocumentCount(expected: number): Promise<void> {
    await expect(this.documentCards).toHaveCount(expected);
  }

  async expectMessageCount(expected: number): Promise<void> {
    await expect(this.messageBubbles).toHaveCount(expected);
  }
}
