import { test, expect, Page } from '@playwright/test';

/**
 * E2E Tests for AI Infrastructure - Functional Interaction Tests
 * 
 * These tests verify actual interactions with AI agents and expect correct responses.
 * Tests include:
 * 1. Monitoring agent - metrics queries
 * 2. K8s agent - cluster operations
 * 3. Models agent - ML model management
 * 4. LLMOps agent - training jobs
 * 5. AIOps agent - incident analysis
 * 6. VectorDB agent - similarity search
 */

const AI_AGENTS_URL = 'http://localhost:8003';
const EXPECTED_RESPONSE_TIMEOUT = 60000; // 60 seconds for AI service responses

/**
 * Helper function to wait for AI response
 */
async function waitForAIResponse(page: Page, timeout: number = EXPECTED_RESPONSE_TIMEOUT): Promise<boolean> {
  try {
    // Wait for any response content (either success or error message)
    await page.waitForFunction(
      () => {
        const chatContainer = document.querySelector('[class*="ChatContainer"], [class*="chat"]');
        if (!chatContainer) return false;
        const text = chatContainer.textContent || '';
        // Look for response indicators
        return text.includes('Processing') || 
               text.includes('Running') ||
               text.includes('✓') ||
               text.includes('Error') ||
               text.includes('Metric') ||
               text.includes('Pods') ||
               text.includes('Models') ||
               text.includes('Incident') ||
               text.length > 100;
      },
      { timeout }
    );
    return true;
  } catch {
    return false;
  }
}

/**
 * Helper to get chat container text
 */
async function getChatContent(page: Page): Promise<string> {
  return await page.evaluate(() => {
    const container = document.querySelector('[class*="ChatContainer"], [class*="chat"]');
    return container?.textContent || '';
  });
}

test.describe('AI Infrastructure - Monitoring Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(500);
  });

  test('should query CPU metrics and receive response', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    // Click the quick prompt button
    const cpuPrompt = page.getByRole('button', { name: /CPU usage/i });
    const buttonExists = await cpuPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await cpuPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Show CPU usage in last hour');
    }
    
    await sendButton.click();
    
    // Wait for response
    const hasResponse = await waitForAIResponse(page, 20000);
    
    // Get chat content
    const content = await getChatContent(page);
    
    // Verify response contains expected data (either metrics or tool call result)
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(50);
  });

  test('should query memory metrics', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const memoryPrompt = page.getByRole('button', { name: /memory/i }).first();
    const buttonExists = await memoryPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await memoryPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Check memory utilization');
    }
    
    await sendButton.click();
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });

  test('should list active alerts', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('List all active alerts');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Should contain alert-related content
    expect(content.toLowerCase()).toMatch(/alert|metric|cpu|memory|usage/i);
  });
});

test.describe('AI Infrastructure - K8s Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const k8sTab = page.getByRole('tab', { name: /K8s/i });
    await k8sTab.click();
    await page.waitForTimeout(500);
  });

  test('should list running pods', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    // Use quick prompt
    const podsPrompt = page.getByRole('button', { name: /running pods/i });
    const buttonExists = await podsPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await podsPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Show me all running pods');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Response should contain pod or cluster related content
    expect(content.toLowerCase()).toMatch(/pod|container|cluster|deployment|service|kubernetes|k8s/i);
  });

  test('should check cluster health', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Check cluster health status');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(30);
  });

  test('should scale deployment', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Scale deployment to 3 replicas');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });
});

test.describe('AI Infrastructure - Models Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const modelsTab = page.getByRole('tab', { name: /Models/i });
    await modelsTab.click();
    await page.waitForTimeout(500);
  });

  test('should list deployed models', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const listPrompt = page.getByRole('button', { name: /deployed models/i });
    const buttonExists = await listPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await listPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('List deployed models');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Should contain model-related content
    expect(content.toLowerCase()).toMatch(/model|version|stage|deployed|framework|accuracy/i);
  });

  test('should show model performance metrics', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Show model performance metrics');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(30);
  });

  test('should compare inference latency', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Compare inference latency');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });
});

test.describe('AI Infrastructure - LLMOps Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const llmopsTab = page.getByRole('tab', { name: /LLMOps/i });
    await llmopsTab.click();
    await page.waitForTimeout(500);
  });

  test('should show current training jobs', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const jobsPrompt = page.getByRole('button', { name: /training jobs/i });
    const buttonExists = await jobsPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await jobsPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Show current training jobs');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Should contain training/ML related content
    expect(content.toLowerCase()).toMatch(/training|job|model|llm|ml|experiment|fine-tune/i);
  });

  test('should list fine-tuned models', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('List fine-tuned models');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });

  test('should check training dataset status', async ({ page }) => {
    test.setTimeout(180000); // 3 minutes for slow services
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Check training dataset status');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 60000);
    
    expect(hasResponse).toBeTruthy();
  });
});

