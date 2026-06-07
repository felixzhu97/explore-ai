import { Page, Locator, expect } from '@playwright/test';

export class VisionPanelPageObject {
  readonly page: Page;
  readonly baseUrl: string;

  readonly container: Locator;
  readonly tabHeader: Locator;
  readonly segmentButtons: Locator;
  readonly imageArea: Locator;
  readonly dropZone: Locator;
  readonly previewImage: Locator;
  readonly zoomHint: Locator;
  readonly clearButton: Locator;
  readonly loadingOverlay: Locator;
  readonly spinner: Locator;
  readonly actionButton: Locator;
  readonly resultContent: Locator;
  readonly emptyState: Locator;
  readonly errorMessage: Locator;
  readonly zoomModal: Locator;
  readonly zoomCloseButton: Locator;

  constructor(page: Page, baseUrl = 'http://localhost:4200') {
    this.page = page;
    this.baseUrl = baseUrl;

    this.container = page.locator('app-vision-panel, .vision-panel');
    this.tabHeader = page.locator('.tab-header');
    this.segmentButtons = page.locator('.segment-button');
    this.imageArea = page.locator('.image-area');
    this.dropZone = page.locator('.drop-zone');
    this.previewImage = page.locator('.preview-image');
    this.zoomHint = page.locator('.zoom-hint');
    this.clearButton = page.locator('.clear-button');
    this.loadingOverlay = page.locator('.loading-overlay');
    this.spinner = page.locator('.spinner');
    this.actionButton = page.locator('.action-button.primary');
    this.resultContent = page.locator('.result-content');
    this.emptyState = page.locator('.empty-state');
    this.errorMessage = page.locator('.error-message');
    this.zoomModal = page.locator('.zoom-modal');
    this.zoomCloseButton = page.locator('.zoom-close');
  }

  async navigate(path = '/vision'): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path}`);
    await this.page.waitForLoadState('networkidle');
  }

  async selectTaskTab(tab: 'caption' | 'detect' | 'ocr'): Promise<void> {
    const tabMap = {
      caption: this.segmentButtons.filter({ hasText: /caption/i }).first(),
      detect: this.segmentButtons.filter({ hasText: /detect/i }).first(),
      ocr: this.segmentButtons.filter({ hasText: /ocr/i }).first(),
    };
    await tabMap[tab].click();
    await this.page.waitForTimeout(300);
  }

  async uploadImage(filePath: string): Promise<void> {
    const fileInput = this.page.locator('input[type="file"]');
    await fileInput.setInputFiles(filePath);
    await this.page.waitForTimeout(500);
  }

  async isDropZoneVisible(): Promise<boolean> {
    return await this.dropZone.isVisible();
  }

  async isPreviewImageVisible(): Promise<boolean> {
    return await this.previewImage.isVisible();
  }

  async hoverOverPreviewImage(): Promise<void> {
    await this.previewImage.hover();
    await this.page.waitForTimeout(200);
  }

  async clickPreviewImage(): Promise<void> {
    await this.previewImage.click();
    await this.page.waitForTimeout(300);
  }

  async isZoomModalVisible(): Promise<boolean> {
    return await this.zoomModal.isVisible();
  }

  async closeZoomModal(): Promise<void> {
    await this.zoomCloseButton.click();
    await this.page.waitForTimeout(300);
  }

  async clickClearButton(): Promise<void> {
    await this.clearButton.click();
    await this.page.waitForTimeout(300);
  }

  async isLoadingOverlayVisible(): Promise<boolean> {
    return await this.loadingOverlay.isVisible();
  }

  async isSpinnerVisible(): Promise<boolean> {
    return await this.spinner.isVisible();
  }

  async clickActionButton(): Promise<void> {
    await this.actionButton.click();
  }

  async isActionButtonEnabled(): Promise<boolean> {
    return !(await this.actionButton.isDisabled());
  }

  async getZoomHintOpacity(): Promise<string> {
    return await this.zoomHint.evaluate((el) => getComputedStyle(el).opacity);
  }

  async getImageAreaBackgroundColor(): Promise<string> {
    return await this.imageArea.evaluate(
      (el) => getComputedStyle(el).backgroundColor
    );
  }

  async getPreviewImageTransform(): Promise<string> {
    return await this.previewImage.evaluate((el) => getComputedStyle(el).transform);
  }

  async getSpinnerBorderColor(): Promise<string> {
    return await this.spinner.evaluate((el) => {
      const style = getComputedStyle(el);
      return style.borderTopColor;
    });
  }

  async getSpinnerAnimation(): Promise<string> {
    return await this.spinner.evaluate((el) => getComputedStyle(el).animation);
  }

  async getErrorMessageStyles(): Promise<{
    backgroundColor: string;
    color: string;
    borderRadius: string;
  } | null> {
    return await this.errorMessage.evaluate((el) => {
      const style = getComputedStyle(el);
      return {
        backgroundColor: style.backgroundColor,
        color: style.color,
        borderRadius: style.borderRadius,
      };
    });
  }

  async expectDropZoneVisible(): Promise<void> {
    await expect(this.dropZone).toBeVisible();
  }

  async expectPreviewImageVisible(): Promise<void> {
    await expect(this.previewImage).toBeVisible();
  }

  async expectActionButtonDisabled(): Promise<void> {
    await expect(this.actionButton).toBeDisabled();
  }

  async expectErrorMessageContains(text: string): Promise<void> {
    await expect(this.errorMessage).toContainText(text);
  }

  async getTabCount(): Promise<number> {
    return await this.segmentButtons.count();
  }
}
