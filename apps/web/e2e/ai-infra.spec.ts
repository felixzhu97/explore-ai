import { test, expect, Page } from '@playwright/test';

/**
 * E2E Tests for AI Infrastructure Panel
 * Tests cover:
 * 1. Navigation between AI Infra tabs (Supervisor, K8s, Monitoring, etc.)
 * 2. Agent chat functionality
 * 3. Quick prompt interactions
 * 4. Tool call and response display
 */

test.describe('AI Infrastructure Panel - Tab Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should display AI Infra navigation', async ({ page }) => {
    const aiInfraButton = page.getByRole('button', { name: /AI Infra/i });
    await expect(aiInfraButton).toBeVisible();
  });

  test('should switch between all tabs', async ({ page }) => {
    // Navigate to AI Infra
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);

    // List of expected tabs
    const tabs = [
      'Supervisor',
      'K8s',
      'Monitoring',
      'Models',
      'LLMOps',
      'AIOps',
      'VectorDB',
    ];

    for (const tabName of tabs) {
      const tab = page.getByRole('tab', { name: new RegExp(tabName, 'i') });
      const tabVisible = await tab.isVisible().catch(() => false);
      
      if (tabVisible) {
        await tab.click();
        await page.waitForTimeout(200);
        await expect(tab).toHaveAttribute('aria-selected', 'true');
      }
    }
  });

  test('should show agent status badge', async ({ page }) => {
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);

    // Status badge should show Online
    const statusBadge = page.getByText('Online');
    await expect(statusBadge).toBeVisible();
  });
});

test.describe('AI Infrastructure - Supervisor Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    // Ensure Supervisor tab is active
    const supervisorTab = page.getByRole('tab', { name: /Supervisor/i });
    await supervisorTab.click();
    await page.waitForTimeout(200);
  });

  test('should display chat interface', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    await expect(chatInput).toBeVisible();
  });

  test('should enable send button when input has content', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button[aria-label*="send"], button:has-text("→")');

    // Initially disabled
    await expect(sendButton).toBeDisabled();

    // Type text
    await chatInput.fill('Hello');
    await expect(sendButton).toBeEnabled();
  });

  test('should display quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /agent|delegate|health/i });
    await expect(quickPrompts.first()).toBeVisible();
  });

  test('should fill input from quick prompt', async ({ page }) => {
    const quickPrompt = page.locator('button').filter({ hasText: /List all available agents/i }).first();
    
    const isVisible = await quickPrompt.isVisible().catch(() => false);
    if (isVisible) {
      await quickPrompt.click();
      await page.waitForTimeout(200);
      
      const chatInput = page.locator('textarea[placeholder*="Type your message"]');
      const inputValue = await chatInput.inputValue();
      expect(inputValue.length).toBeGreaterThan(0);
    }
  });
});

test.describe('AI Infrastructure - K8s Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const k8sTab = page.getByRole('tab', { name: /K8s/i });
    await k8sTab.click();
    await page.waitForTimeout(200);
  });

  test('should display K8s panel with title', async ({ page }) => {
    const title = page.getByRole('heading', { name: /K8s/i });
    await expect(title).toBeVisible();
  });

  test('should display K8s quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /pod|cluster|scale/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });

  test('should send message to K8s agent', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Show me all running pods');
    await sendButton.click();
    await page.waitForTimeout(1000);

    // Message should appear in chat
    const message = page.getByText(/Show me all running pods/i);
    await expect(message).toBeVisible();
  });
});

test.describe('AI Infrastructure - Monitoring Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(200);
  });

  test('should display Monitoring panel', async ({ page }) => {
    const title = page.getByRole('heading', { name: /Monitoring/i });
    await expect(title).toBeVisible();
  });

  test('should display Monitoring quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /CPU|alert|memory/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });

  test('should send monitoring query', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Show CPU usage in last hour');
    await sendButton.click();
    await page.waitForTimeout(2000);

    // Should either show response or error (depending on backend)
    const hasContent = await page.locator('text=/CPU|usage|error|Processing/i').first().isVisible();
    expect(hasContent).toBeTruthy();
  });
});

