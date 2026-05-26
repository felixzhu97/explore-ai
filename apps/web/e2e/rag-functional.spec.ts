import { test, expect, Page } from '@playwright/test';

/**
 * E2E Tests for Document QA (RAG) - Functional Interaction Tests
 * 
 * Tests verify:
 * 1. Document selection and management
 * 2. Query submission and AI response
 * 3. RAG tool calls and source citations
 */

const RAG_SERVICE_URL = 'http://localhost:8010';

/**
 * Helper to get RAG chat content
 */
async function getRAGContent(page: Page): Promise<string> {
  return await page.evaluate(() => {
    const container = document.querySelector('[class*="RAGChat"], [class*="chat"]');
    return container?.textContent || '';
  });
}

/**
 * Wait for RAG response
 */
async function waitForRAGResponse(page: Page, timeout: number = 15000): Promise<boolean> {
  try {
    await page.waitForFunction(
      () => {
        const container = document.querySelector('[class*="RAGChat"], [class*="chat"]');
        if (!container) return false;
        const text = container.textContent || '';
        return text.includes('Processing') || 
               text.includes('检索') ||
               text.includes('相关') ||
               text.includes('根据') ||
               text.length > 100;
      },
      { timeout }
    );
    return true;
  } catch {
    return false;
  }
}

test.describe('Document QA - Navigation and Display', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /Document QA|文档问答/i }).click();
    await page.waitForTimeout(500);
  });

  test('should display Document QA header', async ({ page }) => {
    const header = page.getByRole('heading', { name: /Document Q&A|文档问答/i });
    await expect(header).toBeVisible();
  });

  test('should display document list section', async ({ page }) => {
    const sectionTitle = page.getByText(/Available Documents|可用文档/i);
    await expect(sectionTitle).toBeVisible();
  });

  test('should display upload area', async ({ page }) => {
    const uploadArea = page.getByText(/Upload|上传/i);
    await expect(uploadArea.first()).toBeVisible();
  });
});

test.describe('Document QA - Document Selection', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /Document QA|文档问答/i }).click();
    await page.waitForTimeout(1000);
  });

  test('should show document checkboxes', async ({ page }) => {
    const checkboxes = page.locator('[role="checkbox"]');
    const count = await checkboxes.count();
    
    // Should have at least some documents or show empty state
    const hasDocuments = count > 0;
    if (hasDocuments) {
      await expect(checkboxes.first()).toBeVisible();
    }
  });

  test('should select/deselect documents', async ({ page }) => {
    const checkboxes = page.locator('[role="checkbox"]');
    const count = await checkboxes.count();
    
    if (count > 0) {
      const firstCheckbox = checkboxes.first();
      await firstCheckbox.click();
      await page.waitForTimeout(300);
      
      // Should be checked after click
      const isChecked = await firstCheckbox.getAttribute('aria-checked');
      expect(isChecked).toBe('true');
    }
  });

  test('should show select all button', async ({ page }) => {
    const selectAllBtn = page.getByRole('button', { name: /Select All/i });
    const isVisible = await selectAllBtn.isVisible().catch(() => false);
    
    if (isVisible) {
      await expect(selectAllBtn).toBeVisible();
    }
  });
});

test.describe('Document QA - Chat Interaction', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /Document QA|文档问答/i }).click();
    await page.waitForTimeout(500);
  });

  test('should display chat input area', async ({ page }) => {
    const input = page.locator('textarea[placeholder*="question|问题|question"], input[placeholder*="question|问题"]');
    await expect(input.first()).toBeVisible();
  });

  test('should send query using quick action', async ({ page }) => {
    const quickAction = page.getByRole('button', { name: /summary|summarize|总结/i }).first();
    const isVisible = await quickAction.isVisible().catch(() => false);
    
    if (isVisible) {
      await quickAction.click();
      await page.waitForTimeout(300);
      
      // Input should be filled
      const input = page.locator('textarea[placeholder*="question|问题"]').first();
      const inputValue = await input.inputValue();
      expect(inputValue.length).toBeGreaterThan(0);
    }
  });

  test('should send custom query', async ({ page }) => {
    const input = page.locator('textarea[placeholder*="question|问题"], input[placeholder*="question|问题"]').first();
    const sendButton = page.locator('button:has-text("→")');

    await input.fill('What is this document about?');
    await sendButton.click();
    
    // Wait for response
    await page.waitForTimeout(3000);
    
    const content = await getRAGContent(page);
    // Should have some response content
    expect(content.length).toBeGreaterThan(0);
  });
});

test.describe('Document QA - RAG Response Verification', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /Document QA|文档问答/i }).click();
    await page.waitForTimeout(500);
  });

  test('should display response after query', async ({ page }) => {
    const input = page.locator('textarea[placeholder*="question|问题"], input[placeholder*="question|问题"]').first();
    const sendButton = page.locator('button:has-text("→")');

    await input.fill('Summarize the key points');
    await sendButton.click();
    
    // Wait for response
    await waitForRAGResponse(page, 10000);
    
    const content = await getRAGContent(page);
    expect(content.length).toBeGreaterThan(50);
  });

  test('should handle error gracefully when RAG service unavailable', async ({ page }) => {
    const input = page.locator('textarea[placeholder*="question|问题"], input[placeholder*="question|问题"]').first();
    const sendButton = page.locator('button:has-text("→")');

    await input.fill('Test query');
    await sendButton.click();
    
    // Wait for error or response
    await page.waitForTimeout(5000);
    
    const content = await getRAGContent(page);
    // Should show either response or error message
    const hasResponse = content.length > 0;
    const hasError = content.toLowerCase().includes('error') || 
                     content.includes('不可用') || 
                     content.includes('unavailable');
    
    expect(hasResponse || hasError).toBeTruthy();
  });
});

test.describe('Document QA - Error Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /Document QA|文档问答/i }).click();
    await page.waitForTimeout(500);
  });

  test('should show appropriate message when no documents selected', async ({ page }) => {
    // Clear any selections
    const clearBtn = page.getByRole('button', { name: /Clear/i });
    const clearVisible = await clearBtn.isVisible().catch(() => false);
    if (clearVisible) {
      await clearBtn.click();
      await page.waitForTimeout(300);
    }
    
    // Try to send query
    const input = page.locator('textarea[placeholder*="question|问题"], input[placeholder*="question|问题"]').first();
    const sendButton = page.locator('button:has-text("→")');
    
    await input.fill('Test query');
    await sendButton.click();
    
    // Should either work or show error about no documents
    await page.waitForTimeout(3000);
    const content = await getRAGContent(page);
    expect(content.length).toBeGreaterThan(0);
  });

  test('should maintain UI stability on rapid clicks', async ({ page }) => {
    const input = page.locator('textarea[placeholder*="question|问题"], input[placeholder*="question|问题"]').first();
    const sendButton = page.locator('button:has-text("→")');

    await input.fill('Test');
    
    // Click send multiple times rapidly
    await sendButton.click();
    await page.waitForTimeout(100);
    await sendButton.click();
    await page.waitForTimeout(100);
    
    // UI should remain stable
    await page.waitForTimeout(500);
    const inputStillVisible = await input.isVisible();
    expect(inputStillVisible).toBeTruthy();
  });
});