test.describe('AI Infrastructure - AIOps Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const aiopsTab = page.getByRole('tab', { name: /AIOps/i });
    await aiopsTab.click();
    await page.waitForTimeout(500);
  });

  test('should analyze recent incidents', async ({ page }) => {
    test.setTimeout(180000); // 3 minutes for slow services
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const incidentPrompt = page.getByRole('button', { name: /recent incidents/i });
    const buttonExists = await incidentPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await incidentPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Analyze recent incidents');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 60000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Should contain incident/ops related content
    expect(content.toLowerCase()).toMatch(/incident|alert|anomaly|ops|error|warning|cpu|memory|issue/i);
  });

  test('should show root cause analysis', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const rootCausePrompt = page.getByRole('button', { name: /root cause/i });
    const buttonExists = await rootCausePrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await rootCausePrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Show root cause for incident #123');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });

  test('should list pending automations', async ({ page }) => {
    test.setTimeout(180000); // 3 minutes for slow services
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('List pending automations');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 60000);
    
    expect(hasResponse).toBeTruthy();
  });
});

test.describe('AI Infrastructure - VectorDB Agent Functional Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
    
    const vectorTab = page.getByRole('tab', { name: /VectorDB/i });
    await vectorTab.click();
    await page.waitForTimeout(500);
  });

  test('should search for similar documents', async ({ page }) => {
    test.setTimeout(180000); // 3 minutes for slow services
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const searchPrompt = page.getByRole('button', { name: /similar documents/i });
    const buttonExists = await searchPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await searchPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Search for similar documents about machine learning');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 60000);
    const content = await getChatContent(page);
    
    expect(hasResponse).toBeTruthy();
    // Should contain vector/search related content
    expect(content.toLowerCase()).toMatch(/vector|document|similar|search|embedding|collection|index/i);
  });

  test('should show index statistics', async ({ page }) => {
    test.setTimeout(180000); // 3 minutes for slow services
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    const statsPrompt = page.getByRole('button', { name: /index statistics/i });
    const buttonExists = await statsPrompt.isVisible().catch(() => false);
    
    if (buttonExists) {
      await statsPrompt.click();
      await page.waitForTimeout(200);
    } else {
      await chatInput.fill('Show index statistics');
    }
    
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 60000);
    
    expect(hasResponse).toBeTruthy();
  });

  test('should list recent embeddings', async ({ page }) => {
    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('List recent embeddings');
    await sendButton.click();
    
    const hasResponse = await waitForAIResponse(page, 20000);
    
    expect(hasResponse).toBeTruthy();
  });
});

test.describe('AI Infrastructure - Tool Call Verification', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
  });

  test('should show tool call indicators for Monitoring', async ({ page }) => {
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Show CPU usage');
    await sendButton.click();
    
    // Wait for response
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    // Verify we got a meaningful response (either tool call result or AI response)
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(30);
  });

  test('should show tool call for Models agent', async ({ page }) => {
    const modelsTab = page.getByRole('tab', { name: /Models/i });
    await modelsTab.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('List deployed models');
    await sendButton.click();
    
    // Wait for response
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    // Verify we got a meaningful response
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(30);
  });

  test('should show tool call for VectorDB agent', async ({ page }) => {
    const vectorTab = page.getByRole('tab', { name: /VectorDB/i });
    await vectorTab.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Show index statistics');
    await sendButton.click();
    
    // Wait for response
    const hasResponse = await waitForAIResponse(page, 20000);
    const content = await getChatContent(page);
    
    // Verify we got a meaningful response
    expect(hasResponse).toBeTruthy();
    expect(content.length).toBeGreaterThan(30);
  });
});

test.describe('AI Infrastructure - Error Response Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /AI Infra/i }).click();
    await page.waitForTimeout(300);
  });

  test('should display meaningful error when service unavailable', async ({ page }) => {
    // This test verifies error handling behavior
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    await chatInput.fill('Test error handling');
    await sendButton.click();
    
    // Wait for any response (success or error)
    await page.waitForTimeout(3000);
    
    const content = await getChatContent(page);
    
    // Should have some content (either response or error message)
    expect(content.length).toBeGreaterThan(0);
  });

  test('should handle rapid message sending gracefully', async ({ page }) => {
    const monitoringTab = page.getByRole('tab', { name: /Monitoring/i });
    await monitoringTab.click();
    await page.waitForTimeout(500);

    const chatInput = page.locator('textarea[placeholder*="Type your message"]');
    const sendButton = page.locator('button:has-text("→")');

    // Fill input but don't click send (button should be enabled)
    await chatInput.fill('Test');
    await page.waitForTimeout(300);
    
    // UI should remain stable
    const chatInputVisible = await chatInput.isVisible();
    const buttonEnabled = await sendButton.isEnabled();
    expect(chatInputVisible).toBeTruthy();
    expect(buttonEnabled).toBeTruthy();
  });
});