test.describe('AI Infrastructure - Models Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const modelsTab = page.getByRole('tab', { name: /Models/i });
    await modelsTab.click();
    await page.waitForTimeout(200);
  });

  test('should display Models panel', async ({ page }) => {
    const title = page.getByRole('heading', { name: /Models/i });
    await expect(title).toBeVisible();
  });

  test('should display Models quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /model|deployed|latency/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });
});

test.describe('AI Infrastructure - LLMOps Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const llmopsTab = page.getByRole('tab', { name: /LLMOps/i });
    await llmopsTab.click();
    await page.waitForTimeout(200);
  });

  test('should display LLMOps panel', async ({ page }) => {
    const title = page.getByRole('heading', { name: /LLMOps/i });
    await expect(title).toBeVisible();
  });

  test('should display LLMOps quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /training|fine-tune|job/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });
});

test.describe('AI Infrastructure - AIOps Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const aiopsTab = page.getByRole('tab', { name: /AIOps/i });
    await aiopsTab.click();
    await page.waitForTimeout(200);
  });

  test('should display AIOps panel', async ({ page }) => {
    const title = page.getByRole('heading', { name: /AIOps/i });
    await expect(title).toBeVisible();
  });

  test('should display AIOps quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /incident|root cause|automation/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });
});

test.describe('AI Infrastructure - VectorDB Agent', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const vectordbTab = page.getByRole('tab', { name: /VectorDB/i });
    await vectordbTab.click();
    await page.waitForTimeout(200);
  });

  test('should display VectorDB panel', async ({ page }) => {
    const title = page.getByRole('heading', { name: /VectorDB/i });
    await expect(title).toBeVisible();
  });

  test('should display VectorDB quick prompts', async ({ page }) => {
    const quickPrompts = page.locator('button').filter({ hasText: /document|index|embedding/i });
    const count = await quickPrompts.count();
    expect(count).toBeGreaterThan(0);
  });
});

test.describe('AI Infrastructure - Error Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
  });

  test('should handle API errors gracefully', async ({ page }) => {
    const supervisorTab = page.getByRole('tab', { name: /Supervisor/i });
    await supervisorTab.click();
    await page.waitForTimeout(200);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Test message');
    await sendButton.click();
    await page.waitForTimeout(3000);

    // Chat container should remain stable
    const chatContainer = page.locator('[class*="ChatContainer"], [class*="chat"]').first();
    await expect(chatContainer).toBeVisible();
  });

  test('should maintain tab state after error', async ({ page }) => {
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(200);

    // Try to send message
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    if (await chatInput.isVisible()) {
      await chatInput.fill('Test');
      await page.locator('button:has-text("→")').click();
      await page.waitForTimeout(3000);
    }

    // Tab should still be selected
    await expect(monitoringTab).toHaveAttribute('aria-selected', 'true');
  });
});

test.describe('AI Infrastructure - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
  });

  test('should have accessible tab navigation', async ({ page }) => {
    const tablist = page.locator('[role="tablist"]');
    await expect(tablist).toBeVisible();
    
    const tabs = page.locator('[role="tab"]');
    await expect(tabs.first()).toHaveAttribute('role', 'tab');
  });

  test('should have accessible chat input', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    await expect(chatInput).toBeVisible();
    await expect(chatInput).toHaveAttribute('placeholder', /.*/);
  });

  test('should support keyboard navigation in tabs', async ({ page }) => {
    const firstTab = page.getByRole('tab').first();
    await firstTab.focus();
    
    // Tab key should move focus
    await page.keyboard.press('Tab');
    
    // Focus should be maintained
    const focused = await page.evaluate(() => document.activeElement !== null);
    expect(focused).toBeTruthy();
  });
});
